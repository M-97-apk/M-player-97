package com.example.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * Pure math mapping between Android's physical STREAM_MUSIC hardware volume steps
 * (e.g., 0..15) and app Volume Boost percentage (0%..100% standard, 101%..797% amplified).
 */
object HardwareVolumeMath {
  const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
  const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

  fun hardwareStepToPercent(step: Int, maxStep: Int): Int {
    if (maxStep <= 0) return 100
    val clamped = step.coerceIn(0, maxStep)
    return ((clamped.toFloat() / maxStep.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
  }

  fun percentToHardwareStep(percent: Int, maxStep: Int): Int {
    if (maxStep <= 0) return 0
    if (percent <= 0) return 0
    if (percent >= 100) return maxStep
    val raw = ((percent.toFloat() / 100f) * maxStep.toFloat()).roundToInt()
    // Ensure tiny non-zero standard percentages (1%..3%) still unmute step 1 so fine PCM vernier works smoothly
    return raw.coerceIn(1, maxStep)
  }

  /**
   * Computes exact fractional DSP trim within [0..100]% so that hardware step * PCM trim
   * produces the exact requested percentage without double-attenuating hardware volume.
   * For boosted > 100% (or hardware changes directly), returns 1.0f.
   */
  fun computePcmTrimForStandardPercent(
    percent: Int,
    hardwareStep: Int,
    maxStep: Int,
  ): Float {
    if (percent <= 0) return 0f
    if (percent >= 100 || maxStep <= 0 || hardwareStep <= 0) return 1f
    val desiredRatio = percent.toFloat() / 100f
    val hardwareRatio = (hardwareStep.toFloat() / maxStep.toFloat()).coerceAtLeast(1e-3f)
    return (desiredRatio / hardwareRatio).coerceIn(0.05f, 1.25f)
  }
}

/**
 * Listens for live physical phone volume button changes on AudioManager.STREAM_MUSIC
 * via system volume broadcast + Settings.System ContentObserver fallback.
 */
class HardwareVolumeMonitor(
  private val context: Context,
  private val onHardwareVolumeChanged: (currentStep: Int, maxStep: Int) -> Unit,
) {
  private val audioManager: AudioManager? =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val handler = Handler(Looper.getMainLooper())
  private var lastObservedStep: Int = -1
  private var receiver: BroadcastReceiver? = null
  private var settingsObserver: ContentObserver? = null

  fun queryCurrentStepAndMax(): Pair<Int, Int> {
    val am = audioManager ?: return 15 to 15
    val max = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
    val cur = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(max)
    return cur to max
  }

  fun setHardwareMusicStep(targetStep: Int) {
    val am = audioManager ?: return
    val max = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
    if (max <= 0) return
    val clamped = targetStep.coerceIn(0, max)
    lastObservedStep = clamped
    runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0) }
  }

  fun start() {
    if (receiver != null) return
    val (initialStep, _) = queryCurrentStepAndMax()
    lastObservedStep = initialStep

    val br =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          if (intent?.action != HardwareVolumeMath.ACTION_VOLUME_CHANGED) return
          val streamType = intent.getIntExtra(HardwareVolumeMath.EXTRA_VOLUME_STREAM_TYPE, -1)
          if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
            dispatchIfChanged()
          }
        }
      }
    val filter = IntentFilter(HardwareVolumeMath.ACTION_VOLUME_CHANGED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(br, filter, Context.RECEIVER_EXPORTED)
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(br, filter)
    }
    receiver = br

    val observer =
      object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
          dispatchIfChanged()
        }
      }
    runCatching {
      context.contentResolver.registerContentObserver(
        Settings.System.CONTENT_URI,
        true,
        observer,
      )
      settingsObserver = observer
    }
  }

  private fun dispatchIfChanged() {
    val (cur, max) = queryCurrentStepAndMax()
    if (max <= 0) return
    if (cur != lastObservedStep) {
      lastObservedStep = cur
      onHardwareVolumeChanged(cur, max)
    }
  }

  fun stop() {
    receiver?.let { runCatching { context.unregisterReceiver(it) } }
    receiver = null
    settingsObserver?.let { runCatching { context.contentResolver.unregisterContentObserver(it) } }
    settingsObserver = null
  }
}
