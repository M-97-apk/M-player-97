package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prefs.UserPreferences
import com.example.localization.LocalAppStrings
import com.example.model.AudioTrack
import com.example.overlay.FloatingMiniPlayerPanel
import com.example.overlay.FloatingOverlayService
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.ThemedPrimaryButton
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.MPlayerTheme
import kotlinx.coroutines.launch

/**
 * Handles audio and video files opened from external file managers.
 * Prompts the user to choose between:
 *  - Play normally (Audio + Video) in the full player
 *  - Play as audio only in the floating mini-player widget without modifying the source file
 */
class ExternalAudioTrampolineActivity : ComponentActivity() {

  private var incomingUriState by mutableStateOf<Uri?>(null)
  private var incomingMimeState by mutableStateOf<String?>(null)
  private var pendingAudioOnlyOverlayLaunch by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIncomingIntent(intent)
    renderPromptContent()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    // Launch full floating service automatically once user returns after granting overlay permission
    // for a chosen "Play as audio only" session.
    if (pendingAudioOnlyOverlayLaunch && Settings.canDrawOverlays(this)) {
      pendingAudioOnlyOverlayLaunch = false
      FloatingOverlayService.start(this)
      finish()
    }
  }

  private fun handleIncomingIntent(incomingIntent: Intent?) {
    val mediaUri: Uri? = incomingIntent?.data
    if (mediaUri == null) {
      finish()
      return
    }
    runCatching {
      contentResolver.takePersistableUriPermission(
        mediaUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
    }
    val mimeType = incomingIntent.type ?: contentResolver.getType(mediaUri)
    val path = mediaUri.path?.lowercase() ?: ""
    val isVideo =
      mimeType?.startsWith("video/", ignoreCase = true) == true ||
        path.endsWith(".mp4") ||
        path.endsWith(".mkv") ||
        path.endsWith(".avi") ||
        path.endsWith(".mov") ||
        path.endsWith(".webm")

    if (!isVideo) {
      // Audio file: skip dialog entirely and play immediately
      val app = application as MPlayerApplication
      app.playerController.playExternalUri(mediaUri, audioOnly = true)
      if (Settings.canDrawOverlays(this)) {
        FloatingOverlayService.start(this)
      } else {
        startActivity(
          Intent(this, MainActivity::class.java).apply {
            flags =
              Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PLAYER_TAB, true)
          }
        )
      }
      finish()
      return
    }

    pendingAudioOnlyOverlayLaunch = false
    incomingMimeState = mimeType
    incomingUriState = mediaUri
  }

  private fun renderPromptContent() {
    val app = application as MPlayerApplication

    setContent {
      val prefs by
        app.prefsRepository.preferencesFlow.collectAsStateWithLifecycle(
          initialValue = UserPreferences()
        )
      val playback by app.playerController.state.collectAsStateWithLifecycle()
      val activeUri = incomingUriState
      val mimeType = incomingMimeState
      var resolvedTrack by remember(activeUri) { mutableStateOf<AudioTrack?>(null) }

      LaunchedEffect(activeUri) {
        if (activeUri != null) {
          resolvedTrack = runCatching { app.musicRepository.resolveExternalTrack(activeUri) }.getOrNull()
        }
      }

      MPlayerTheme(
        themeStyle = prefs.themeStyle,
        darkTheme = prefs.isDarkMode,
        language = prefs.language,
        glassOpacity = prefs.appGlassOpacity,
      ) {
        val strings = LocalAppStrings.current
        val palette = LocalMPlayerPalette.current
        val scope = rememberCoroutineScope()

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.45f))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finish() },
              )
              .padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier =
              Modifier.widthIn(max = 420.dp)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  onClick = {},
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            if (!pendingAudioOnlyOverlayLaunch && activeUri != null) {
              val isVideoItem =
                resolvedTrack?.isVideo ?: (mimeType?.startsWith("video/", ignoreCase = true) == true)
              val displayTitle =
                resolvedTrack?.title?.takeIf { it.isNotBlank() }
                  ?: activeUri.lastPathSegment?.substringAfterLast('/')
                  ?: strings.appName
              val cardShape = RoundedCornerShape(24.dp)

              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(cardShape)
                    .background(palette.sheetBg)
                    .background(palette.glassCardFrostBrush)
                    .border(palette.borderStrokeWidth, palette.borderBrush, cardShape)
                    .padding(18.dp)
                    .testTag("external_media_prompt_dialog")
              ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(
                        imageVector = if (isVideoItem) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                        contentDescription = null,
                        tint = palette.accentGlow,
                        modifier = Modifier.size(24.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = strings.externalOpenTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                      )
                    }
                    ThemedIconButton(onClick = { finish() }, buttonSize = 36.dp) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.cancel,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.glassBadgeFillBrush)
                        .border(1.dp, palette.borderBrush, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                          .background(palette.glassBadgeActiveFillBrush)
                          .border(0.8.dp, palette.borderBrush, RoundedCornerShape(6.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = if (isVideoItem) strings.videoBadgeLabel else strings.audioBadgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentGlow,
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      val duration = resolvedTrack?.durationMs ?: 0L
                      if (duration > 0L) {
                        Text(
                          text = formatDuration(duration),
                          style = MaterialTheme.typography.labelSmall,
                          color = palette.textSecondary,
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = strings.externalOpenSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  ThemedPrimaryButton(
                    onClick = {
                      app.playerController.playExternalUri(activeUri, audioOnly = false)
                      startActivity(
                        Intent(this@ExternalAudioTrampolineActivity, MainActivity::class.java).apply {
                          flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                              Intent.FLAG_ACTIVITY_SINGLE_TOP or
                              Intent.FLAG_ACTIVITY_CLEAR_TOP
                          putExtra(MainActivity.EXTRA_OPEN_PLAYER_TAB, true)
                        }
                      )
                      finish()
                    },
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag("external_play_normal_btn"),
                  ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = strings.externalPlayNormal,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  OutlinedButton(
                    onClick = {
                      app.playerController.playExternalUri(activeUri, audioOnly = true)
                      if (Settings.canDrawOverlays(this@ExternalAudioTrampolineActivity)) {
                        FloatingOverlayService.start(this@ExternalAudioTrampolineActivity)
                        finish()
                      } else {
                        pendingAudioOnlyOverlayLaunch = true
                        runCatching {
                          startActivity(
                            Intent(
                              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                              Uri.parse("package:$packageName"),
                            )
                          )
                        }
                      }
                    },
                    border = BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(48.dp)
                        .testTag("external_play_audio_only_btn"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Headphones,
                      contentDescription = null,
                      tint = palette.accentGlow,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = strings.externalPlayAudioOnly,
                      color = palette.textPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            } else {
              // In-place floating mini-player fallback when overlay permission isn't granted yet
              val panAngle by app.playerController.panAngleDegrees.collectAsStateWithLifecycle()
              FloatingMiniPlayerPanel(
                playback = playback,
                panAngleDegrees = panAngle,
                appThemeStyle = prefs.themeStyle,
                widgetColorMode = prefs.floatingWidgetColorMode,
                widgetGlassMode = prefs.floatingWidgetGlassMode,
                widgetCustomOpacity = prefs.floatingWidgetCustomOpacity,
                onSelectWidgetColorMode = { mode ->
                  scope.launch { app.prefsRepository.setFloatingWidgetColorMode(mode) }
                },
                onSelectWidgetGlassMode = { mode ->
                  scope.launch { app.prefsRepository.setFloatingWidgetGlassMode(mode) }
                },
                onChangeWidgetCustomOpacity = { opacity ->
                  scope.launch { app.prefsRepository.setFloatingWidgetCustomOpacity(opacity) }
                },
                onDrag = null,
                onPlayPause = { app.playerController.togglePlayPause() },
                onRewind10s = { app.playerController.seekBy(-10_000L) },
                onForward10s = { app.playerController.seekBy(10_000L) },
                onPrevTrack = { app.playerController.skipPrevious() },
                onNextTrack = { app.playerController.skipNext() },
                onSeek = { app.playerController.seekTo(it) },
                onToggle8D = { app.playerController.toggle8DAudio() },
                onSelect8DSpeed = { app.playerController.set8DSpeed(it) },
                onChange8DIntensity = { app.playerController.set8DIntensity(it) },
                onExpandApp = {
                  startActivity(
                    Intent(this@ExternalAudioTrampolineActivity, MainActivity::class.java).apply {
                      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                      putExtra(MainActivity.EXTRA_OPEN_PLAYER_TAB, true)
                    }
                  )
                  finish()
                },
                onClose = {
                  app.playerController.stopPlayback()
                  finish()
                },
              )

              Spacer(modifier = Modifier.height(8.dp))

              ThemedCard(cornerRadius = 20.dp, elevated = true) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                  Text(
                    text = strings.overlaySetupPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                  ) {
                    TextButton(onClick = { finish() }) {
                      Text(strings.dismiss, color = palette.textSecondary)
                    }
                    ThemedPrimaryButton(
                      onClick = {
                        val settingsIntent =
                          Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                          )
                        startActivity(settingsIntent)
                      },
                    ) {
                      Text(strings.openSystemOverlaySettings)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
