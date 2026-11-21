package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    PlaylistEntity::class,
    PlaylistTrackEntity::class,
    PlayHistoryEntity::class,
    CachedMediaEntity::class,
  ],
  version = 5,
  exportSchema = false,
)
abstract class MPlayerDatabase : RoomDatabase() {
  abstract fun playlistDao(): PlaylistDao

  abstract fun playHistoryDao(): PlayHistoryDao

  abstract fun cachedMediaDao(): CachedMediaDao

  companion object {
    @Volatile
    private var INSTANCE: MPlayerDatabase? = null

    internal val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE play_history ADD COLUMN listenedMs INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE play_history ADD COLUMN playedAtIso TEXT NOT NULL DEFAULT ''")
        }
      }

    internal val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE playlists ADD COLUMN isFavorites INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE playlist_tracks ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'AUDIO'")
          db.execSQL("ALTER TABLE play_history ADD COLUMN lastPositionMs INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE play_history ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'AUDIO'")
        }
      }

    internal val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_media` (
              `contentUri` TEXT NOT NULL,
              `trackId` INTEGER NOT NULL,
              `title` TEXT NOT NULL,
              `artist` TEXT NOT NULL,
              `album` TEXT NOT NULL,
              `albumId` INTEGER NOT NULL,
              `durationMs` INTEGER NOT NULL,
              `albumArtUri` TEXT,
              `dateAddedMs` INTEGER NOT NULL,
              `dateModifiedMs` INTEGER NOT NULL,
              `sizeBytes` INTEGER NOT NULL,
              `mimeType` TEXT NOT NULL,
              `folderName` TEXT NOT NULL,
              `mediaType` TEXT NOT NULL,
              `sourceBucket` TEXT NOT NULL,
              PRIMARY KEY(`contentUri`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_media_mediaType` ON `cached_media` (`mediaType`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_media_sourceBucket` ON `cached_media` (`sourceBucket`)"
          )
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_media_dateModifiedMs` ON `cached_media` (`dateModifiedMs`)"
          )
        }
      }

    internal val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE play_history ADD COLUMN playCount INTEGER NOT NULL DEFAULT 1")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_play_history_contentUri` ON `play_history` (`contentUri`)"
          )
        }
      }

    fun getInstance(context: Context): MPlayerDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                MPlayerDatabase::class.java,
                "mplayer97_offline.db",
              )
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
  }
}
