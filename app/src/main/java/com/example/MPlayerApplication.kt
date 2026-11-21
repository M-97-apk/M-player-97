package com.example

import android.app.Application
import com.example.data.db.MPlayerDatabase
import com.example.data.media.AudioScanner
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerController

class MPlayerApplication : Application() {
  val database: MPlayerDatabase by lazy { MPlayerDatabase.getInstance(this) }
  val prefsRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
  val audioScanner: AudioScanner by lazy { AudioScanner(this) }
  val musicRepository: MusicRepository by lazy {
    MusicRepository(
      context = this,
      audioScanner = audioScanner,
      playlistDao = database.playlistDao(),
      playHistoryDao = database.playHistoryDao(),
      cachedMediaDao = database.cachedMediaDao(),
    )
  }
  val playerController: PlayerController by lazy {
    PlayerController(
      context = this,
      repository = musicRepository,
      prefsRepository = prefsRepository,
    )
  }
}
