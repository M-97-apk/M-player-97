package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.model.AudioTrack
import com.example.model.ConversionFormat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaConversionResult(
  val success: Boolean,
  val outputUri: Uri? = null,
  val outputDisplayPath: String = "",
  val errorMessage: String? = null,
)

object MediaConverter {
  const val APP_FOLDER_NAME = "M player 97"

  fun hasValidTreePermission(context: Context, treeUriString: String?): Boolean {
    if (treeUriString.isNullOrBlank()) return false
    val uri = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return false
    return context.contentResolver.persistedUriPermissions.any { perm ->
      perm.uri == uri && perm.isWritePermission && perm.isReadPermission
    }
  }

  /**
   * Resolves or creates the dedicated "M player 97" child directory inside the user-authorized
   * Storage Access Framework document tree.
   */
  fun ensureSafAppDirectory(context: Context, treeUri: Uri): Pair<Uri, String>? {
    val resolver = context.contentResolver
    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
    val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

    var rootName = ""
    runCatching {
      resolver.query(
        rootDocUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null,
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          rootName = cursor.getString(0).orEmpty()
        }
      }
    }
    if (rootName.equals(APP_FOLDER_NAME, ignoreCase = true)) {
      return rootDocUri to APP_FOLDER_NAME
    }

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
    var existingDirUri: Uri? = null
    runCatching {
      resolver.query(
        childrenUri,
        arrayOf(
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE,
        ),
        null,
        null,
        null,
      )?.use { cursor ->
        while (cursor.moveToNext()) {
          val docId = cursor.getString(0).orEmpty()
          val name = cursor.getString(1).orEmpty()
          val mime = cursor.getString(2).orEmpty()
          if (
            mime == DocumentsContract.Document.MIME_TYPE_DIR &&
            name.equals(APP_FOLDER_NAME, ignoreCase = true)
          ) {
            existingDirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            break
          }
        }
      }
    }
    if (existingDirUri != null) {
      return existingDirUri!! to "$rootName/$APP_FOLDER_NAME".trimStart('/')
    }

    val createdUri =
      runCatching {
        DocumentsContract.createDocument(
          resolver,
          rootDocUri,
          DocumentsContract.Document.MIME_TYPE_DIR,
          APP_FOLDER_NAME,
        )
      }.getOrNull() ?: return null

    val label =
      if (rootName.isNotBlank()) "$rootName/$APP_FOLDER_NAME" else APP_FOLDER_NAME
    return createdUri to label
  }

  suspend fun convertMedia(
    context: Context,
    source: AudioTrack,
    format: ConversionFormat,
    safTreeUriString: String?,
    onProgress: (Float) -> Unit,
  ): MediaConversionResult =
    withContext(Dispatchers.IO) {
      val safeStem = sanitizeFilename(source.title).ifBlank { "media_${source.id}" }
      val ext = if (format == ConversionFormat.MP3_AUDIO) "mp3" else "mp4"
      val mimeType = if (format == ConversionFormat.MP3_AUDIO) "audio/mpeg" else "video/mp4"
      val outputName = "$safeStem.$ext"

      val tempOutFile = File(context.cacheDir, "convert_tmp_${System.currentTimeMillis()}.$ext")
      try {
        onProgress(0.03f)
        val ok =
          when (format) {
            ConversionFormat.MP3_AUDIO ->
              convertAudioToMp3(context, source, tempOutFile, onProgress)
            ConversionFormat.MP4_VIDEO ->
              remuxToMp4(context, source.contentUri, tempOutFile, onProgress)
          }

        if (!ok || !tempOutFile.exists() || tempOutFile.length() == 0L) {
          return@withContext MediaConversionResult(
            success = false,
            errorMessage = "No compatible audio/video stream found for conversion",
          )
        }

        onProgress(0.95f)
        val saved =
          writeOutputToDestination(
            context = context,
            tempFile = tempOutFile,
            fileName = outputName,
            mimeType = mimeType,
            format = format,
            safTreeUriString = safTreeUriString,
          )
        onProgress(1.0f)
        saved
      } catch (err: Exception) {
        MediaConversionResult(success = false, errorMessage = err.message ?: "Conversion failed")
      } finally {
        runCatching { tempOutFile.delete() }
      }
    }

  private fun writeOutputToDestination(
    context: Context,
    tempFile: File,
    fileName: String,
    mimeType: String,
    format: ConversionFormat,
    safTreeUriString: String?,
  ): MediaConversionResult {
    // Write strictly ONCE to the single highest-priority valid destination:
    // 1. Authorized SAF "M player 97" tree URI
    if (hasValidTreePermission(context, safTreeUriString)) {
      val treeUri = Uri.parse(safTreeUriString)
      val dirInfo = ensureSafAppDirectory(context, treeUri)
      if (dirInfo != null) {
        val (dirUri, folderDisplay) = dirInfo
        val docUri =
          runCatching {
            DocumentsContract.createDocument(context.contentResolver, dirUri, mimeType, fileName)
          }.getOrNull()
        if (docUri != null) {
          val written =
            runCatching {
              context.contentResolver.openOutputStream(docUri, "wt")?.use { out ->
                FileInputStream(tempFile).use { input -> input.copyTo(out) }
                out.flush()
              } != null
            }.getOrDefault(false)
          if (written) {
            return MediaConversionResult(
              success = true,
              outputUri = docUri,
              outputDisplayPath = "$folderDisplay/$fileName",
            )
          } else {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, docUri) }
          }
        }
      }
    }

    // 2. Android Q+ scoped MediaStore collection under dedicated "Music/M player 97" or "Movies/M player 97"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val relPath =
        if (format == ConversionFormat.MP3_AUDIO) {
          "${Environment.DIRECTORY_MUSIC}/$APP_FOLDER_NAME"
        } else {
          "${Environment.DIRECTORY_MOVIES}/$APP_FOLDER_NAME"
        }
      val collection =
        if (format == ConversionFormat.MP3_AUDIO) {
          MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
          MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
      val values =
        ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
          put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
          put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
      val itemUri = context.contentResolver.insert(collection, values)
      if (itemUri != null) {
        val ok =
          runCatching {
            context.contentResolver.openOutputStream(itemUri)?.use { out ->
              FileInputStream(tempFile).use { input -> input.copyTo(out) }
              out.flush()
            } != null
          }.getOrDefault(false)
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(itemUri, values, null, null)
        if (ok) {
          return MediaConversionResult(
            success = true,
            outputUri = itemUri,
            outputDisplayPath = "$relPath/$fileName",
          )
        } else {
          runCatching { context.contentResolver.delete(itemUri, null, null) }
        }
      }
    }

    // 3. App-scoped external media fallback (zero broad storage permissions needed)
    val baseDir =
      context.getExternalFilesDir(
        if (format == ConversionFormat.MP3_AUDIO) Environment.DIRECTORY_MUSIC
        else Environment.DIRECTORY_MOVIES
      ) ?: context.filesDir
    val appDir = File(baseDir, APP_FOLDER_NAME).apply { mkdirs() }
    val destFile = resolveUniqueFile(appDir, fileName)
    tempFile.copyTo(destFile, overwrite = false)
    return MediaConversionResult(
      success = true,
      outputUri = Uri.fromFile(destFile),
      outputDisplayPath = "$APP_FOLDER_NAME/${destFile.name}",
    )
  }

  private fun resolveUniqueFile(dir: File, desiredName: String): File {
    var candidate = File(dir, desiredName)
    if (!candidate.exists()) return candidate
    val stem = desiredName.substringBeforeLast('.')
    val ext = desiredName.substringAfterLast('.', "")
    var idx = 1
    while (candidate.exists() && idx < 1000) {
      val name = if (ext.isNotEmpty()) "$stem ($idx).$ext" else "$stem ($idx)"
      candidate = File(dir, name)
      idx++
    }
    return candidate
  }

  /**
   * Strictly isolates the primary audio track from any video/audio source, decodes it to clean PCM
   * via MediaCodec (accurately honoring KEY_PCM_ENCODING for 16-bit vs 32-bit float PCM and output
   * format changes), and encodes it into a crisp 320 kbps CBR .mp3 file via PureMp3Encoder.
   */
  private fun convertAudioToMp3(
    context: Context,
    source: AudioTrack,
    outFile: File,
    onProgress: (Float) -> Unit,
  ): Boolean {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var pfd: ParcelFileDescriptor? = null
    var encoder: PureMp3Encoder? = null
    var outStream: BufferedOutputStream? = null

    try {
      pfd = openSource(context, extractor, source.contentUri)
      var audioTrackIdx = -1
      var inputFormat: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("audio/")) {
          audioTrackIdx = i
          inputFormat = fmt
          break
        }
      }
      if (audioTrackIdx < 0 || inputFormat == null) return false

      for (i in 0 until extractor.trackCount) {
        runCatching { extractor.unselectTrack(i) }
      }
      extractor.selectTrack(audioTrackIdx)

      val trackMime = inputFormat.getString(MediaFormat.KEY_MIME).orEmpty()
      val durationUs =
        if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
          inputFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
        } else {
          (source.durationMs * 1000L).coerceAtLeast(1L)
        }

      outStream = BufferedOutputStream(FileOutputStream(outFile), 32 * 1024)

      decoder = MediaCodec.createDecoderByType(trackMime)
      decoder.configure(inputFormat, null, null, 0)
      decoder.start()

      var activeSampleRate =
        if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
          inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
        } else {
          44100
        }
      var activeChannels =
        if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
          inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        } else {
          2
        }
      var activePcmEncoding =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
          inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
        ) {
          inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
          AudioFormat.ENCODING_PCM_16BIT
        }

      fun syncFormat(fmt: MediaFormat) {
        if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
          activeSampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
        }
        if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
          activeChannels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
          fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)
        ) {
          activePcmEncoding = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
        }
      }

      val bufferInfo = MediaCodec.BufferInfo()
      var inputDone = false
      var outputDone = false
      var idleIterations = 0
      var transferBytes = ByteArray(16 * 1024)

      while (!outputDone && idleIterations < 500) {
        var progressed = false

        if (!inputDone) {
          val inIdx = decoder.dequeueInputBuffer(10_000L)
          if (inIdx >= 0) {
            progressed = true
            val inBuf = decoder.getInputBuffer(inIdx)
            if (inBuf != null) {
              val sampleSize = extractor.readSampleData(inBuf, 0)
              if (sampleSize < 0) {
                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputDone = true
              } else {
                val ptsUs = extractor.sampleTime.coerceAtLeast(0L)
                decoder.queueInputBuffer(inIdx, 0, sampleSize, ptsUs, 0)
                onProgress(0.05f + 0.85f * (ptsUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f))
                extractor.advance()
              }
            }
          }
        }

        val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
        when {
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            progressed = true
            syncFormat(decoder.outputFormat)
          }
          outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
          outIdx >= 0 -> {
            progressed = true
            val isCodecConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
            val outBuf = decoder.getOutputBuffer(outIdx)

            if (!isCodecConfig && outBuf != null && bufferInfo.size > 0) {
              if (encoder == null) {
                syncFormat(decoder.outputFormat)
                encoder =
                  PureMp3Encoder(
                    inputSampleRate = activeSampleRate,
                    inputChannels = activeChannels,
                    output = outStream,
                    title = source.title,
                    artist = source.artist,
                    album = source.album,
                  )
              }

              outBuf.position(bufferInfo.offset)
              outBuf.limit(bufferInfo.offset + bufferInfo.size)
              if (transferBytes.size < bufferInfo.size) {
                transferBytes = ByteArray(bufferInfo.size)
              }
              outBuf.get(transferBytes, 0, bufferInfo.size)

              if (activePcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                encoder.feedPcmFloat(transferBytes, bufferInfo.size)
              } else {
                encoder.feedPcm16(transferBytes, bufferInfo.size)
              }
            }

            decoder.releaseOutputBuffer(outIdx, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              outputDone = true
            }
          }
        }

        idleIterations = if (progressed) 0 else idleIterations + 1
      }

      encoder?.finish()
      outStream.flush()
      return (encoder?.framesWritten ?: 0) > 0 && outFile.exists() && outFile.length() > 128L
    } finally {
      runCatching { decoder?.stop() }
      runCatching { decoder?.release() }
      runCatching { outStream?.close() }
      runCatching { extractor.release() }
      runCatching { pfd?.close() }
    }
  }

  private fun remuxToMp4(
    context: Context,
    sourceUri: Uri,
    outFile: File,
    onProgress: (Float) -> Unit,
  ): Boolean {
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var pfd: ParcelFileDescriptor? = null
    try {
      pfd = openSource(context, extractor, sourceUri)
      muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      val trackIndexMap = mutableMapOf<Int, Int>()
      var maxDurationUs = 1L
      var maxBufferSize = 512 * 1024

      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
          val muxIdx =
            runCatching { muxer.addTrack(fmt) }.getOrNull() ?: continue
          trackIndexMap[i] = muxIdx
          extractor.selectTrack(i)
          if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
            val dur = fmt.getLong(MediaFormat.KEY_DURATION)
            if (dur > maxDurationUs) maxDurationUs = dur
          }
          if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            val sz = fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            if (sz > maxBufferSize) maxBufferSize = sz
          }
        }
      }

      if (trackIndexMap.isEmpty()) return false
      muxer.start()

      val buffer = ByteBuffer.allocate(maxBufferSize)
      val bufferInfo = MediaCodec.BufferInfo()

      while (true) {
        val sampleTrack = extractor.sampleTrackIndex
        if (sampleTrack < 0) break
        val sampleSize = extractor.readSampleData(buffer, 0)
        if (sampleSize < 0) break

        val targetIdx = trackIndexMap[sampleTrack]
        if (targetIdx != null) {
          bufferInfo.offset = 0
          bufferInfo.size = sampleSize
          bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
          val sampleFlags = extractor.sampleFlags
          bufferInfo.flags =
            if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
              MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else {
              0
            }
          buffer.rewind()
          muxer.writeSampleData(targetIdx, buffer, bufferInfo)
          onProgress(
            0.05f + 0.88f * (bufferInfo.presentationTimeUs.toFloat() / maxDurationUs.toFloat()).coerceIn(0f, 1f)
          )
        }
        extractor.advance()
      }

      muxer.stop()
      return outFile.exists() && outFile.length() > 0L
    } finally {
      runCatching { muxer?.release() }
      runCatching { extractor.release() }
      runCatching { pfd?.close() }
    }
  }

  private fun openSource(
    context: Context,
    extractor: MediaExtractor,
    uri: Uri,
  ): ParcelFileDescriptor? {
    val pfd =
      if (uri.scheme == "content") {
        context.contentResolver.openFileDescriptor(uri, "r")
      } else {
        null
      }
    if (pfd != null) {
      extractor.setDataSource(pfd.fileDescriptor)
    } else {
      extractor.setDataSource(context, uri, null)
    }
    return pfd
  }

  private fun sanitizeFilename(raw: String): String =
    raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(60)
}
