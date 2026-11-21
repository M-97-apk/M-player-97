package com.example.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.Audio8DSpeed
import com.example.model.AudioOutputRoute
import com.example.model.AudioOutputRouteType
import com.example.model.EqualizerPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared real-time bus connecting UI controls, Floating Overlay, Settings,
 * MediaSession notification, and the ExoPlayer DSP AudioProcessor.
 */
object Audio8DEffectBus {
  private val _isEnabledFlow = MutableStateFlow(false)
  val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

  @Volatile
  var isEnabled: Boolean = false
    private set

  private val _speedFlow = MutableStateFlow(Audio8DSpeed.NORMAL)
  val speedFlow: StateFlow<Audio8DSpeed> = _speedFlow.asStateFlow()

  @Volatile
  var speed: Audio8DSpeed = Audio8DSpeed.NORMAL
    private set

  private val _intensityPercentFlow = MutableStateFlow(97)
  val intensityPercentFlow: StateFlow<Int> = _intensityPercentFlow.asStateFlow()

  @Volatile
  var intensityPercent: Int = 97
    private set

  private val _panAngleDegrees = MutableStateFlow(0f)
  val panAngleDegrees: StateFlow<Float> = _panAngleDegrees.asStateFlow()

  fun setEnabled(enabled: Boolean) {
    isEnabled = enabled
    _isEnabledFlow.value = enabled
  }

  fun setSpeed(newSpeed: Audio8DSpeed) {
    speed = newSpeed
    _speedFlow.value = newSpeed
  }

  fun setIntensityPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    intensityPercent = clamped
    _intensityPercentFlow.value = clamped
  }

  internal fun publishAngle(degrees: Float) {
    _panAngleDegrees.value = degrees
  }
}

/**
 * Ultra Boost Volume Engine bus (0% to 797% synced with STREAM_MUSIC hardware volume).
 */
object VolumeBoostBus {
  private val _boostPercentFlow = MutableStateFlow(100)
  val boostPercentFlow: StateFlow<Int> = _boostPercentFlow.asStateFlow()

  @Volatile
  var boostPercent: Int = 100
    private set

  @Volatile
  var hardwareFineTrim: Float = 1f
    private set

  fun setBoostPercent(percent: Int, fineTrim: Float = 1f) {
    val clamped = percent.coerceIn(0, 797)
    hardwareFineTrim = fineTrim.coerceIn(0f, 1.25f)
    boostPercent = clamped
    _boostPercentFlow.value = clamped
  }
}

/**
 * Separate dedicated real-time Amplifier preamp + loudness enhancer bus (0%..100% -> +0dB..+97dB).
 */
object AudioAmplifierBus {
  const val MAX_PREAMP_DB = 97f

  private val _isEnabledFlow = MutableStateFlow(false)
  val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

  @Volatile
  var isEnabled: Boolean = false
    private set

  private val _levelPercentFlow = MutableStateFlow(50)
  val levelPercentFlow: StateFlow<Int> = _levelPercentFlow.asStateFlow()

  @Volatile
  var levelPercent: Int = 50
    private set

  fun setEnabled(enabled: Boolean) {
    isEnabled = enabled
    _isEnabledFlow.value = enabled
  }

  fun setLevelPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    levelPercent = clamped
    _levelPercentFlow.value = clamped
  }

  fun computeGainDb(enabled: Boolean = isEnabled, level: Int = levelPercent): Float {
    if (!enabled || level <= 0) return 0f
    return (level.coerceIn(0, 100) / 100f) * MAX_PREAMP_DB
  }

  fun computeGainLinear(enabled: Boolean = isEnabled, level: Int = levelPercent): Float {
    val db = computeGainDb(enabled, level)
    if (db <= 0.001f) return 1f
    return 10.0.pow(db.toDouble() / 20.0).toFloat()
  }

  fun computeCombinedDspGain(
    boostPercent: Int,
    hardwareFineTrim: Float,
    amplifierEnabled: Boolean,
    amplifierLevelPercent: Int,
  ): Float {
    if (boostPercent <= 0) return 0f
    val boostLinear =
      if (boostPercent <= 100) {
        hardwareFineTrim.coerceIn(0f, 1.25f).toDouble()
      } else {
        (boostPercent / 100.0).coerceIn(1.01, 7.97)
      }
    val ampDb = computeGainDb(amplifierEnabled, amplifierLevelPercent).toDouble()
    val ampLinear = if (ampDb > 0.001) 10.0.pow(ampDb / 20.0) else 1.0
    return (boostLinear * ampLinear).coerceIn(0.0, 600_000.0).toFloat()
  }
}

/**
 * Shared live hardware audio output route bus (Speaker vs Wired Headphones vs Bluetooth).
 */
object AudioOutputRouteBus {
  private val _routeFlow = MutableStateFlow(AudioOutputRoute())
  val routeFlow: StateFlow<AudioOutputRoute> = _routeFlow.asStateFlow()

  @Volatile
  var currentRoute: AudioOutputRoute = AudioOutputRoute()
    private set

  fun updateRoute(route: AudioOutputRoute) {
    if (currentRoute != route) {
      currentRoute = route
      _routeFlow.value = route
    }
  }
}

/**
 * Real-time Graphic Equalizer DSP state bus (5 stereo biquad bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz).
 */
object AudioEqualizerBus {
  const val BAND_COUNT = 5
  const val MIN_DB = -12f
  const val MAX_DB = 12f
  val BAND_CENTER_FREQS_HZ = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)

  private val _isEnabledFlow = MutableStateFlow(true)
  val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

  @Volatile
  var isEnabled: Boolean = true
    private set

  private val _presetFlow = MutableStateFlow<EqualizerPreset?>(EqualizerPreset.FLAT)
  val presetFlow: StateFlow<EqualizerPreset?> = _presetFlow.asStateFlow()

  private val _bandGainsDbFlow = MutableStateFlow(EqualizerPreset.FLAT.gainsDb)
  val bandGainsDbFlow: StateFlow<List<Float>> = _bandGainsDbFlow.asStateFlow()

  @Volatile
  var bandGainsDb: FloatArray = FloatArray(BAND_COUNT) { 0f }
    private set

  @Volatile
  var revision: Int = 0
    private set

  fun setEnabled(enabled: Boolean) {
    isEnabled = enabled
    _isEnabledFlow.value = enabled
    revision++
  }

  fun applyPreset(preset: EqualizerPreset) {
    val copy = FloatArray(BAND_COUNT) { idx -> preset.gainsDb.getOrElse(idx) { 0f }.coerceIn(MIN_DB, MAX_DB) }
    bandGainsDb = copy
    _bandGainsDbFlow.value = copy.toList()
    _presetFlow.value = preset
    revision++
  }

  fun setBandGain(bandIndex: Int, gainDb: Float) {
    if (bandIndex !in 0 until BAND_COUNT) return
    val current = _bandGainsDbFlow.value.toMutableList()
    current[bandIndex] = gainDb.coerceIn(MIN_DB, MAX_DB)
    setCustomGains(current)
  }

  fun setCustomGains(gains: List<Float>) {
    val safe = FloatArray(BAND_COUNT) { idx -> gains.getOrElse(idx) { 0f }.coerceIn(MIN_DB, MAX_DB) }
    bandGainsDb = safe
    _bandGainsDbFlow.value = safe.toList()
    val matchedPreset =
      EqualizerPreset.entries.firstOrNull { preset ->
        preset.gainsDb.indices.all { i -> abs(preset.gainsDb[i] - safe[i]) < 0.15f }
      }
    _presetFlow.value = matchedPreset
    revision++
  }
}

/**
 * Analyzed audio telemetry frame stamped with audio stream elapsed time (ms since last flush).
 */
data class VisualizerFrame(
  val audioTimeMs: Double,
  val rms: Float,
  val bass: Float,
  val waveform: FloatArray,
  val spectrum: FloatArray,
)

/**
 * Pure, zero-allocation real-time FFT spectrum & beat-onset analyzer.
 * Uses a 1024-sample Hann-windowed Radix-2 FFT with a 512-sample hop (~86 frames/sec @ 44.1 kHz)
 * so every kick drum, bass drop, vocal swell, and hi-hat is resolved accurately in frequency & time.
 */
internal class RealtimeFftSpectrumAnalyzer(
  sampleRateHz: Int = 44100,
) {
  companion object {
    const val FFT_SIZE = 1024
    const val HOP_SIZE = 512
    const val HALF_FFT = FFT_SIZE / 2
    const val LOG2_N = 10
  }

  private var sampleRate = sampleRateHz.coerceAtLeast(8000)

  // Precomputed bit-reversal indices, Hann window, and Radix-2 twiddle factors (zero per-frame alloc)
  private val bitReversed = IntArray(FFT_SIZE)
  private val hannWindow = FloatArray(FFT_SIZE)
  private val cosTable = FloatArray(HALF_FFT)
  private val sinTable = FloatArray(HALF_FFT)

  // Sliding PCM mono ring buffer & FFT scratch arrays
  private val monoRing = FloatArray(FFT_SIZE)
  private var ringWritePos = 0
  private var samplesSinceHop = 0

  private val fftReal = FloatArray(FFT_SIZE)
  private val fftImag = FloatArray(FFT_SIZE)
  private val magnitudes = FloatArray(HALF_FFT)

  // Precomputed logarithmic bin boundaries for 20 visual spectrum bars (45 Hz -> 15 kHz)
  private val barBinStart = IntArray(AudioVisualizerBus.SPECTRUM_BINS)
  private val barBinEnd = IntArray(AudioVisualizerBus.SPECTRUM_BINS)

  // Adaptive dynamics & beat onset trackers
  private var peakReference = 0.08f
  private var smoothedRms = 0f
  private var bassMovingAvg = 0f
  private var beatImpulse = 0f
  private var smoothedBass = 0f
  private val smoothedSpectrum = FloatArray(AudioVisualizerBus.SPECTRUM_BINS)
  private val smoothedWave = FloatArray(AudioVisualizerBus.WAVEFORM_POINTS)

  init {
    for (i in 0 until FFT_SIZE) {
      var x = i
      var rev = 0
      for (b in 0 until LOG2_N) {
        rev = (rev shl 1) or (x and 1)
        x = x ushr 1
      }
      bitReversed[i] = rev
      hannWindow[i] = (0.5 * (1.0 - cos((2.0 * PI * i) / (FFT_SIZE - 1)))).toFloat()
    }
    for (k in 0 until HALF_FFT) {
      val angle = -2.0 * PI * k / FFT_SIZE
      cosTable[k] = cos(angle).toFloat()
      sinTable[k] = sin(angle).toFloat()
    }
    recomputeFrequencyBands(sampleRate)
  }

  fun setSampleRate(newRateHz: Int) {
    val safe = newRateHz.coerceAtLeast(8000)
    if (safe != sampleRate) {
      sampleRate = safe
      recomputeFrequencyBands(safe)
    }
  }

  private fun recomputeFrequencyBands(sr: Int) {
    val minFreq = 45.0
    val maxFreq = (sr * 0.42).coerceAtMost(15000.0).coerceAtLeast(2400.0)
    val freqRatio = maxFreq / minFreq
    val hzPerBin = sr.toDouble() / FFT_SIZE.toDouble()

    var prevEnd = 0
    for (bar in 0 until AudioVisualizerBus.SPECTRUM_BINS) {
      val f0 = minFreq * freqRatio.pow(bar.toDouble() / AudioVisualizerBus.SPECTRUM_BINS.toDouble())
      val f1 = minFreq * freqRatio.pow((bar + 1).toDouble() / AudioVisualizerBus.SPECTRUM_BINS.toDouble())
      var bStart = (f0 / hzPerBin).toInt().coerceIn(1, HALF_FFT - 1)
      if (bStart <= prevEnd && prevEnd < HALF_FFT - 2) {
        bStart = prevEnd + 1
      }
      val bEnd = (f1 / hzPerBin).toInt().coerceIn(bStart, HALF_FFT - 1)
      barBinStart[bar] = bStart
      barBinEnd[bar] = bEnd
      prevEnd = bStart
    }
  }

  fun reset() {
    monoRing.fill(0f)
    ringWritePos = 0
    samplesSinceHop = 0
    peakReference = 0.08f
    smoothedRms = 0f
    bassMovingAvg = 0f
    beatImpulse = 0f
    smoothedBass = 0f
    smoothedSpectrum.fill(0f)
    smoothedWave.fill(0f)
  }

  /**
   * Feeds a single normalized mono sample (-1f..1f).
   * Returns a newly computed [VisualizerFrame] every [HOP_SIZE] samples (~11.6ms @ 44.1 kHz).
   */
  fun pushSample(normalizedMono: Float, audioTimeMs: Double): VisualizerFrame? {
    monoRing[ringWritePos] = normalizedMono.coerceIn(-1.5f, 1.5f)
    ringWritePos = (ringWritePos + 1) and (FFT_SIZE - 1)
    samplesSinceHop++
    if (samplesSinceHop < HOP_SIZE) return null
    samplesSinceHop = 0
    return analyzeCurrentWindow(audioTimeMs)
  }

  private fun analyzeCurrentWindow(audioTimeMs: Double): VisualizerFrame {
    var sumSq = 0.0
    for (i in 0 until FFT_SIZE) {
      val sample = monoRing[(ringWritePos + i) and (FFT_SIZE - 1)]
      sumSq += (sample * sample).toDouble()
      fftReal[bitReversed[i]] = sample * hannWindow[i]
      fftImag[bitReversed[i]] = 0f
    }

    val rawRms = sqrt(sumSq / FFT_SIZE.toDouble()).toFloat()

    // Silence gate: zero out cleanly when track is silent
    if (rawRms < 0.0008f) {
      smoothedRms *= 0.65f
      smoothedBass *= 0.65f
      beatImpulse *= 0.65f
      for (b in smoothedSpectrum.indices) smoothedSpectrum[b] *= 0.65f
      for (w in smoothedWave.indices) smoothedWave[w] *= 0.65f
      return VisualizerFrame(
        audioTimeMs = audioTimeMs,
        rms = if (smoothedRms < 0.004f) 0f else smoothedRms,
        bass = if (smoothedBass < 0.004f) 0f else smoothedBass,
        waveform = FloatArray(AudioVisualizerBus.WAVEFORM_POINTS) { if (abs(smoothedWave[it]) < 0.004f) 0f else smoothedWave[it] },
        spectrum = FloatArray(AudioVisualizerBus.SPECTRUM_BINS) { if (smoothedSpectrum[it] < 0.004f) 0f else smoothedSpectrum[it] },
      )
    }

    // In-place Cooley-Tukey Radix-2 FFT
    var halfLen = 1
    while (halfLen < FFT_SIZE) {
      val step = halfLen shl 1
      val twiddleStep = FFT_SIZE / step
      var start = 0
      while (start < FFT_SIZE) {
        var twIdx = 0
        for (k in 0 until halfLen) {
          val iEven = start + k
          val iOdd = iEven + halfLen
          val cosW = cosTable[twIdx]
          val sinW = sinTable[twIdx]
          val oddR = fftReal[iOdd]
          val oddI = fftImag[iOdd]
          val tR = oddR * cosW - oddI * sinW
          val tI = oddR * sinW + oddI * cosW
          val evenR = fftReal[iEven]
          val evenI = fftImag[iEven]
          fftReal[iEven] = evenR + tR
          fftImag[iEven] = evenI + tI
          fftReal[iOdd] = evenR - tR
          fftImag[iOdd] = evenI - tI
          twIdx += twiddleStep
        }
        start += step
      }
      halfLen = step
    }

    // Compute FFT bin magnitudes normalized by window coherent gain (~0.25 * FFT_SIZE)
    val magScale = 2.0f / (FFT_SIZE * 0.5f)
    for (bin in 1 until HALF_FFT) {
      val re = fftReal[bin]
      val im = fftImag[bin]
      magnitudes[bin] = sqrt(re * re + im * im) * magScale
    }

    // Adaptive AGC peak tracker (fast attack, gentle decay so both quiet verses & heavy drops shine)
    if (rawRms > peakReference) {
      peakReference = peakReference * 0.35f + rawRms * 0.65f
    } else {
      peakReference = (peakReference * 0.994f).coerceAtLeast(0.035f)
    }

    val normRms = (rawRms / (peakReference * 1.15f)).coerceIn(0f, 1f)
    smoothedRms =
      if (normRms >= smoothedRms) smoothedRms * 0.25f + normRms * 0.75f
      else smoothedRms * 0.78f + normRms * 0.22f

    // Bass energy (~43 Hz to ~180 Hz) & beat onset transient detector
    val bassEndBin = (185.0 / (sampleRate.toDouble() / FFT_SIZE.toDouble())).toInt().coerceIn(2, 10)
    var bassSum = 0f
    var bassPeak = 0f
    for (b in 1..bassEndBin) {
      val m = magnitudes[b]
      bassSum += m
      if (m > bassPeak) bassPeak = m
    }
    val rawBass = ((bassSum / bassEndBin.toFloat()) * 0.55f + bassPeak * 0.45f)
    val normBassLevel = (rawBass / (peakReference * 1.45f)).coerceIn(0f, 1f)

    bassMovingAvg = bassMovingAvg * 0.91f + rawBass * 0.09f
    val onsetDelta = (rawBass - bassMovingAvg * 1.18f).coerceAtLeast(0f)
    val onsetNorm = (onsetDelta / (bassMovingAvg + peakReference * 0.35f + 0.02f)).coerceIn(0f, 1f)
    beatImpulse =
      if (onsetNorm > beatImpulse) onsetNorm
      else (beatImpulse * 0.80f).coerceAtLeast(onsetNorm)

    val targetBass = (normBassLevel * 0.46f + beatImpulse * 0.62f).coerceIn(0f, 1f)
    smoothedBass =
      if (targetBass >= smoothedBass) smoothedBass * 0.18f + targetBass * 0.82f
      else smoothedBass * 0.76f + targetBass * 0.24f

    // 20 logarithmic FFT spectrum bars with perceptual high-frequency tilt + EQ-faithful shape
    val eqEnabled = AudioEqualizerBus.isEnabled
    val outBins = FloatArray(AudioVisualizerBus.SPECTRUM_BINS)
    val agcDiv = (peakReference * 1.25f).coerceAtLeast(0.03f)
    for (bar in 0 until AudioVisualizerBus.SPECTRUM_BINS) {
      val b0 = barBinStart[bar]
      val b1 = barBinEnd[bar]
      var binPeak = 0f
      var binMean = 0f
      val count = (b1 - b0 + 1).coerceAtLeast(1)
      for (b in b0..b1) {
        val m = magnitudes[b]
        binMean += m
        if (m > binPeak) binPeak = m
      }
      binMean /= count.toFloat()
      val combinedMag = binPeak * 0.62f + binMean * 0.38f

      // Natural pink-spectrum compensation: hi-hats carry less raw RMS than sub-bass, boost slightly with bar index
      val perceptualTilt = 1.0f + (bar / 19f) * 0.95f
      val scaled = (combinedMag * perceptualTilt) / agcDiv
      // Log compression maps quiet harmonics smoothly while keeping kick & snare peaks punchy
      var targetBar = (kotlin.math.ln(1f + scaled * 4.2f) / kotlin.math.ln(5.2f)).coerceIn(0f, 1f)
      if (bar <= 3) {
        targetBar = (targetBar * 0.78f + beatImpulse * 0.26f).coerceIn(0f, 1f)
      }
      if (eqEnabled) {
        val eqBandIdx = ((bar / 19f) * (AudioEqualizerBus.BAND_COUNT - 1)).toInt()
          .coerceIn(0, AudioEqualizerBus.BAND_COUNT - 1)
        val eqGainFactor = (1f + AudioEqualizerBus.bandGainsDb[eqBandIdx] / 36f).coerceIn(0.70f, 1.30f)
        targetBar = (targetBar * eqGainFactor).coerceIn(0f, 1f)
      }

      val prev = smoothedSpectrum[bar]
      val next = if (targetBar >= prev) prev * 0.22f + targetBar * 0.78f else prev * 0.72f + targetBar * 0.28f
      smoothedSpectrum[bar] = next
      outBins[bar] = if (next < 0.008f) 0f else next
    }

    // Smooth anti-aliased 32-point zero-crossing-aligned waveform from the active 1024 window
    val searchLimit = FFT_SIZE / 2
    var zeroCrossingIdx = 0
    for (i in 1 until searchLimit) {
      val s0 = monoRing[(ringWritePos + i - 1) and (FFT_SIZE - 1)]
      val s1 = monoRing[(ringWritePos + i) and (FFT_SIZE - 1)]
      if (s0 <= 0f && s1 > 0f) {
        zeroCrossingIdx = i
        break
      }
    }
    val waveWindowSamples = 384 // ~8.7ms low-aliased oscilloscope slice starting at rising zero-crossing
    val blockLen = (waveWindowSamples / AudioVisualizerBus.WAVEFORM_POINTS).coerceAtLeast(1)
    val waveGain = (1.0f / (peakReference * 2.1f)).coerceIn(0.6f, 6.5f)
    val outWave = FloatArray(AudioVisualizerBus.WAVEFORM_POINTS)
    for (p in 0 until AudioVisualizerBus.WAVEFORM_POINTS) {
      var avg = 0f
      val base = zeroCrossingIdx + p * blockLen
      for (k in 0 until blockLen) {
        avg += monoRing[(ringWritePos + base + k) and (FFT_SIZE - 1)]
      }
      avg /= blockLen.toFloat()
      val targetSample = (avg * waveGain).coerceIn(-1f, 1f)
      val nextSample = smoothedWave[p] * 0.30f + targetSample * 0.70f
      smoothedWave[p] = nextSample
      outWave[p] = if (abs(nextSample) < 0.004f) 0f else nextSample
    }

    return VisualizerFrame(
      audioTimeMs = audioTimeMs,
      rms = smoothedRms,
      bass = smoothedBass,
      waveform = outWave,
      spectrum = outBins,
    )
  }
}

/**
 * Time-synchronized PCM audio telemetry bus (zero permissions required) powering:
 * - Spectrum Bars (20 real FFT frequency bins)
 * - Pulsing Circular Visualizer (true kick/bass onset + 32 radial FFT spokes)
 * - Smooth Glowing Neon Waveforms (zero-crossing aligned anti-aliased PCM wave)
 * - True 3D Volumetric Particle Sphere (synced to bass pulse & frequency bands)
 *
 * Uses a timestamped lookahead ring buffer so AudioProcessor decode bursts ahead of ExoPlayer's
 * AudioTrack output are played back smoothly at ~60 FPS locked to speaker output timing.
 */
object AudioVisualizerBus {
  const val WAVEFORM_POINTS = 32
  const val SPECTRUM_BINS = 20
  private const val RING_CAPACITY = 512
  private const val AUDIO_PIPELINE_LEAD_MS = 135.0

  private val _amplitude = MutableStateFlow(0f)
  val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

  private val _bassEnergy = MutableStateFlow(0f)
  val bassEnergy: StateFlow<Float> = _bassEnergy.asStateFlow()

  private val _waveformSamples = MutableStateFlow(FloatArray(WAVEFORM_POINTS))
  val waveformSamples: StateFlow<FloatArray> = _waveformSamples.asStateFlow()

  private val _spectrumBins = MutableStateFlow(FloatArray(SPECTRUM_BINS))
  val spectrumBins: StateFlow<FloatArray> = _spectrumBins.asStateFlow()

  private val ringLock = Any()
  private val frameRing = arrayOfNulls<VisualizerFrame>(RING_CAPACITY)
  private var ringCount = 0
  private var ringTail = 0 // index of newest written frame + 1
  private var queuedAudioMs = 0.0
  private var playheadAudioMs = 0.0
  private var audioPlayoutStarted = false

  internal fun enqueueFrame(frame: VisualizerFrame, totalQueuedMs: Double) {
    synchronized(ringLock) {
      queuedAudioMs = totalQueuedMs
      frameRing[ringTail] = frame
      ringTail = (ringTail + 1) % RING_CAPACITY
      if (ringCount < RING_CAPACITY) ringCount++
    }
  }

  internal fun onAudioPlayoutStarted() {
    synchronized(ringLock) {
      audioPlayoutStarted = true
    }
  }

  internal fun advancePlayheadAndPublish(deltaWallMs: Double, playbackSpeed: Float) {
    val selected: VisualizerFrame?
    synchronized(ringLock) {
      if (ringCount == 0) {
        selected = null
      } else {
        val started = audioPlayoutStarted || queuedAudioMs > 240.0
        if (started) {
          playheadAudioMs += (deltaWallMs * playbackSpeed.toDouble().coerceIn(0.25, 2.5))
          val idealMax = (queuedAudioMs - AUDIO_PIPELINE_LEAD_MS).coerceAtLeast(0.0)
          if (queuedAudioMs - playheadAudioMs > 460.0) {
            playheadAudioMs = idealMax
          } else if (playheadAudioMs > queuedAudioMs) {
            playheadAudioMs = queuedAudioMs
          }
        }
        selected = findNearestFrameLocked(playheadAudioMs)
      }
    }

    if (selected != null) {
      _amplitude.value = selected.rms
      _bassEnergy.value = selected.bass
      _waveformSamples.value = selected.waveform
      _spectrumBins.value = selected.spectrum
    }
  }

  internal fun decayToRest(): Boolean {
    val curAmp = _amplitude.value
    val curBass = _bassEnergy.value
    val curBins = _spectrumBins.value
    val curWave = _waveformSamples.value

    var anyNonZero = curAmp > 0.008f || curBass > 0.008f
    val nextAmp = if (curAmp > 0.008f) curAmp * 0.72f else 0f
    val nextBass = if (curBass > 0.008f) curBass * 0.72f else 0f
    val nextBins =
      FloatArray(SPECTRUM_BINS) { i ->
        val v = curBins.getOrElse(i) { 0f } * 0.72f
        if (v > 0.008f) {
          anyNonZero = true
          v
        } else 0f
      }
    val nextWave =
      FloatArray(WAVEFORM_POINTS) { i ->
        val v = curWave.getOrElse(i) { 0f } * 0.70f
        if (abs(v) > 0.006f) {
          anyNonZero = true
          v
        } else 0f
      }

    _amplitude.value = nextAmp
    _bassEnergy.value = nextBass
    _spectrumBins.value = nextBins
    _waveformSamples.value = nextWave
    return anyNonZero
  }

  private fun findNearestFrameLocked(targetTimeMs: Double): VisualizerFrame? {
    if (ringCount == 0) return null
    val startIdx = (ringTail - ringCount + RING_CAPACITY) % RING_CAPACITY
    var best = frameRing[startIdx]
    for (offset in 0 until ringCount) {
      val idx = (startIdx + offset) % RING_CAPACITY
      val candidate = frameRing[idx] ?: continue
      if (candidate.audioTimeMs <= targetTimeMs) {
        best = candidate
      } else {
        break
      }
    }
    return best
  }

  internal fun reset() {
    synchronized(ringLock) {
      frameRing.fill(null)
      ringCount = 0
      ringTail = 0
      queuedAudioMs = 0.0
      playheadAudioMs = 0.0
      audioPlayoutStarted = false
    }
    _amplitude.value = 0f
    _bassEnergy.value = 0f
    _waveformSamples.value = FloatArray(WAVEFORM_POINTS)
    _spectrumBins.value = FloatArray(SPECTRUM_BINS)
  }
}

/**
 * Transposed Direct Form II stereo Biquad filter for real-time zero-allocation graphic EQ.
 */
private class StereoBiquadBand {
  var b0 = 1.0
  var b1 = 0.0
  var b2 = 0.0
  var a1 = 0.0
  var a2 = 0.0

  private var z1L = 0.0
  private var z2L = 0.0
  private var z1R = 0.0
  private var z2R = 0.0

  var active = false
    private set

  fun resetState() {
    z1L = 0.0
    z2L = 0.0
    z1R = 0.0
    z2R = 0.0
  }

  fun configure(sampleRate: Int, centerFreqHz: Double, gainDb: Float, isLowShelf: Boolean, isHighShelf: Boolean) {
    if (abs(gainDb) < 0.05f) {
      active = false
      b0 = 1.0
      b1 = 0.0
      b2 = 0.0
      a1 = 0.0
      a2 = 0.0
      return
    }
    active = true
    val sr = sampleRate.toDouble().coerceAtLeast(8000.0)
    val freq = centerFreqHz.coerceIn(20.0, sr * 0.45)
    val aGain = 10.0.pow(gainDb.toDouble() / 40.0)
    val w0 = 2.0 * PI * freq / sr
    val cosW0 = cos(w0)
    val sinW0 = sin(w0)

    val rawB0: Double
    val rawB1: Double
    val rawB2: Double
    val rawA0: Double
    val rawA1: Double
    val rawA2: Double

    when {
      isLowShelf -> {
        val alpha = sinW0 / 2.0 * sqrt(2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(aGain) * alpha
        rawB0 = aGain * ((aGain + 1.0) - (aGain - 1.0) * cosW0 + twoSqrtAAlpha)
        rawB1 = 2.0 * aGain * ((aGain - 1.0) - (aGain + 1.0) * cosW0)
        rawB2 = aGain * ((aGain + 1.0) - (aGain - 1.0) * cosW0 - twoSqrtAAlpha)
        rawA0 = (aGain + 1.0) + (aGain - 1.0) * cosW0 + twoSqrtAAlpha
        rawA1 = -2.0 * ((aGain - 1.0) + (aGain + 1.0) * cosW0)
        rawA2 = (aGain + 1.0) + (aGain - 1.0) * cosW0 - twoSqrtAAlpha
      }
      isHighShelf -> {
        val alpha = sinW0 / 2.0 * sqrt(2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(aGain) * alpha
        rawB0 = aGain * ((aGain + 1.0) + (aGain - 1.0) * cosW0 + twoSqrtAAlpha)
        rawB1 = -2.0 * aGain * ((aGain - 1.0) + (aGain + 1.0) * cosW0)
        rawB2 = aGain * ((aGain + 1.0) + (aGain - 1.0) * cosW0 - twoSqrtAAlpha)
        rawA0 = (aGain + 1.0) - (aGain - 1.0) * cosW0 + twoSqrtAAlpha
        rawA1 = 2.0 * ((aGain - 1.0) - (aGain + 1.0) * cosW0)
        rawA2 = (aGain + 1.0) - (aGain - 1.0) * cosW0 - twoSqrtAAlpha
      }
      else -> {
        val q = 1.15
        val alpha = sinW0 / (2.0 * q)
        rawB0 = 1.0 + alpha * aGain
        rawB1 = -2.0 * cosW0
        rawB2 = 1.0 - alpha * aGain
        rawA0 = 1.0 + alpha / aGain
        rawA1 = -2.0 * cosW0
        rawA2 = 1.0 - alpha / aGain
      }
    }

    b0 = rawB0 / rawA0
    b1 = rawB1 / rawA0
    b2 = rawB2 / rawA0
    a1 = rawA1 / rawA0
    a2 = rawA2 / rawA0
  }

  fun processLeft(x: Float): Float {
    if (!active) return x
    val xd = x.toDouble()
    val y = b0 * xd + z1L
    z1L = b1 * xd - a1 * y + z2L
    z2L = b2 * xd - a2 * y
    return y.toFloat()
  }

  fun processRight(x: Float): Float {
    if (!active) return x
    val xd = x.toDouble()
    val y = b0 * xd + z1R
    z1R = b1 * xd - a1 * y + z2R
    z2R = b2 * xd - a2 * y
    return y.toFloat()
  }
}

/**
 * Unified Media3 PCM AudioProcessor handling:
 * 1) Real-time 5-band Graphic Equalizer (Bass, Midrange, Treble Biquad IIR bank).
 * 2) Real-time 8D Spatial Audio (LFO 360° L/R/F/B panning, ITD delay, and reverb).
 * 3) Zero-allocation real-time 1024-point FFT Spectrum, Beat Impulse, & Waveform analysis
 *    pushed into [AudioVisualizerBus]'s timestamped lookahead ring buffer.
 */
@OptIn(UnstableApi::class)
class Audio8DEffectProcessor : BaseAudioProcessor() {

  private var phaseRad = 0.0
  private var sampleRateHz = 44100
  private var inputChannels = 2
  private var inputEncoding = C.ENCODING_PCM_16BIT

  private val bufferSize = 8192
  private val bufferMask = bufferSize - 1
  private var leftDelayBuf = FloatArray(bufferSize)
  private var rightDelayBuf = FloatArray(bufferSize)
  private var reverbBufL = FloatArray(bufferSize)
  private var reverbBufR = FloatArray(bufferSize)
  private var writeIndex = 0
  private var processedFramesSinceFlush = 0L

  // Binaural acoustic head-shadow & damped hall reverb IIR states
  private var headShadowLpL = 0f
  private var headShadowLpR = 0f
  private var reverbDampL = 0f
  private var reverbDampR = 0f

  // 5 stereo EQ filters + cached headroom attenuation
  private val eqBands = Array(AudioEqualizerBus.BAND_COUNT) { StereoBiquadBand() }
  private var lastEqRevision = -1
  private var eqHeadroomScale = 1f

  // Clean DRC + Soft-Knee Limiter states for Ultra Boost Volume Engine (0%..797%) & Amplifier (+0..+97 dB)
  private var compEnvelope = 0f
  private var compGainSmoothed = 1f

  private val fftAnalyzer = RealtimeFftSpectrumAnalyzer(44100)

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    val supportedEncoding =
      inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
        inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT ||
        inputAudioFormat.encoding == C.ENCODING_PCM_24BIT ||
        inputAudioFormat.encoding == C.ENCODING_PCM_32BIT
    if (!supportedEncoding || inputAudioFormat.channelCount < 1) {
      return AudioProcessor.AudioFormat.NOT_SET
    }
    sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8000)
    inputChannels = inputAudioFormat.channelCount
    inputEncoding = inputAudioFormat.encoding
    fftAnalyzer.setSampleRate(sampleRateHz)
    lastEqRevision = -1
    updateEqCoefficientsIfNeeded()
    return AudioProcessor.AudioFormat(sampleRateHz, 2, C.ENCODING_PCM_16BIT)
  }

  override fun onFlush() {
    leftDelayBuf.fill(0f)
    rightDelayBuf.fill(0f)
    reverbBufL.fill(0f)
    reverbBufR.fill(0f)
    headShadowLpL = 0f
    headShadowLpR = 0f
    reverbDampL = 0f
    reverbDampR = 0f
    compEnvelope = 0f
    compGainSmoothed = 1f
    eqBands.forEach { it.resetState() }
    writeIndex = 0
    processedFramesSinceFlush = 0L
    fftAnalyzer.reset()
    AudioVisualizerBus.reset()
  }

  override fun onReset() {
    phaseRad = 0.0
    lastEqRevision = -1
    onFlush()
  }

  private fun updateEqCoefficientsIfNeeded() {
    val currentRev = AudioEqualizerBus.revision
    if (currentRev == lastEqRevision) return
    lastEqRevision = currentRev

    val eqEnabled = AudioEqualizerBus.isEnabled
    val gains = AudioEqualizerBus.bandGainsDb
    var maxPositiveDb = 0f
    for (b in 0 until AudioEqualizerBus.BAND_COUNT) {
      val gain = if (eqEnabled) gains.getOrElse(b) { 0f } else 0f
      if (gain > maxPositiveDb) maxPositiveDb = gain
      eqBands[b].configure(
        sampleRate = sampleRateHz,
        centerFreqHz = AudioEqualizerBus.BAND_CENTER_FREQS_HZ[b].toDouble(),
        gainDb = gain,
        isLowShelf = (b == 0),
        isHighShelf = (b == AudioEqualizerBus.BAND_COUNT - 1),
      )
    }
    // Gentle soft-limiter gain offset to prevent digital clipping on high positive boosts (+9dB Bass Boost)
    eqHeadroomScale =
      if (maxPositiveDb > 1.5f) {
        10.0.pow((-(maxPositiveDb * 0.42f)).toDouble() / 20.0).toFloat().coerceIn(0.45f, 1f)
      } else {
        1f
      }
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    val remainingBytes = inputBuffer.remaining()
    if (remainingBytes == 0) return

    val bytesPerSample =
      when (inputEncoding) {
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> 2
      }
    val frameByteSize = inputChannels * bytesPerSample
    val frameCount = remainingBytes / frameByteSize
    if (frameCount <= 0) return

    updateEqCoefficientsIfNeeded()

    val outputBuffer = replaceOutputBuffer(frameCount * 4).order(ByteOrder.nativeOrder())
    val inBuf = inputBuffer.order(ByteOrder.nativeOrder())

    val eqEnabled = AudioEqualizerBus.isEnabled
    val spatialEnabled = Audio8DEffectBus.isEnabled
    val panFreqHz = Audio8DEffectBus.speed.frequencyHz
    val intensityDepth = (Audio8DEffectBus.intensityPercent / 100f).coerceIn(0f, 1f)
    val phaseIncrement = (2.0 * PI * panFreqHz) / sampleRateHz.toDouble()
    val isHeadphonesRoute = AudioOutputRouteBus.currentRoute.type != AudioOutputRouteType.SPEAKER
    val binauralFloor = if (isHeadphonesRoute) 0.12 else 0.22
    val binauralSpan = 1.0 - binauralFloor

    val boostPercent = VolumeBoostBus.boostPercent
    val boostFineTrim = VolumeBoostBus.hardwareFineTrim
    val ampEnabled = AudioAmplifierBus.isEnabled
    val ampLevelPercent = AudioAmplifierBus.levelPercent
    val rawCombinedGain =
      AudioAmplifierBus.computeCombinedDspGain(
        boostPercent = boostPercent,
        hardwareFineTrim = boostFineTrim,
        amplifierEnabled = ampEnabled,
        amplifierLevelPercent = ampLevelPercent,
      )
    val totalBoostDb =
      if (rawCombinedGain > 1.005f) {
        (20.0 * kotlin.math.log10(rawCombinedGain.toDouble())).toFloat().coerceAtLeast(0f)
      } else {
        0f
      }
    val overdriveCeiling = (1.15f + (totalBoostDb / 10f) * 1.15f).coerceIn(1.15f, 14.0f)
    val compAttackAlpha = (1f - kotlin.math.exp(-1.0 / (sampleRateHz * 0.0015))).toFloat().coerceIn(0.005f, 0.25f)
    val compReleaseAlpha = (1f - kotlin.math.exp(-1.0 / (sampleRateHz * 0.090))).toFloat().coerceIn(0.0001f, 0.05f)
    val gainTrackAlpha = (1f - kotlin.math.exp(-1.0 / (sampleRateHz * 0.0035))).toFloat().coerceIn(0.002f, 0.18f)

    // Binaural ITD (0.58ms max across head width) + multi-stage room reflection & tank taps
    val itdMaxSamples = (sampleRateHz * 0.00058).toFloat().coerceIn(4f, 40f)
    val erTap1 = (sampleRateHz * 0.013).toInt().coerceIn(48, bufferSize - 1)
    val erTap2 = (sampleRateHz * 0.023).toInt().coerceIn(48, bufferSize - 1)
    val tankTapL = (sampleRateHz * 0.043).toInt().coerceIn(128, bufferSize - 1)
    val tankTapR = (sampleRateHz * 0.059).toInt().coerceIn(128, bufferSize - 1)

    // Normalize 1-pole lowpass coefficient by sample rate
    val srScale = (44100f / sampleRateHz.toFloat()).coerceIn(0.25f, 2f)
    val reverbDampAlpha = (0.34f * srScale).coerceIn(0.10f, 0.72f)
    val reverbFeedback = 0.46f * intensityDepth

    val msPerSample = 1000.0 / sampleRateHz.toDouble()

    for (i in 0 until frameCount) {
      var sampleL = readNextPcmSample(inBuf)
      var sampleR = if (inputChannels >= 2) readNextPcmSample(inBuf) else sampleL
      if (inputChannels > 2) {
        for (ch in 2 until inputChannels) {
          val extra = readNextPcmSample(inBuf)
          when (ch) {
            2 -> {
              sampleL += extra * 0.707f
              sampleR += extra * 0.707f
            }
            3 -> {
              sampleL += extra * 0.5f
              sampleR += extra * 0.5f
            }
            else -> {
              if (ch % 2 == 0) sampleL += extra * 0.5f else sampleR += extra * 0.5f
            }
          }
        }
      }

      // Step 1: Real-time 5-band Graphic Equalizer cascade
      if (eqEnabled) {
        sampleL *= eqHeadroomScale
        sampleR *= eqHeadroomScale
        for (b in 0 until AudioEqualizerBus.BAND_COUNT) {
          sampleL = eqBands[b].processLeft(sampleL)
          sampleR = eqBands[b].processRight(sampleR)
        }
      }

      processedFramesSinceFlush++
      val audioTimeMs = processedFramesSinceFlush * msPerSample
      val monoNorm = ((sampleL + sampleR) * 0.5f) / 32768f
      val analyzedFrame = fftAnalyzer.pushSample(monoNorm, audioTimeMs)
      if (analyzedFrame != null) {
        AudioVisualizerBus.enqueueFrame(analyzedFrame, audioTimeMs)
      }

      var outL = sampleL
      var outR = sampleR

      // Step 2: Real-time 8D Spatial Audio (if enabled)
      if (spatialEnabled && intensityDepth > 0.001f) {
        phaseRad += phaseIncrement
        if (phaseRad >= 2.0 * PI) {
          phaseRad -= 2.0 * PI
        }

        // Focus stereo program slightly toward coherent center-stage before orbiting 360° around listener
        val monoCenter = (sampleL + sampleR) * 0.5f
        val widthKeep = 1f - 0.35f * intensityDepth
        val stageL = monoCenter * (1f - widthKeep) + sampleL * widthKeep
        val stageR = monoCenter * (1f - widthKeep) + sampleR * widthKeep

        val pan = sin(phaseRad) * intensityDepth // -1 = Left ear, +1 = Right ear
        val frontBack = cos(phaseRad) // +1 = Front, -1 = Behind head

        // Equal-power binaural panning adapted cleanly to Headphone vs Speaker routing
        val theta = (pan * 0.5 + 0.5) * (PI * 0.5)
        val binauralL = (binauralFloor + binauralSpan * cos(theta)).toFloat()
        val binauralR = (binauralFloor + binauralSpan * sin(theta)).toFloat()
        val leftPanGain = (1f - intensityDepth) + binauralL * intensityDepth
        val rightPanGain = (1f - intensityDepth) + binauralR * intensityDepth

        // Rear-hemisphere distance & pinna darkening cue when source orbits behind head
        val rearFactor = (((1.0 - frontBack) * 0.5) * intensityDepth).toFloat().coerceIn(0f, 1f)
        val distanceAttenuation = (1f - rearFactor * 0.16f).coerceIn(0.82f, 1f)

        leftDelayBuf[writeIndex] = stageL
        rightDelayBuf[writeIndex] = stageR

        // Smooth linearly interpolated Interaural Time Difference (ITD) for lifelike localization
        val leftDelaySamples = if (pan > 0.0) (pan.toFloat() * itdMaxSamples) else 0f
        val rightDelaySamples = if (pan < 0.0) ((-pan).toFloat() * itdMaxSamples) else 0f

        val delayedL = readInterpolated(leftDelayBuf, writeIndex, leftDelaySamples, bufferMask)
        val delayedR = readInterpolated(rightDelayBuf, writeIndex, rightDelaySamples, bufferMask)

        // Contralateral Acoustic Head-Shadowing (ear away from source loses high frequencies smoothly)
        val leftOcclusion = (((pan.coerceAtLeast(0.0)).toFloat() * 0.80f + rearFactor * 0.32f) * intensityDepth).coerceIn(0f, 0.92f)
        val rightOcclusion = ((((-pan).coerceAtLeast(0.0)).toFloat() * 0.80f + rearFactor * 0.32f) * intensityDepth).coerceIn(0f, 0.92f)
        val alphaL = (1f - 0.86f * leftOcclusion * (1f / srScale).coerceAtLeast(0.6f)).coerceIn(0.12f, 1f)
        val alphaR = (1f - 0.86f * rightOcclusion * (1f / srScale).coerceAtLeast(0.6f)).coerceIn(0.12f, 1f)

        headShadowLpL += alphaL * (delayedL - headShadowLpL)
        headShadowLpR += alphaR * (delayedR - headShadowLpR)
        val shadowedL = delayedL * (1f - leftOcclusion * 0.72f) + headShadowLpL * (leftOcclusion * 0.72f)
        val shadowedR = delayedR * (1f - rightOcclusion * 0.72f) + headShadowLpR * (rightOcclusion * 0.72f)

        // Damped Stereo Hall Reverb (early reflections + cross-coupled damped reverb tank)
        val earlyL =
          leftDelayBuf[(writeIndex - erTap1) and bufferMask] * 0.45f +
            rightDelayBuf[(writeIndex - erTap2) and bufferMask] * 0.35f
        val earlyR =
          rightDelayBuf[(writeIndex - erTap1) and bufferMask] * 0.45f +
            leftDelayBuf[(writeIndex - erTap2) and bufferMask] * 0.35f

        val prevTankL = reverbBufL[(writeIndex - tankTapL) and bufferMask]
        val prevTankR = reverbBufR[(writeIndex - tankTapR) and bufferMask]
        reverbDampL += reverbDampAlpha * (prevTankL - reverbDampL)
        reverbDampR += reverbDampAlpha * (prevTankR - reverbDampR)

        val revInL = earlyL * 0.55f + reverbDampR * reverbFeedback
        val revInR = earlyR * 0.55f + reverbDampL * reverbFeedback
        reverbBufL[writeIndex] = revInL.coerceIn(-32768f, 32767f)
        reverbBufR[writeIndex] = revInR.coerceIn(-32768f, 32767f)

        val wetMix = 0.24f * intensityDepth
        val dryScale = 1f - (0.14f * intensityDepth)
        outL = shadowedL * leftPanGain * distanceAttenuation * dryScale + revInL * wetMix
        outR = shadowedR * rightPanGain * distanceAttenuation * dryScale + revInR * wetMix

        writeIndex = (writeIndex + 1) and bufferMask
      }

      // Step 3: Combined Hardware-Synced Volume Boost + Dedicated Real Amplifier + Soft-Knee Limiter
      if (rawCombinedGain <= 0.0001f) {
        outL = 0f
        outR = 0f
        compEnvelope = 0f
        compGainSmoothed = 0f
      } else if (rawCombinedGain > 1.005f) {
        val inNormL = outL / 32768f
        val inNormR = outR / 32768f
        val instPeak = max(abs(inNormL), abs(inNormR))
        compEnvelope =
          if (instPeak > compEnvelope) {
            compEnvelope + compAttackAlpha * (instPeak - compEnvelope)
          } else {
            compEnvelope + compReleaseAlpha * (instPeak - compEnvelope)
          }

        val desiredLevel = compEnvelope * rawCombinedGain
        val targetGain =
          if (desiredLevel > overdriveCeiling) {
            (overdriveCeiling / compEnvelope.coerceAtLeast(1e-5f)).coerceAtMost(rawCombinedGain)
          } else {
            rawCombinedGain
          }
        compGainSmoothed += gainTrackAlpha * (targetGain - compGainSmoothed)
        outL = softLimitPcm(inNormL * compGainSmoothed) * 32768f
        outR = softLimitPcm(inNormR * compGainSmoothed) * 32768f
      } else {
        compGainSmoothed += gainTrackAlpha * (rawCombinedGain - compGainSmoothed)
        if (compGainSmoothed > 1.005f) {
          outL = softLimitPcm((outL * compGainSmoothed) / 32768f) * 32768f
          outR = softLimitPcm((outR * compGainSmoothed) / 32768f) * 32768f
        } else {
          outL *= compGainSmoothed
          outR *= compGainSmoothed
        }
      }

      outputBuffer.putShort(outL.toInt().coerceIn(-32768, 32767).toShort())
      outputBuffer.putShort(outR.toInt().coerceIn(-32768, 32767).toShort())
    }

    if (spatialEnabled) {
      val degrees = ((phaseRad / (2.0 * PI)) * 360.0).toFloat()
      Audio8DEffectBus.publishAngle(degrees)
    }

    outputBuffer.flip()
  }

  private fun readNextPcmSample(buf: ByteBuffer): Float =
    when (inputEncoding) {
      C.ENCODING_PCM_16BIT -> buf.short.toFloat()
      C.ENCODING_PCM_FLOAT -> (buf.float * 32767f)
      C.ENCODING_PCM_24BIT -> {
        val b0: Int
        val b1: Int
        val b2: Int
        if (buf.order() == ByteOrder.BIG_ENDIAN) {
          b2 = buf.get().toInt()
          b1 = buf.get().toInt() and 0xFF
          b0 = buf.get().toInt() and 0xFF
        } else {
          b0 = buf.get().toInt() and 0xFF
          b1 = buf.get().toInt() and 0xFF
          b2 = buf.get().toInt()
        }
        ((b2 shl 16) or (b1 shl 8) or b0) / 256f
      }
      C.ENCODING_PCM_32BIT -> buf.int / 65536f
      else -> buf.short.toFloat()
    }

  /**
   * Smooth hyperbolic-tangent soft-knee limiter on normalized audio [-1f..1f]
   * eliminating harsh integer hard-clipping and static distortion at extreme boost levels.
   */
  internal fun softLimitPcm(normalizedSample: Float): Float {
    val mag = abs(normalizedSample)
    if (mag <= 0.82f) return normalizedSample
    val headroom = 0.16f
    val shaped = 0.82f + headroom * tanh(((mag - 0.82f) / headroom).toDouble()).toFloat()
    return sign(normalizedSample) * shaped.coerceAtMost(0.985f)
  }

  private fun readInterpolated(buf: FloatArray, writeIdx: Int, delaySamples: Float, mask: Int): Float {
    val intDelay = delaySamples.toInt()
    val frac = delaySamples - intDelay
    val idx0 = (writeIdx - intDelay) and mask
    val idx1 = (writeIdx - intDelay - 1) and mask
    val s0 = buf[idx0]
    val s1 = buf[idx1]
    return s0 + (s1 - s0) * frac
  }
}
