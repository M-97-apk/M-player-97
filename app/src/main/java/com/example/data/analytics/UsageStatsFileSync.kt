package com.example.data.analytics

import android.content.Context
import android.net.Uri
import com.example.data.db.PlayHistoryEntity
import com.example.model.AnalyticsTimeframe
import com.example.model.WeeklySummaryReport
import com.example.model.WeeklyTopTrack
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

data class UsageFileSyncResult(
  val success: Boolean,
  val syncedAtIso: String,
  val internalFilePath: String,
  val externalSynced: Boolean,
  val errorMessage: String? = null,
)

data class UsageImportResult(
  val success: Boolean,
  val importedCount: Int = 0,
  val skippedDuplicates: Int = 0,
  val errorMessage: String? = null,
)

object UsageStatsEngine {

  const val USAGE_STATS_FILENAME = "mplayer97_usage_stats.json"

  fun formatIsoTimestamp(epochMs: Long): String {
    val sdf =
      SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
    return sdf.format(Date(epochMs))
  }

  fun parseIsoTimestampOrNull(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (pattern in patterns) {
      val parsed =
        runCatching {
            SimpleDateFormat(pattern, Locale.US)
              .apply { timeZone = TimeZone.getTimeZone("UTC") }
              .parse(iso.trim())
              ?.time
          }
          .getOrNull()
      if (parsed != null) return parsed
    }
    return null
  }

  fun computeTimeframeStartMs(
    timeframe: AnalyticsTimeframe,
    nowMs: Long = System.currentTimeMillis(),
  ): Long {
    val cal =
      Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    when (timeframe) {
      AnalyticsTimeframe.DAILY -> Unit // Start of today (00:00:00.000 local)
      AnalyticsTimeframe.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, -6)
      AnalyticsTimeframe.MONTHLY -> cal.add(Calendar.DAY_OF_YEAR, -29)
      AnalyticsTimeframe.YEARLY -> cal.add(Calendar.DAY_OF_YEAR, -364)
    }
    return cal.timeInMillis
  }

  fun aggregateUsageReport(
    events: List<PlayHistoryEntity>,
    timeframe: AnalyticsTimeframe,
    nowMs: Long = System.currentTimeMillis(),
  ): WeeklySummaryReport {
    val startMs = computeTimeframeStartMs(timeframe, nowMs)
    val filtered = events.filter { it.playedAtMs in startMs..nowMs }
    if (filtered.isEmpty()) {
      return WeeklySummaryReport(
        topTracks = emptyList(),
        totalPlaysThisWeek = 0,
        totalListeningMinutes = 0L,
        uniqueTracksPlayed = 0,
        timeframe = timeframe,
        totalListenedMs = 0L,
        totalListeningSeconds = 0L,
      )
    }

    val grouped = filtered.groupBy { it.contentUri.ifBlank { "id:${it.trackId}" } }
    val ranked =
      grouped.values
        .map { group ->
          val latest = group.maxByOrNull { it.playedAtMs } ?: group.first()
          val trackListenedMs = group.sumOf { it.listenedMs.coerceAtLeast(0L) }
          val trackPlayCount = group.sumOf { it.playCount.coerceAtLeast(1) }
          WeeklyTopTrack(
            trackId = latest.trackId,
            title = latest.title,
            artist = latest.artist,
            album = latest.album,
            albumArtUri = latest.albumArtUri,
            contentUri = latest.contentUri,
            durationMs = latest.durationMs,
            playCount = trackPlayCount,
            listenedMs = trackListenedMs,
            lastPlayedAtMs = latest.playedAtMs,
            lastPlayedAtIso =
              latest.playedAtIso.ifBlank { formatIsoTimestamp(latest.playedAtMs) },
          )
        }
        .sortedWith(
          compareByDescending<WeeklyTopTrack> { it.playCount }
            .thenByDescending { it.listenedMs }
            .thenByDescending { it.lastPlayedAtMs }
        )

    val totalListenedMs = filtered.sumOf { it.listenedMs.coerceAtLeast(0L) }
    val totalPlays = filtered.sumOf { it.playCount.coerceAtLeast(1) }
    val totalSeconds = totalListenedMs / 1000L
    val totalMinutes = totalListenedMs / 60_000L

    return WeeklySummaryReport(
      topTracks = ranked.take(7),
      totalPlaysThisWeek = totalPlays,
      totalListeningMinutes = totalMinutes,
      uniqueTracksPlayed = grouped.size,
      timeframe = timeframe,
      totalListenedMs = totalListenedMs,
      totalListeningSeconds = totalSeconds,
    )
  }

  fun buildUsageStatsJson(
    events: List<PlayHistoryEntity>,
    nowMs: Long = System.currentTimeMillis(),
  ): String {
    val updatedAtIso = formatIsoTimestamp(nowMs)
    val root = JSONObject()
    root.put("app", "M player 97")
    root.put("schemaVersion", 1)
    root.put("updatedAtIso", updatedAtIso)

    val summariesObj = JSONObject()
    AnalyticsTimeframe.entries.forEach { tf ->
      val report = aggregateUsageReport(events, tf, nowMs)
      val tfObj = JSONObject()
      tfObj.put("plays", report.totalPlaysThisWeek)
      tfObj.put("listenedMs", report.totalListenedMs)
      tfObj.put("listenedSeconds", report.totalListeningSeconds)
      tfObj.put("listenedMinutes", report.totalListeningMinutes)
      tfObj.put("uniqueTracks", report.uniqueTracksPlayed)
      summariesObj.put(tf.name.lowercase(Locale.US), tfObj)
    }
    root.put("summaries", summariesObj)

    val eventsArray = JSONArray()
    events.sortedByDescending { it.playedAtMs }.forEach { ev ->
      val item = JSONObject()
      item.put("id", ev.id)
      item.put("trackId", ev.trackId)
      item.put("title", ev.title)
      item.put("artist", ev.artist)
      item.put("album", ev.album)
      item.put("contentUri", ev.contentUri)
      if (!ev.albumArtUri.isNullOrBlank()) {
        item.put("albumArtUri", ev.albumArtUri)
      }
      item.put("playedAtMs", ev.playedAtMs)
      item.put("playedAtIso", ev.playedAtIso.ifBlank { formatIsoTimestamp(ev.playedAtMs) })
      item.put("listenedMs", ev.listenedMs.coerceAtLeast(0L))
      item.put("listenedSeconds", ev.listenedMs.coerceAtLeast(0L) / 1000L)
      item.put("trackDurationMs", ev.durationMs)
      item.put("lastPositionMs", ev.lastPositionMs.coerceAtLeast(0L))
      item.put("mediaType", ev.mediaType.ifBlank { "AUDIO" })
      item.put("playCount", ev.playCount.coerceAtLeast(1))
      eventsArray.put(item)
    }
    root.put("events", eventsArray)

    return root.toString(2)
  }

  fun parseUsageStatsJson(jsonText: String): List<PlayHistoryEntity> {
    val trimmed = jsonText.trim()
    if (trimmed.isEmpty()) return emptyList()
    val root = JSONObject(trimmed)
    val eventsArray = root.optJSONArray("events") ?: return emptyList()
    val parsed = ArrayList<PlayHistoryEntity>(eventsArray.length())

    for (i in 0 until eventsArray.length()) {
      val item = eventsArray.optJSONObject(i) ?: continue
      val title = item.optString("title", "").trim()
      if (title.isEmpty()) continue
      val playedAtIso = item.optString("playedAtIso", "").trim()
      val rawPlayedAtMs = item.optLong("playedAtMs", 0L)
      val playedAtMs =
        when {
          rawPlayedAtMs > 0L -> rawPlayedAtMs
          else -> parseIsoTimestampOrNull(playedAtIso) ?: continue
        }
      val resolvedIso = playedAtIso.ifBlank { formatIsoTimestamp(playedAtMs) }
      val listenedMs =
        when {
          item.has("listenedMs") -> item.optLong("listenedMs", 0L).coerceAtLeast(0L)
          item.has("listenedSeconds") -> (item.optLong("listenedSeconds", 0L) * 1000L).coerceAtLeast(0L)
          else -> 0L
        }
      val trackId = item.optLong("trackId", 0L)
      val artist = item.optString("artist", "")
      val album = item.optString("album", "")
      val contentUri = item.optString("contentUri", "").ifBlank { "id:$trackId" }
      val albumArtUri = item.optString("albumArtUri", "").takeIf { it.isNotBlank() }
      val durationMs = item.optLong("trackDurationMs", 0L).coerceAtLeast(0L)
      val lastPositionMs = item.optLong("lastPositionMs", 0L).coerceAtLeast(0L)
      val mediaType = item.optString("mediaType", "AUDIO").ifBlank { "AUDIO" }
      val playCount = item.optInt("playCount", 1).coerceAtLeast(1)

      parsed.add(
        PlayHistoryEntity(
          trackId = trackId,
          title = title,
          artist = artist,
          album = album,
          albumArtUri = albumArtUri,
          contentUri = contentUri,
          durationMs = durationMs,
          playedAtMs = playedAtMs,
          listenedMs = listenedMs,
          playedAtIso = resolvedIso,
          lastPositionMs = lastPositionMs,
          mediaType = mediaType,
          playCount = playCount,
        )
      )
    }
    return parsed
  }

  fun readTextFromUri(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
      }
      .getOrNull()

  fun syncStatsToLocalFiles(
    context: Context,
    events: List<PlayHistoryEntity>,
    externalDocumentUri: String?,
  ): UsageFileSyncResult {
    val nowMs = System.currentTimeMillis()
    val syncedIso = formatIsoTimestamp(nowMs)
    val json = buildUsageStatsJson(events, nowMs)

    val internalFile = File(context.filesDir, USAGE_STATS_FILENAME)
    val tempFile = File(context.filesDir, "$USAGE_STATS_FILENAME.tmp")
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
          return UsageFileSyncResult(
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

    return UsageFileSyncResult(
      success = internalWritten,
      syncedAtIso = syncedIso,
      internalFilePath = internalFile.absolutePath,
      externalSynced = externalSynced,
    )
  }
}
