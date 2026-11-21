package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.PlaylistSortOption

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val coverPath: String? = null,
  val sortOption: String = PlaylistSortOption.MANUAL.name,
  val createdAtMs: Long = System.currentTimeMillis(),
  val isFavorites: Boolean = false,
)

@Entity(
  tableName = "playlist_tracks",
  foreignKeys = [
    ForeignKey(
      entity = PlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("playlistId"), Index("trackId")],
)
data class PlaylistTrackEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playlistId: Long,
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

@Entity(
  tableName = "play_history",
  indices = [Index("playedAtMs"), Index("trackId"), Index("contentUri")],
)
data class PlayHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val trackId: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumArtUri: String?,
  val contentUri: String,
  val durationMs: Long,
  val playedAtMs: Long = System.currentTimeMillis(),
  val listenedMs: Long = 0L,
  val playedAtIso: String = "",
  val lastPositionMs: Long = 0L,
  val mediaType: String = "AUDIO",
  val playCount: Int = 1,
)

@Entity(
  tableName = "cached_media",
  indices = [Index("mediaType"), Index("sourceBucket"), Index("dateModifiedMs")],
)
data class CachedMediaEntity(
  @PrimaryKey val contentUri: String,
  val trackId: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  val albumArtUri: String?,
  val dateAddedMs: Long,
  val dateModifiedMs: Long,
  val sizeBytes: Long,
  val mimeType: String,
  val folderName: String,
  val mediaType: String,
  val sourceBucket: String,
)

