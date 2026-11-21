package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistWithStatsRow(
  val id: Long,
  val name: String,
  val coverPath: String?,
  val sortOption: String,
  val createdAtMs: Long,
  val trackCount: Int,
  val totalDurationMs: Long,
  val isFavorites: Boolean,
)

@Dao
interface PlaylistDao {
  @Query(
    """
    SELECT p.id, p.name, p.coverPath, p.sortOption, p.createdAtMs,
           COUNT(pt.id) AS trackCount,
           COALESCE(SUM(pt.durationMs), 0) AS totalDurationMs,
           p.isFavorites
    FROM playlists p
    LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
    GROUP BY p.id
    ORDER BY p.isFavorites DESC, p.createdAtMs DESC
    """
  )
  fun observePlaylistsWithStats(): Flow<List<PlaylistWithStatsRow>>

  @Query("SELECT * FROM playlists WHERE isFavorites = 1 LIMIT 1")
  suspend fun getFavoritesPlaylist(): PlaylistEntity?

  @Query(
    """
    SELECT pt.contentUri FROM playlist_tracks pt
    INNER JOIN playlists p ON pt.playlistId = p.id
    WHERE p.isFavorites = 1
    """
  )
  fun observeFavoriteContentUris(): Flow<List<String>>

  @Query("SELECT * FROM playlists WHERE id = :playlistId")
  fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

  @Query("SELECT * FROM playlists WHERE id = :playlistId")
  suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Update
  suspend fun updatePlaylist(playlist: PlaylistEntity)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylistById(playlistId: Long)

  @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
  suspend fun renamePlaylist(playlistId: Long, newName: String)

  @Query("UPDATE playlists SET coverPath = :coverPath WHERE id = :playlistId")
  suspend fun updatePlaylistCover(playlistId: Long, coverPath: String?)

  @Query("UPDATE playlists SET sortOption = :sortOption WHERE id = :playlistId")
  suspend fun updatePlaylistSortOption(playlistId: Long, sortOption: String)

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY manualOrder ASC, id ASC")
  fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT COALESCE(MAX(manualOrder), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun getMaxManualOrder(playlistId: Long): Int

  @Query(
    "SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId AND (contentUri = :contentUri OR trackId = :trackId)"
  )
  suspend fun countTrackInPlaylistByUriOrId(
    playlistId: Long,
    contentUri: String,
    trackId: Long,
  ): Int

  @Query(
    "DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND (contentUri = :contentUri OR trackId = :trackId)"
  )
  suspend fun removeTrackByPlaylistAndUriOrId(
    playlistId: Long,
    contentUri: String,
    trackId: Long,
  )

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

  @Query("DELETE FROM playlist_tracks WHERE id = :entryId")
  suspend fun removeTrackByEntryId(entryId: Long)

  @Query("UPDATE playlist_tracks SET manualOrder = :order WHERE id = :entryId")
  suspend fun updateTrackOrder(entryId: Long, order: Int)

  @Query("SELECT * FROM playlists ORDER BY isFavorites DESC, createdAtMs ASC")
  suspend fun getAllPlaylistsSnapshot(): List<PlaylistEntity>

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY manualOrder ASC, id ASC")
  suspend fun getPlaylistTracksSnapshot(playlistId: Long): List<PlaylistTrackEntity>

  @Transaction
  suspend fun reorderPlaylistTracks(orderedEntryIds: List<Long>) {
    orderedEntryIds.forEachIndexed { index, entryId ->
      updateTrackOrder(entryId, index)
    }
  }
}

@Dao
interface PlayHistoryDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlayEvent(event: PlayHistoryEntity): Long

  @Update
  suspend fun updatePlayEvent(event: PlayHistoryEntity)

  @Query("DELETE FROM play_history WHERE contentUri = :contentUri AND id != :keepId")
  suspend fun deleteDuplicatesForUri(contentUri: String, keepId: Long)

  @Query("UPDATE play_history SET listenedMs = listenedMs + :deltaMs WHERE id = :eventId")
  suspend fun incrementListenedMs(eventId: Long, deltaMs: Long)

  @Query("UPDATE play_history SET lastPositionMs = :lastPositionMs WHERE id = :eventId")
  suspend fun updateLastPosition(eventId: Long, lastPositionMs: Long)

  @Query("UPDATE play_history SET lastPositionMs = 0 WHERE contentUri = :contentUri AND lastPositionMs > 0")
  suspend fun resetLastPositionForUri(contentUri: String)

  @Query("SELECT * FROM play_history WHERE contentUri = :contentUri ORDER BY playedAtMs DESC LIMIT 1")
  suspend fun getLatestHistoryForUri(contentUri: String): PlayHistoryEntity?

  @Query("SELECT * FROM play_history WHERE playedAtMs >= :sinceMs ORDER BY playedAtMs DESC")
  fun observeHistorySince(sinceMs: Long): Flow<List<PlayHistoryEntity>>

  @Query("SELECT * FROM play_history ORDER BY playedAtMs DESC")
  fun observeAllHistory(): Flow<List<PlayHistoryEntity>>

  @Query("SELECT * FROM play_history ORDER BY playedAtMs DESC")
  suspend fun getAllHistorySnapshot(): List<PlayHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlayEvents(events: List<PlayHistoryEntity>)

  @Query("UPDATE play_history SET listenedMs = :listenedMs WHERE id = :eventId")
  suspend fun updateListenedMs(eventId: Long, listenedMs: Long)

  @Query("DELETE FROM play_history WHERE id = :eventId")
  suspend fun deleteHistoryEvent(eventId: Long)

  @Query("DELETE FROM play_history")
  suspend fun clearAllHistory()

  @Query("DELETE FROM play_history WHERE playedAtMs < :olderThanMs")
  suspend fun pruneHistoryOlderThan(olderThanMs: Long)
}

@Dao
interface CachedMediaDao {
  @Query("SELECT * FROM cached_media ORDER BY dateModifiedMs DESC")
  fun observeAllCachedMedia(): Flow<List<CachedMediaEntity>>

  @Query("SELECT * FROM cached_media ORDER BY dateModifiedMs DESC")
  suspend fun getAllCachedMediaSnapshot(): List<CachedMediaEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<CachedMediaEntity>)

  @Query("DELETE FROM cached_media WHERE contentUri IN (:uris)")
  suspend fun deleteByUris(uris: List<String>)

  @Query("DELETE FROM cached_media WHERE contentUri = :contentUri OR trackId = :trackId")
  suspend fun deleteByUriOrId(contentUri: String, trackId: Long)

  @Transaction
  suspend fun applySyncDiff(
    upserts: List<CachedMediaEntity>,
    deletedUris: List<String>,
  ) {
    if (deletedUris.isNotEmpty()) {
      deletedUris.chunked(450).forEach { batch ->
        deleteByUris(batch)
      }
    }
    if (upserts.isNotEmpty()) {
      upserts.chunked(450).forEach { batch ->
        upsertAll(batch)
      }
    }
  }
}

