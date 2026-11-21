package com.example.data.analytics

import android.content.Context
import android.net.Uri
import com.example.model.PlaylistSortOption
import java.io.File
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupPlaylistTrack(
  val trackId: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  val contentUri: String,
  val albumArtUri: String?,
  val dateAddedDeviceMs: Long,
  val dateModifiedDeviceMs: Long,
  val sizeBytes: Long,
  val mimeType: String,
  val folderName: String,
  val addedToPlaylistAtMs: Long,
  val manualOrder: Int,
  val mediaType: String = "AUDIO",
)

data class BackupPlaylist(
  val name: String,
  val sortOption: String = PlaylistSortOption.MANUAL.name,
  val createdAtMs: Long = System.currentTimeMillis(),
  val tracks: List<BackupPlaylistTrack> = emptyList(),
  val isFavorites: Boolean = false,
)

data class PlaylistBackupSyncResult(
  val success: Boolean,
  val syncedAtIso: String,
  val internalFilePath: String,
  val externalSynced: Boolean,
  val archivedPlaylistCount: Int = 0,
  val errorMessage: String? = null,
)

data class PlaylistImportResult(
  val success: Boolean,
  val importedPlaylistsCount: Int = 0,
  val importedTracksCount: Int = 0,
  val errorMessage: String? = null,
)

object PlaylistBackupEngine {

  const val PLAYLIST_BACKUP_FILENAME = "mplayer97_playlists.json"

  private fun playlistMergeKey(playlist: BackupPlaylist): String =
    "${playlist.name.trim().lowercase(Locale.US)}|${playlist.createdAtMs}"

  private fun trackMergeKey(track: BackupPlaylistTrack): String =
    track.contentUri.ifBlank { "id:${track.trackId}|${track.title.trim().lowercase(Locale.US)}" }

  /**
   * Strictly add-only merge:
   * - Never deletes playlists that were already in [existingArchive], even if removed inside the app.
   * - Never deletes archived tracks from existing playlists; only appends newly added playlists or tracks.
   */
  fun mergePlaylistsAddOnly(
    existingArchive: List<BackupPlaylist>,
    incomingPlaylists: List<BackupPlaylist>,
  ): List<BackupPlaylist> {
    val merged = LinkedHashMap<String, BackupPlaylist>()

    for (archived in existingArchive) {
      val key = playlistMergeKey(archived)
      merged[key] = archived
    }

    for (incoming in incomingPlaylists) {
      val exactKey = playlistMergeKey(incoming)
      val matchedKey =
        if (merged.containsKey(exactKey)) {
          exactKey
        } else {
          val normalizedName = incoming.name.trim().lowercase(Locale.US)
          merged.entries
            .firstOrNull { it.value.name.trim().lowercase(Locale.US) == normalizedName }
            ?.key
        }

      if (matchedKey == null) {
        merged[exactKey] = incoming
      } else {
        val current = merged.getValue(matchedKey)
        val seenTrackKeys = HashSet<String>(current.tracks.size)
        val combinedTracks = ArrayList<BackupPlaylistTrack>(current.tracks.size + incoming.tracks.size)
        for (t in current.tracks) {
          seenTrackKeys.add(trackMergeKey(t))
          combinedTracks.add(t)
        }
        var nextOrder = (combinedTracks.maxOfOrNull { it.manualOrder } ?: -1) + 1
        for (t in incoming.tracks) {
          val tKey = trackMergeKey(t)
          if (seenTrackKeys.add(tKey)) {
            combinedTracks.add(t.copy(manualOrder = nextOrder++))
          }
        }
        merged[matchedKey] =
          current.copy(
            sortOption = incoming.sortOption.ifBlank { current.sortOption },
            tracks = combinedTracks,
          )
      }
    }

    return merged.values.toList()
  }

  fun buildPlaylistsJson(
    playlists: List<BackupPlaylist>,
    nowMs: Long = System.currentTimeMillis(),
  ): String {
    val root = JSONObject()
    root.put("app", "M player 97")
    root.put("schemaVersion", 1)
    root.put("syncMode", "add_only")
    root.put("updatedAtIso", UsageStatsEngine.formatIsoTimestamp(nowMs))

    val playlistsArray = JSONArray()
    playlists.forEach { playlist ->
      val pObj = JSONObject()
      pObj.put("name", playlist.name)
      pObj.put("sortOption", playlist.sortOption)
      pObj.put("createdAtMs", playlist.createdAtMs)
      pObj.put("createdAtIso", UsageStatsEngine.formatIsoTimestamp(playlist.createdAtMs))
      pObj.put("isFavorites", playlist.isFavorites)

      val tracksArray = JSONArray()
      playlist.tracks.forEach { track ->
        val tObj = JSONObject()
        tObj.put("trackId", track.trackId)
        tObj.put("title", track.title)
        tObj.put("artist", track.artist)
        tObj.put("album", track.album)
        tObj.put("albumId", track.albumId)
        tObj.put("durationMs", track.durationMs)
        tObj.put("contentUri", track.contentUri)
        if (!track.albumArtUri.isNullOrBlank()) {
          tObj.put("albumArtUri", track.albumArtUri)
        }
        tObj.put("dateAddedDeviceMs", track.dateAddedDeviceMs)
        tObj.put("dateModifiedDeviceMs", track.dateModifiedDeviceMs)
        tObj.put("sizeBytes", track.sizeBytes)
        tObj.put("mimeType", track.mimeType)
        tObj.put("folderName", track.folderName)
        tObj.put("addedToPlaylistAtMs", track.addedToPlaylistAtMs)
        tObj.put("manualOrder", track.manualOrder)
        tObj.put("mediaType", track.mediaType)
        tracksArray.put(tObj)
      }
      pObj.put("tracks", tracksArray)
      playlistsArray.put(pObj)
    }
    root.put("playlists", playlistsArray)

    return root.toString(2)
  }

  fun parsePlaylistsJson(jsonText: String): List<BackupPlaylist> {
    val trimmed = jsonText.trim()
    if (trimmed.isEmpty()) return emptyList()
    val root = JSONObject(trimmed)
    val playlistsArray = root.optJSONArray("playlists") ?: return emptyList()
    val result = ArrayList<BackupPlaylist>(playlistsArray.length())

    for (i in 0 until playlistsArray.length()) {
      val pObj = playlistsArray.optJSONObject(i) ?: continue
      val name = pObj.optString("name", "").trim()
      if (name.isEmpty()) continue
      val sortOption =
        pObj.optString("sortOption", PlaylistSortOption.MANUAL.name)
          .ifBlank { PlaylistSortOption.MANUAL.name }
      val createdAtMs =
        pObj.optLong("createdAtMs", 0L).takeIf { it > 0L }
          ?: UsageStatsEngine.parseIsoTimestampOrNull(pObj.optString("createdAtIso", ""))
          ?: System.currentTimeMillis()

      val tracksArray = pObj.optJSONArray("tracks")
      val tracks = ArrayList<BackupPlaylistTrack>(tracksArray?.length() ?: 0)
      if (tracksArray != null) {
        for (j in 0 until tracksArray.length()) {
          val tObj = tracksArray.optJSONObject(j) ?: continue
          val title = tObj.optString("title", "").trim()
          if (title.isEmpty()) continue
          val trackId = tObj.optLong("trackId", 0L)
          val contentUri = tObj.optString("contentUri", "").trim()
          if (contentUri.isEmpty() && trackId == 0L) continue

          tracks.add(
            BackupPlaylistTrack(
              trackId = trackId,
              title = title,
              artist = tObj.optString("artist", ""),
              album = tObj.optString("album", ""),
              albumId = tObj.optLong("albumId", 0L),
              durationMs = tObj.optLong("durationMs", 0L).coerceAtLeast(0L),
              contentUri = contentUri.ifBlank { "id:$trackId" },
              albumArtUri = tObj.optString("albumArtUri", "").takeIf { it.isNotBlank() },
              dateAddedDeviceMs = tObj.optLong("dateAddedDeviceMs", 0L),
              dateModifiedDeviceMs = tObj.optLong("dateModifiedDeviceMs", 0L),
              sizeBytes = tObj.optLong("sizeBytes", 0L),
              mimeType = tObj.optString("mimeType", "audio/*"),
              folderName = tObj.optString("folderName", ""),
              addedToPlaylistAtMs = tObj.optLong("addedToPlaylistAtMs", createdAtMs),
              manualOrder = tObj.optInt("manualOrder", j),
              mediaType = tObj.optString("mediaType", "AUDIO").ifBlank { "AUDIO" },
            )
          )
        }
      }

      result.add(
        BackupPlaylist(
          name = name,
          sortOption = sortOption,
          createdAtMs = createdAtMs,
          tracks = tracks,
          isFavorites = pObj.optBoolean("isFavorites", false),
        )
      )
    }
    return result
  }

  fun syncPlaylistsAddOnlyToFiles(
    context: Context,
    currentPlaylists: List<BackupPlaylist>,
    externalDocumentUri: String?,
  ): PlaylistBackupSyncResult {
    val nowMs = System.currentTimeMillis()
    val syncedIso = UsageStatsEngine.formatIsoTimestamp(nowMs)
    val internalFile = File(context.filesDir, PLAYLIST_BACKUP_FILENAME)

    // Read existing archive from external SAF document first (fallback to internal archive)
    // so deleted in-app playlists remain safely preserved in the backup archive.
    val existingFromExternal =
      externalDocumentUri
        ?.takeIf { it.isNotBlank() }
        ?.let { uriStr ->
          runCatching {
              val raw = UsageStatsEngine.readTextFromUri(context, Uri.parse(uriStr))
              if (!raw.isNullOrBlank()) parsePlaylistsJson(raw) else emptyList()
            }
            .getOrDefault(emptyList())
        }
        ?: emptyList()

    val existingFromInternal =
      runCatching {
          if (internalFile.exists() && internalFile.length() > 0L) {
            parsePlaylistsJson(internalFile.readText(Charsets.UTF_8))
          } else {
            emptyList()
          }
        }
        .getOrDefault(emptyList())

    val combinedArchive = mergePlaylistsAddOnly(existingFromInternal, existingFromExternal)
    val finalMerged = mergePlaylistsAddOnly(combinedArchive, currentPlaylists)
    val json = buildPlaylistsJson(finalMerged, nowMs)

    val tempFile = File(context.filesDir, "$PLAYLIST_BACKUP_FILENAME.tmp")
    val internalWritten =
      runCatching {
          tempFile.writeText(json, Charsets.UTF_8)
          if (!tempFile.renameTo(internalFile)) {
            internalFile.writeText(json, Charsets.UTF_8)
            tempFile.delete()
          }
          true
        }
        .getOrElse { err ->
          return PlaylistBackupSyncResult(
            success = false,
            syncedAtIso = syncedIso,
            internalFilePath = internalFile.absolutePath,
            externalSynced = false,
            errorMessage = err.message,
          )
        }

    var externalSynced = false
    if (!externalDocumentUri.isNullOrBlank()) {
      val uri = Uri.parse(externalDocumentUri)
      val bytes = json.toByteArray(Charsets.UTF_8)
      externalSynced =
        listOf("wt", "w").any { mode ->
          runCatching {
              context.contentResolver.openOutputStream(uri, mode)?.use { out ->
                out.write(bytes)
                out.flush()
              } != null
            }
            .getOrDefault(false)
        }
    }

    return PlaylistBackupSyncResult(
      success = internalWritten,
      syncedAtIso = syncedIso,
      internalFilePath = internalFile.absolutePath,
      externalSynced = externalSynced,
      archivedPlaylistCount = finalMerged.size,
    )
  }
}
