package com.example.data.media

import de.sciss.jump3r.mp3.BitStream
import de.sciss.jump3r.mp3.GainAnalysis
import de.sciss.jump3r.mp3.GetAudio
import de.sciss.jump3r.mp3.ID3Tag
import de.sciss.jump3r.mp3.Lame
import de.sciss.jump3r.mp3.LameGlobalFlags
import de.sciss.jump3r.mp3.MPEGMode
import de.sciss.jump3r.mp3.Parse
import de.sciss.jump3r.mp3.Presets
import de.sciss.jump3r.mp3.Quantize
import de.sciss.jump3r.mp3.QuantizePVT
import de.sciss.jump3r.mp3.Reservoir
import de.sciss.jump3r.mp3.Takehiro
import de.sciss.jump3r.mp3.VBRTag
import de.sciss.jump3r.mp3.VbrMode
import de.sciss.jump3r.mp3.Version
import de.sciss.jump3r.mpg.Common
import de.sciss.jump3r.mpg.Interface
import de.sciss.jump3r.mpg.MPGLib
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-quality MPEG-1 Layer III (320 kbps CBR, 44.1 kHz) pure-JVM MP3 encoder powered by LAME
 * (via Jump3r). Accepts both 16-bit signed PCM and 32-bit IEEE float PCM from Android MediaCodec,
 * resamples cleanly, writes UTF-8 ID3v2.3 tags, and produces zero-noise standard .mp3 streams
 * with zero native (.so) binaries or Dynamic Code Loading.
 */
class PureMp3Encoder(
  private val inputSampleRate: Int,
  private val inputChannels: Int,
  private val output: OutputStream,
  private val title: String = "",
  private val artist: String = "",
  private val album: String = "",
) {
  companion object {
    const val OUTPUT_SAMPLE_RATE = 44100
    const val BITRATE_KBPS = 320
    private const val CHUNK_SAMPLES = 4096
  }

  private val safeInputChannels = inputChannels.coerceAtLeast(1)
  private val outputChannels = if (safeInputChannels == 1) 1 else 2

  private val lame = Lame()
  private val gaud = GetAudio()
  private val ga = GainAnalysis()
  private val bs = BitStream()
  private val p = Presets()
  private val qupvt = QuantizePVT()
  private val qu = Quantize()
  private val vbr = VBRTag()
  private val ver = Version()
  private val id3 = ID3Tag()
  private val rv = Reservoir()
  private val tak = Takehiro()
  private val parse = Parse()
  private val mpg = MPGLib()
  private val intf = Interface()
  private val common = Common()
  private val gfp: LameGlobalFlags

  private var id3Written = false
  private var finished = false
  private var mp3DataBytesWritten = 0L

  var framesWritten: Int = 0
    private set

  private val mp3Buffer = ByteArray((1.25 * CHUNK_SAMPLES + 8192).toInt())

  init {
    lame.setModules(ga, bs, p, qupvt, qu, vbr, ver, id3, mpg)
    bs.setModules(ga, mpg, ver, vbr)
    id3.setModules(bs, ver)
    p.setModules(lame)
    qu.setModules(bs, rv, qupvt, tak)
    qupvt.setModules(tak, rv, lame.enc.psy)
    rv.setModules(bs)
    tak.setModules(qupvt)
    vbr.setModules(lame, bs, ver)
    gaud.setModules(parse, mpg)
    parse.setModules(ver, id3, p)
    mpg.setModules(intf, common)
    intf.setModules(vbr, common)

    gfp = lame.lame_init()
    gfp.num_channels = outputChannels
    gfp.in_samplerate = inputSampleRate.coerceIn(8000, 192000)
    gfp.out_samplerate = OUTPUT_SAMPLE_RATE
    gfp.mode = if (outputChannels == 1) MPEGMode.MONO else MPEGMode.JOINT_STEREO
    gfp.VBR = VbrMode.vbr_off
    gfp.brate = BITRATE_KBPS
    gfp.quality = 2
    gfp.bWriteVbrTag = false
    gfp.write_id3tag_automatic = false
    id3.id3tag_init(gfp)

    val initResult = lame.lame_init_params(gfp)
    require(initResult >= 0) { "MP3 encoder initialization failed with code $initResult" }
  }

  private fun ensureId3Header() {
    if (id3Written) return
    id3Written = true
    if (title.isBlank() && artist.isBlank() && album.isBlank()) return

    val frames = mutableListOf<ByteArray>()
    if (title.isNotBlank()) frames.add(buildId3TextFrame("TIT2", title))
    if (artist.isNotBlank()) frames.add(buildId3TextFrame("TPE1", artist))
    if (album.isNotBlank()) frames.add(buildId3TextFrame("TALB", album))
    val payloadSize = frames.sumOf { it.size }
    val header =
      byteArrayOf(
        'I'.code.toByte(),
        'D'.code.toByte(),
        '3'.code.toByte(),
        3,
        0,
        0,
        ((payloadSize ushr 21) and 0x7F).toByte(),
        ((payloadSize ushr 14) and 0x7F).toByte(),
        ((payloadSize ushr 7) and 0x7F).toByte(),
        (payloadSize and 0x7F).toByte(),
      )
    output.write(header)
    frames.forEach { output.write(it) }
  }

  private fun buildId3TextFrame(frameId: String, text: String): ByteArray {
    val textBytes = text.toByteArray(Charsets.UTF_8)
    val contentSize = 1 + textBytes.size
    val out = ByteArray(10 + contentSize)
    for (i in 0..3) out[i] = frameId[i].code.toByte()
    out[4] = ((contentSize ushr 24) and 0xFF).toByte()
    out[5] = ((contentSize ushr 16) and 0xFF).toByte()
    out[6] = ((contentSize ushr 8) and 0xFF).toByte()
    out[7] = (contentSize and 0xFF).toByte()
    out[8] = 0
    out[9] = 0
    out[10] = 3 // UTF-8 text encoding
    System.arraycopy(textBytes, 0, out, 11, textBytes.size)
    return out
  }

  fun feedPcm16(pcmBytes: ByteArray, lengthBytes: Int) {
    if (finished || lengthBytes <= 0) return
    ensureId3Header()

    val bytesPerFrame = 2 * safeInputChannels
    val totalSamples = lengthBytes / bytesPerFrame
    if (totalSamples <= 0) return

    val buf = ByteBuffer.wrap(pcmBytes, 0, totalSamples * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
    val leftChunk = IntArray(minOf(totalSamples, CHUNK_SAMPLES))
    val rightChunk = IntArray(minOf(totalSamples, CHUNK_SAMPLES))

    var remaining = totalSamples
    while (remaining > 0) {
      val batch = minOf(remaining, CHUNK_SAMPLES)
      for (i in 0 until batch) {
        when (safeInputChannels) {
          1 -> {
            val s = buf.short.toInt() shl 16
            leftChunk[i] = s
            rightChunk[i] = s
          }
          2 -> {
            leftChunk[i] = buf.short.toInt() shl 16
            rightChunk[i] = buf.short.toInt() shl 16
          }
          6 -> {
            // 5.1 Surround -> balanced stereo downmix
            val fl = buf.short.toFloat() / 32768f
            val fr = buf.short.toFloat() / 32768f
            val c = buf.short.toFloat() / 32768f
            val lfe = buf.short.toFloat() / 32768f
            val sl = buf.short.toFloat() / 32768f
            val sr = buf.short.toFloat() / 32768f
            val l = ((fl + 0.707f * c + 0.707f * sl + 0.3f * lfe) * 0.65f).coerceIn(-1f, 1f)
            val r = ((fr + 0.707f * c + 0.707f * sr + 0.3f * lfe) * 0.65f).coerceIn(-1f, 1f)
            leftChunk[i] = (l * 2147483647f).toInt()
            rightChunk[i] = (r * 2147483647f).toInt()
          }
          else -> {
            var lSum = 0f
            var rSum = 0f
            for (ch in 0 until safeInputChannels) {
              val s = buf.short.toFloat() / 32768f
              if (ch % 2 == 0) lSum += s else rSum += s
            }
            val div = (safeInputChannels / 2f).coerceAtLeast(1f)
            leftChunk[i] = ((lSum / div).coerceIn(-1f, 1f) * 2147483647f).toInt()
            rightChunk[i] = ((rSum / div).coerceIn(-1f, 1f) * 2147483647f).toInt()
          }
        }
      }
      encodeIntBatch(leftChunk, rightChunk, batch)
      remaining -= batch
    }
  }

  fun feedPcmFloat(floatBytes: ByteArray, lengthBytes: Int) {
    if (finished || lengthBytes <= 0) return
    ensureId3Header()

    val bytesPerFrame = 4 * safeInputChannels
    val totalSamples = lengthBytes / bytesPerFrame
    if (totalSamples <= 0) return

    val buf = ByteBuffer.wrap(floatBytes, 0, totalSamples * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
    val leftChunk = IntArray(minOf(totalSamples, CHUNK_SAMPLES))
    val rightChunk = IntArray(minOf(totalSamples, CHUNK_SAMPLES))

    var remaining = totalSamples
    while (remaining > 0) {
      val batch = minOf(remaining, CHUNK_SAMPLES)
      for (i in 0 until batch) {
        when (safeInputChannels) {
          1 -> {
            val s = (buf.float.coerceIn(-1f, 1f) * 2147483647f).toInt()
            leftChunk[i] = s
            rightChunk[i] = s
          }
          2 -> {
            leftChunk[i] = (buf.float.coerceIn(-1f, 1f) * 2147483647f).toInt()
            rightChunk[i] = (buf.float.coerceIn(-1f, 1f) * 2147483647f).toInt()
          }
          6 -> {
            val fl = buf.float
            val fr = buf.float
            val c = buf.float
            val lfe = buf.float
            val sl = buf.float
            val sr = buf.float
            val l = ((fl + 0.707f * c + 0.707f * sl + 0.3f * lfe) * 0.65f).coerceIn(-1f, 1f)
            val r = ((fr + 0.707f * c + 0.707f * sr + 0.3f * lfe) * 0.65f).coerceIn(-1f, 1f)
            leftChunk[i] = (l * 2147483647f).toInt()
            rightChunk[i] = (r * 2147483647f).toInt()
          }
          else -> {
            var lSum = 0f
            var rSum = 0f
            for (ch in 0 until safeInputChannels) {
              val s = buf.float
              if (ch % 2 == 0) lSum += s else rSum += s
            }
            val div = (safeInputChannels / 2f).coerceAtLeast(1f)
            leftChunk[i] = ((lSum / div).coerceIn(-1f, 1f) * 2147483647f).toInt()
            rightChunk[i] = ((rSum / div).coerceIn(-1f, 1f) * 2147483647f).toInt()
          }
        }
      }
      encodeIntBatch(leftChunk, rightChunk, batch)
      remaining -= batch
    }
  }

  private fun encodeIntBatch(leftChunk: IntArray, rightChunk: IntArray, sampleCount: Int) {
    val encoded =
      lame.lame_encode_buffer_int(
        gfp,
        leftChunk,
        rightChunk,
        sampleCount,
        mp3Buffer,
        0,
        mp3Buffer.size,
      )
    if (encoded > 0) {
      output.write(mp3Buffer, 0, encoded)
      mp3DataBytesWritten += encoded
      framesWritten = gfp.frameNum
    }
  }

  fun finish() {
    if (finished) return
    finished = true
    ensureId3Header()
    try {
      val flushed = lame.lame_encode_flush(gfp, mp3Buffer, 0, mp3Buffer.size)
      if (flushed > 0) {
        output.write(mp3Buffer, 0, flushed)
        mp3DataBytesWritten += flushed
      }
      framesWritten =
        maxOf(
          gfp.frameNum,
          if (mp3DataBytesWritten > 0L) 1 else 0,
        )
    } finally {
      runCatching { lame.lame_close(gfp) }
      output.flush()
    }
  }
}
