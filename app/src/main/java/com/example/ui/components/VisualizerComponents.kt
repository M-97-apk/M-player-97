package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.VisualizerMode
import com.example.playback.AudioVisualizerBus
import com.example.ui.theme.LocalMPlayerPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private suspend fun runFrameTimedPhase(
  durationMs: Long,
  onProgress: (Float) -> Unit,
) {
  var startNanos = Long.MIN_VALUE
  while (currentCoroutineContext().isActive) {
    var finished = false
    withFrameNanos { nanos ->
      if (startNanos == Long.MIN_VALUE) {
        startNanos = nanos
      }
      val elapsedMs = (nanos - startNanos) / 1_000_000f
      val progress = (elapsedMs / durationMs.toFloat()).coerceIn(0f, 1f)
      onProgress(progress)
      if (progress >= 1f) {
        finished = true
      }
    }
    if (finished) break
  }
}

enum class CosmicSpherePhase {
  IDLE,
  SWELLING,
  BURST,
  STARFALL,
  REMERGE,
}

@Stable
class CosmicStarfallState {
  var phase by mutableStateOf(CosmicSpherePhase.IDLE)
  var swellProgress by mutableFloatStateOf(0f)
  var starfallProgress by mutableFloatStateOf(0f)
  var remergeProgress by mutableFloatStateOf(0f)
  var sphereCenterInScreen by mutableStateOf<Offset?>(null)
  var hasCompletedCycleWithCurrentTouch by mutableStateOf(false)

  val isStarfallActive get() =
    phase == CosmicSpherePhase.BURST ||
    phase == CosmicSpherePhase.STARFALL ||
    phase == CosmicSpherePhase.REMERGE
}

@Composable
fun VisualizerModeSelector(
  selectedMode: VisualizerMode,
  onSelectMode: (VisualizerMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  val options =
    listOf(
      Triple(VisualizerMode.ALBUM_ART, strings.visualizerAlbumArt, Icons.Rounded.Album),
      Triple(VisualizerMode.SPECTRUM_BARS, strings.visualizerSpectrum, Icons.Rounded.BarChart),
      Triple(VisualizerMode.PULSING_CIRCLE, strings.visualizerCircular, Icons.Rounded.GraphicEq),
      Triple(VisualizerMode.ENERGY_ORB, strings.visualizerEnergyOrb, Icons.Rounded.BlurCircular),
    )

  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(palette.cardSurface)
        .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(50))
        .horizontalScroll(rememberScrollState())
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD
    options.forEach { (mode, label, icon) ->
      val selected = mode == selectedMode
      val pillMod =
        when {
          !selected -> Modifier.clip(RoundedCornerShape(50))
          isPitchBlackGold ->
            Modifier.clip(RoundedCornerShape(50))
              .background(palette.buttonContainerColor)
              .border(0.9.dp, palette.buttonBorderColor, RoundedCornerShape(50))
          else ->
            Modifier.activeGlassOptionPillDecoration(selected = true, shape = RoundedCornerShape(50))
        }
      Row(
        modifier =
          pillMod
            .clickable { onSelectMode(mode) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("visualizer_mode_${mode.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint =
            when {
              selected && isPitchBlackGold -> palette.buttonContentColor
              selected -> palette.accentGlow
              else -> palette.textSecondary
            },
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          color =
            when {
              selected && isPitchBlackGold -> palette.buttonContentColor
              selected -> palette.accentGlow
              else -> palette.textSecondary
            },
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun DynamicPlayerVisualizer(
  mode: VisualizerMode,
  isPlaying: Boolean,
  is8DEnabled: Boolean,
  panAngleDegrees: Float,
  amplitude: Float,
  bassEnergy: Float,
  waveformSamples: FloatArray,
  spectrumBins: FloatArray,
  albumArtUri: Any?,
  title: String,
  modifier: Modifier = Modifier,
  cosmicStarfallState: CosmicStarfallState = remember { CosmicStarfallState() },
  screenRootCoordinates: LayoutCoordinates? = null,
) {
  AnimatedContent(
    targetState = mode,
    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(200)) },
    label = "visualizer_mode_transition",
    modifier = modifier.fillMaxWidth().height(276.dp),
  ) { currentMode ->
    when (currentMode) {
      VisualizerMode.ALBUM_ART -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Orbit8DVisualizer(
            is8DEnabled = is8DEnabled,
            isPlaying = isPlaying,
            panAngleDegrees = panAngleDegrees,
            albumArtUri = albumArtUri,
            title = title,
          )
        }
      }

      VisualizerMode.SPECTRUM_BARS -> {
        InteractiveSpectrumBarsVisualizer(
          isPlaying = isPlaying,
          is8DEnabled = is8DEnabled,
          bassEnergy = bassEnergy,
          spectrumBins = spectrumBins,
        )
      }

      VisualizerMode.PULSING_CIRCLE -> {
        PulsingCircularBassVisualizer(
          isPlaying = isPlaying,
          is8DEnabled = is8DEnabled,
          panAngleDegrees = panAngleDegrees,
          bassEnergy = bassEnergy,
          spectrumBins = spectrumBins,
          albumArtUri = albumArtUri,
          title = title,
        )
      }

      VisualizerMode.ENERGY_ORB -> {
        InteractiveEnergyOrbVisualizer(
          isPlaying = isPlaying,
          is8DEnabled = is8DEnabled,
          panAngleDegrees = panAngleDegrees,
          amplitude = amplitude,
          bassEnergy = bassEnergy,
          spectrumBins = spectrumBins,
          cosmicState = cosmicStarfallState,
          screenRootCoordinates = screenRootCoordinates,
        )
      }
    }
  }
}

/**
 * Real-time FFT Spectrum Bars Visualizer with floating peak-hold caps and mirror sheen.
 */
@Composable
private fun InteractiveSpectrumBarsVisualizer(
  isPlaying: Boolean,
  is8DEnabled: Boolean,
  bassEnergy: Float,
  spectrumBins: FloatArray,
) {
  val palette = LocalMPlayerPalette.current
  val barCount = AudioVisualizerBus.SPECTRUM_BINS
  val peakCaps = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.03f) } } }

  var touchRatioX by remember { mutableFloatStateOf(-1f) }
  var touchBoost by remember { mutableFloatStateOf(0f) }
  val animatedTouch by
    animateFloatAsState(
      targetValue = touchBoost,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "spectrum_touch",
    )

  val smoothBass by
    animateFloatAsState(
      targetValue = if (isPlaying) bassEnergy.coerceIn(0f, 1f) else 0f,
      animationSpec = tween(45),
      label = "spectrum_bass",
    )

  val barPrimary = palette.vizPrimary
  val barBase = palette.vizBase

  ThemedCard(
    cornerRadius = 28.dp,
    elevated = true,
    modifier =
      Modifier.fillMaxSize()
        .padding(horizontal = 4.dp)
        .testTag("visualizer_spectrum_canvas"),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(
              onPress = { offset ->
                touchRatioX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                touchBoost = 1f
                tryAwaitRelease()
                touchBoost = 0f
              }
            )
          }
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { offset ->
                touchRatioX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                touchBoost = 1f
              },
              onDragEnd = { touchBoost = 0f },
              onDragCancel = { touchBoost = 0f },
              onDrag = { change, _ ->
                change.consume()
                touchRatioX = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
              },
            )
          }
    ) {
      Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp)) {
        val totalW = size.width
        val baseLineY = size.height * 0.78f
        val maxBarH = size.height * 0.68f
        val gap = 5.dp.toPx()
        val barW = ((totalW - gap * (barCount - 1)) / barCount.toFloat()).coerceAtLeast(4.dp.toPx())

        // Baseline horizon glow
        drawLine(
          color = barBase.copy(alpha = 0.35f),
          start = Offset(0f, baseLineY),
          end = Offset(totalW, baseLineY),
          strokeWidth = 1.dp.toPx(),
        )

        for (i in 0 until barCount) {
          val ratio = i.toFloat() / (barCount - 1).toFloat()
          val rawBin = if (isPlaying) spectrumBins.getOrElse(i) { 0f } else 0f
          val lowKickWeight = (1f - (ratio * 3.2f)).coerceIn(0f, 1f) * 0.14f * smoothBass
          val fingerBoost =
            if (touchRatioX >= 0f && animatedTouch > 0.01f) {
              val d = kotlin.math.abs(ratio - touchRatioX)
              (1f - (d * 3.5f).coerceIn(0f, 1f)) * animatedTouch * 0.48f
            } else {
              0f
            }

          val level = (rawBin + lowKickWeight + fingerBoost).coerceIn(0.03f, 1f)
          val prevCap = peakCaps[i]
          val newCap = if (level >= prevCap) level else (prevCap - 0.014f).coerceAtLeast(level)
          peakCaps[i] = newCap

          val barH = maxBarH * level
          val x = i * (barW + gap)
          val topY = baseLineY - barH

          // Outer glow bloom behind tall active bars
          if (level > 0.32f) {
            drawRoundRect(
              color = barBase.copy(alpha = 0.24f * level),
              topLeft = Offset(x - 1.5.dp.toPx(), topY - 2.dp.toPx()),
              size = Size(barW + 3.dp.toPx(), barH + 2.dp.toPx()),
              cornerRadius = CornerRadius(barW / 2f),
            )
          }

          // Primary frequency column
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(barPrimary, barBase), startY = topY, endY = baseLineY),
            topLeft = Offset(x, topY),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barW / 2f),
          )

          // Mirror reflection below baseline
          val reflectH = (barH * 0.24f).coerceAtMost(size.height - baseLineY - 2.dp.toPx())
          if (reflectH > 2f) {
            drawRoundRect(
              brush =
                Brush.verticalGradient(
                  listOf(barBase.copy(alpha = 0.28f), Color.Transparent),
                  startY = baseLineY + 2.dp.toPx(),
                  endY = baseLineY + 2.dp.toPx() + reflectH,
                ),
              topLeft = Offset(x, baseLineY + 2.dp.toPx()),
              size = Size(barW, reflectH),
              cornerRadius = CornerRadius(barW / 2f),
            )
          }

          // Floating Peak Hold Cap
          val capY = (baseLineY - maxBarH * newCap - 4.dp.toPx()).coerceAtLeast(2.dp.toPx())
          drawRoundRect(
            color = barPrimary.copy(alpha = 0.92f),
            topLeft = Offset(x, capY),
            size = Size(barW, 2.5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
          )
        }
      }

      if (is8DEnabled) {
        Icon(
          imageVector = Icons.Rounded.Headphones,
          contentDescription = null,
          tint = barBase,
          modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(18.dp),
        )
      }
    }
  }
}

/**
 * Pulsing Circular Visualizer reacting dynamically to Bass beat drops and circular FFT spokes.
 */
@Composable
private fun PulsingCircularBassVisualizer(
  isPlaying: Boolean,
  is8DEnabled: Boolean,
  panAngleDegrees: Float,
  bassEnergy: Float,
  spectrumBins: FloatArray,
  albumArtUri: Any?,
  title: String,
) {
  val palette = LocalMPlayerPalette.current
  var slowRot by remember { mutableFloatStateOf(0f) }

  androidx.compose.runtime.LaunchedEffect(isPlaying) {
    if (!isPlaying) return@LaunchedEffect
    var lastNanos = 0L
    while (true) {
      androidx.compose.runtime.withFrameNanos { nanos ->
        if (lastNanos != 0L) {
          val dtSec = ((nanos - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
          // Subtle rotation speed accelerates gently on strong bass beats
          slowRot = (slowRot + dtSec * (0.28f + bassEnergy * 0.55f)) % (2f * PI).toFloat()
        }
        lastNanos = nanos
      }
    }
  }

  val smoothBass by
    animateFloatAsState(
      targetValue = if (isPlaying) bassEnergy.coerceIn(0f, 1f) else 0f,
      animationSpec = tween(48),
      label = "smooth_bass_pulse",
    )

  val ringPrimary = palette.vizPrimary
  val ringAccent = palette.vizBase

  Box(
    modifier = Modifier.fillMaxSize().testTag("visualizer_pulsing_circle_canvas"),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(270.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val baseInnerR = size.minDimension * 0.285f
      val pulseRadius = baseInnerR * (1f + smoothBass * 0.24f)

      // Expanding Bass Shockwave Rings (directly synced to beat onset & sub-bass)
      drawCircle(
        brush =
          Brush.radialGradient(
            listOf(ringAccent.copy(alpha = (0.36f * smoothBass + 0.05f).coerceIn(0f, 0.45f)), Color.Transparent),
            center = center,
            radius = pulseRadius * 1.65f,
          ),
        radius = pulseRadius * 1.65f,
        center = center,
      )
      drawCircle(
        color = ringAccent.copy(alpha = 0.18f + smoothBass * 0.56f),
        radius = pulseRadius * (1.26f + smoothBass * 0.22f),
        center = center,
        style = Stroke(width = (1.3f + smoothBass * 2.4f).dp.toPx()),
      )

      // 32 Circular Radial Frequency Spokes reacting strictly to live FFT spectrum
      val spokes = 32
      val maxRayLen = size.minDimension * 0.175f
      for (i in 0 until spokes) {
        val angle = slowRot + (i.toFloat() / spokes.toFloat()) * (2f * PI).toFloat()
        val binIdx = (i * spectrumBins.size / spokes).coerceIn(0, spectrumBins.lastIndex)
        val energy = if (isPlaying) spectrumBins.getOrElse(binIdx) { 0f } else 0f
        val rayLen = 3.dp.toPx() + maxRayLen * (energy * 0.88f + smoothBass * 0.28f).coerceIn(0f, 1.15f)
        val r0 = pulseRadius + 3.dp.toPx()
        val r1 = r0 + rayLen
        val cosA = cos(angle)
        val sinA = sin(angle)

        drawLine(
          brush =
            Brush.linearGradient(
              listOf(ringAccent, ringPrimary),
              start = Offset(center.x + r0 * cosA, center.y + r0 * sinA),
              end = Offset(center.x + r1 * cosA, center.y + r1 * sinA),
            ),
          start = Offset(center.x + r0 * cosA, center.y + r0 * sinA),
          end = Offset(center.x + r1 * cosA, center.y + r1 * sinA),
          strokeWidth = 3.2.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // Live 8D orbit dot around outer perimeter if 8D is active
      if (is8DEnabled) {
        val orbitR = size.minDimension * 0.47f
        val rad = panAngleDegrees * (PI / 180.0)
        val dx = center.x + (orbitR * sin(rad)).toFloat()
        val dy = center.y - (orbitR * cos(rad)).toFloat()
        drawCircle(
          color = ringAccent.copy(alpha = 0.55f),
          radius = 8.dp.toPx(),
          center = Offset(dx, dy),
        )
        drawCircle(
          color = ringPrimary,
          radius = 4.dp.toPx(),
          center = Offset(dx, dy),
        )
      }
    }

    // Centered Album Art Core pulsing slightly with bass beat
    val coreScale = 1f + smoothBass * 0.08f
    Box(
      modifier =
        Modifier.size((142f * coreScale).dp)
          .clip(CircleShape)
          .background(palette.cardSurfaceElevated)
          .border(1.8.dp, palette.borderBrush, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      if (albumArtUri != null) {
        AsyncImage(
          model = albumArtUri,
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.mplayer_icon_art),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

private data class Particle3DNode(
  val nx: Float,
  val ny: Float,
  val nz: Float,
  val baseRadius: Float,
  val shell: Int,
  val bandIndex: Int,
)

private data class ProjectedParticle3D(
  val screenX: Float,
  val screenY: Float,
  val depthZ: Float,
  val radiusPx: Float,
  val alpha: Float,
  val energy: Float,
  val shell: Int,
)

@Composable
private fun InteractiveEnergyOrbVisualizer(
  isPlaying: Boolean,
  is8DEnabled: Boolean,
  panAngleDegrees: Float,
  amplitude: Float,
  bassEnergy: Float,
  spectrumBins: FloatArray,
  cosmicState: CosmicStarfallState,
  screenRootCoordinates: LayoutCoordinates? = null,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val density = LocalDensity.current

  var rotationPhase by remember { mutableFloatStateOf(0f) }
  var touchYaw by remember { mutableFloatStateOf(0f) }
  var touchPitch by remember { mutableFloatStateOf(0.28f) }
  var touchRoll by remember { mutableFloatStateOf(0f) }
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var yawVelocity by remember { mutableFloatStateOf(0f) }
  var pitchVelocity by remember { mutableFloatStateOf(0f) }
  var rawCenterShift by remember { mutableStateOf(Offset.Zero) }
  var pointerCanvasPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
  var isPressed by remember { mutableStateOf(false) }
  var hasCompletedCycleWithCurrentTouch by remember { mutableStateOf(false) }
  var cosmicCycleJob by remember { mutableStateOf<Job?>(null) }
  var deflateJob by remember { mutableStateOf<Job?>(null) }
  val shockwaveProgress = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  val isCosmicTheme = palette.themeStyle == AppThemeStyle.COSMIC_STARFIELD

  DisposableEffect(isCosmicTheme) {
    if (!isCosmicTheme) {
      cosmicCycleJob?.cancel()
      cosmicCycleJob = null
      deflateJob?.cancel()
      deflateJob = null
      cosmicState.phase = CosmicSpherePhase.IDLE
      cosmicState.swellProgress = 0f
      cosmicState.starfallProgress = 0f
      cosmicState.remergeProgress = 0f
      cosmicState.hasCompletedCycleWithCurrentTouch = false
      hasCompletedCycleWithCurrentTouch = false
    }
    onDispose {
      cosmicCycleJob?.cancel()
      cosmicCycleJob = null
      deflateJob?.cancel()
      deflateJob = null
      cosmicState.phase = CosmicSpherePhase.IDLE
      cosmicState.swellProgress = 0f
      cosmicState.starfallProgress = 0f
      cosmicState.remergeProgress = 0f
      cosmicState.hasCompletedCycleWithCurrentTouch = false
    }
  }

  // Continuous physics loop: drives live audio rotation + post-release spin inertia simultaneously
  androidx.compose.runtime.LaunchedEffect(isPlaying, isPressed) {
    if (!isPlaying && isPressed) return@LaunchedEffect
    var lastNanos = 0L
    while (true) {
      val hasInertia = abs(yawVelocity) > 0.008f || abs(pitchVelocity) > 0.008f
      if (!isPlaying && !hasInertia) break
      androidx.compose.runtime.withFrameNanos { nanos ->
        if (lastNanos != 0L) {
          val dtSec = ((nanos - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
          if (isPlaying) {
            // Spin organically in proportion to live audio energy (amplitude + bass)
            val audioSpeed = 0.22f + amplitude * 0.56f + bassEnergy * 0.44f
            rotationPhase = (rotationPhase + dtSec * audioSpeed) % (2f * PI).toFloat()
          }
          if (!isPressed && hasInertia) {
            touchYaw += yawVelocity * dtSec
            touchPitch = (touchPitch + pitchVelocity * dtSec).coerceIn(-1.40f, 1.40f)
            val damp = exp(-dtSec * 2.8f)
            yawVelocity *= damp
            pitchVelocity *= damp
            if (abs(yawVelocity) <= 0.008f) yawVelocity = 0f
            if (abs(pitchVelocity) <= 0.008f) pitchVelocity = 0f
          }
        }
        lastNanos = nanos
      }
    }
  }

  // True 3D Volumetric Fibonacci Sphere across 3 concentric shells (SO(3)-invariant, never flattens)
  val sphereGeometry = remember {
    val nodes = ArrayList<Particle3DNode>(228)
    val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat()
    val shellSpecs = listOf(0.44f to 36, 0.72f to 72, 1.00f to 120)

    shellSpecs.forEachIndexed { shellIdx, (shellRadius, count) ->
      for (i in 0 until count) {
        val t = (i + 0.5f) / count.toFloat()
        val y = 1f - 2f * t
        val radial = sqrt((1f - y * y).coerceAtLeast(0f))
        val theta = goldenAngle * i + shellIdx * 0.85f
        val x = cos(theta) * radial
        val z = sin(theta) * radial
        // Coherent latitude-based FFT band index so ripples sweep cleanly across the 3D shells
        val mappedBand =
          ((t * (AudioVisualizerBus.SPECTRUM_BINS - 1)).toInt())
            .coerceIn(0, AudioVisualizerBus.SPECTRUM_BINS - 1)
        nodes.add(
          Particle3DNode(
            nx = x,
            ny = y,
            nz = z,
            baseRadius = shellRadius,
            shell = shellIdx,
            bandIndex = mappedBand,
          )
        )
      }
    }

    val edges = ArrayList<Pair<Int, Int>>(360)
    for (i in nodes.indices) {
      val a = nodes[i]
      val ax = a.nx * a.baseRadius
      val ay = a.ny * a.baseRadius
      val az = a.nz * a.baseRadius
      var best1 = -1
      var best1Dist = Float.MAX_VALUE
      var best2 = -1
      var best2Dist = Float.MAX_VALUE
      for (j in (i + 1) until nodes.size) {
        val b = nodes[j]
        if (a.shell != b.shell && j % 3 != 0) continue
        val dx = ax - b.nx * b.baseRadius
        val dy = ay - b.ny * b.baseRadius
        val dz = az - b.nz * b.baseRadius
        val d2 = dx * dx + dy * dy + dz * dz
        val maxD2 = if (a.shell == b.shell) 0.24f else 0.14f
        if (d2 < maxD2) {
          if (d2 < best1Dist) {
            best2 = best1
            best2Dist = best1Dist
            best1 = j
            best1Dist = d2
          } else if (d2 < best2Dist) {
            best2 = j
            best2Dist = d2
          }
        }
      }
      if (best1 >= 0) edges.add(i to best1)
      if (best2 >= 0) edges.add(i to best2)
    }
    nodes.toList() to edges.toList()
  }

  val nodes = sphereGeometry.first
  val edges = sphereGeometry.second

  val springCenterShift by
    animateOffsetAsState(
      targetValue = rawCenterShift,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "sphere_center_spring",
    )

  val smoothYaw by
    animateFloatAsState(
      targetValue = touchYaw,
      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
      label = "sphere_yaw",
    )

  val smoothPitch by
    animateFloatAsState(
      targetValue = touchPitch,
      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
      label = "sphere_pitch",
    )

  val smoothRoll by
    animateFloatAsState(
      targetValue = touchRoll,
      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
      label = "sphere_roll",
    )

  val smoothZoom by
    animateFloatAsState(
      targetValue = zoomScale,
      animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
      label = "sphere_zoom",
    )

  val touchEnergy by
    animateFloatAsState(
      targetValue = if (isPressed) 1f else 0f,
      animationSpec = spring(stiffness = Spring.StiffnessLow),
      label = "sphere_touch_energy",
    )

  val beatAmp by
    animateFloatAsState(
      targetValue = if (isPlaying) bassEnergy.coerceIn(0f, 1f) else 0f,
      animationSpec = tween(48),
      label = "sphere_beat",
    )

  val smoothAmplitude by
    animateFloatAsState(
      targetValue = if (isPlaying) amplitude.coerceIn(0f, 1f) else 0f,
      animationSpec = tween(60),
      label = "sphere_amplitude",
    )

  val frontParticleColor = palette.vizPrimary
  val midParticleColor = palette.vizMid
  val backParticleColor = palette.vizBack
  val latticeLineColor = palette.vizLattice
  val volumetricGlowColor = palette.vizBase
  val outerBoundaryColor = palette.vizOuterRing
  val outerBoundaryHighlight = palette.vizPrimary

  val projected = remember(nodes.size) {
    Array(nodes.size) { ProjectedParticle3D(0f, 0f, 0f, 0f, 0f, 0f, 0) }
  }
  val drawOrder = remember(nodes.size) { IntArray(nodes.size) { it } }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .onGloballyPositioned { layoutCoordinates ->
          val root = screenRootCoordinates
          val yOffsetPx = with(density) { 8.dp.toPx() }
          if (root != null && root.isAttached && layoutCoordinates.isAttached) {
            cosmicState.sphereCenterInScreen =
              root.localPositionOf(
                layoutCoordinates,
                Offset(layoutCoordinates.size.width / 2f, layoutCoordinates.size.height / 2f - yOffsetPx),
              )
          } else {
            cosmicState.sphereCenterInScreen =
              Offset(layoutCoordinates.size.width / 2f, layoutCoordinates.size.height / 2f - yOffsetPx)
          }
        }
        .pointerInput(palette.themeStyle) {
          awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            isPressed = true
            yawVelocity = 0f
            pitchVelocity = 0f
            val startDownPos = firstDown.position
            pointerCanvasPositions = listOf(startDownPos)

            var steadyHoldJob: Job? = null
            try {
              if (
                isCosmicTheme &&
                  !hasCompletedCycleWithCurrentTouch &&
                  cosmicState.phase == CosmicSpherePhase.IDLE &&
                  cosmicCycleJob?.isActive != true
              ) {
                deflateJob?.cancel()
                deflateJob = null
                steadyHoldJob =
                  scope.launch {
                    cosmicState.phase = CosmicSpherePhase.SWELLING
                    cosmicState.swellProgress = 0f
                    runFrameTimedPhase(durationMs = 7000L) { progress ->
                      cosmicState.swellProgress = progress
                    }

                    // Exactly 7.0 seconds held steadily! Start uninterrupted full cycle on scope.
                    hasCompletedCycleWithCurrentTouch = true
                    cosmicState.hasCompletedCycleWithCurrentTouch = true
                    cosmicCycleJob?.cancel()
                    cosmicCycleJob =
                      scope.launch {
                        try {
                          // Burst phase (starlight blast)
                          cosmicState.phase = CosmicSpherePhase.BURST
                          cosmicState.swellProgress = 1f
                          runFrameTimedPhase(durationMs = 280L) {}

                          // 7s snow-like starfall across the music player sphere
                          cosmicState.phase = CosmicSpherePhase.STARFALL
                          cosmicState.starfallProgress = 0f
                          runFrameTimedPhase(durationMs = 7000L) { progress ->
                            cosmicState.starfallProgress = progress
                          }

                          // Accelerating vortex spin & remerge
                          cosmicState.phase = CosmicSpherePhase.REMERGE
                          cosmicState.remergeProgress = 0f
                          runFrameTimedPhase(durationMs = 2800L) { progress ->
                            cosmicState.remergeProgress = progress
                          }
                        } finally {
                          // Clean return to IDLE
                          cosmicState.phase = CosmicSpherePhase.IDLE
                          cosmicState.swellProgress = 0f
                          cosmicState.starfallProgress = 0f
                          cosmicState.remergeProgress = 0f
                        }
                      }
                  }
              }

              scope.launch {
                shockwaveProgress.snapTo(0f)
                shockwaveProgress.animateTo(1f, animationSpec = tween(560))
              }

              // Soft immediate lean toward first finger touch
              val viewCenter = Offset(size.width / 2f, size.height / 2f)
              val initialDelta = firstDown.position - viewCenter
              rawCenterShift =
                Offset(
                  (initialDelta.x * 0.12f).coerceIn(-14.dp.toPx(), 14.dp.toPx()),
                  (initialDelta.y * 0.12f).coerceIn(-14.dp.toPx(), 14.dp.toPx()),
                )

              var prevCentroid = firstDown.position
              var prevSpan = 0f
              var prevAngle = 0f
              var prevHadTwoFingers = false
              var lastEventTimeNanos = System.nanoTime()

              do {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                pointerCanvasPositions = pressed.map { it.position }
                if (pressed.isEmpty()) break

                val nowNanos = System.nanoTime()
                val dtSec = ((nowNanos - lastEventTimeNanos) / 1_000_000_000.0).toFloat().coerceIn(0.004f, 0.05f)
                lastEventTimeNanos = nowNanos

                var sumX = 0f
                var sumY = 0f
                pressed.forEach { change ->
                  sumX += change.position.x
                  sumY += change.position.y
                }
                val centroid = Offset(sumX / pressed.size.toFloat(), sumY / pressed.size.toFloat())

                // DRAGGING CHECK:
                // Dragging/moving fingers must NOT count as a long press during the 7s hold.
                // Once the cycle has started (cosmicCycleJob active), dragging will NOT cancel it.
                val distFromStart = hypot(centroid.x - startDownPos.x, centroid.y - startDownPos.y)
                if (distFromStart > 10.dp.toPx()) {
                  if (steadyHoldJob != null && steadyHoldJob.isActive) {
                    steadyHoldJob.cancel()
                    steadyHoldJob = null
                    if (cosmicState.phase == CosmicSpherePhase.SWELLING) {
                      deflateJob?.cancel()
                      deflateJob =
                        scope.launch {
                          while (cosmicState.swellProgress > 0f) {
                            cosmicState.swellProgress = (cosmicState.swellProgress - 0.08f).coerceAtLeast(0f)
                            delay(16L)
                          }
                          if (cosmicState.phase == CosmicSpherePhase.SWELLING) {
                            cosmicState.phase = CosmicSpherePhase.IDLE
                          }
                        }
                    }
                  }
                }

                if (pressed.size >= 2) {
                  val p0 = pressed[0].position
                  val p1 = pressed[1].position
                  val dx = p1.x - p0.x
                  val dy = p1.y - p0.y
                  val span = hypot(dx, dy).coerceAtLeast(12f)
                  val angle = atan2(dy, dx)
                  if (prevHadTwoFingers && prevSpan > 12f) {
                    val scaleFactor = (span / prevSpan).coerceIn(0.84f, 1.18f)
                    zoomScale = (zoomScale * scaleFactor).coerceIn(0.70f, 1.45f)

                    var dAngle = angle - prevAngle
                    while (dAngle > PI.toFloat()) dAngle -= (2f * PI).toFloat()
                    while (dAngle < -PI.toFloat()) dAngle += (2f * PI).toFloat()
                    touchRoll += dAngle * 0.9f
                  }
                  prevSpan = span
                  prevAngle = angle
                  prevHadTwoFingers = true
                } else {
                  prevHadTwoFingers = false
                }

                val moveDelta = centroid - prevCentroid
                prevCentroid = centroid

                if (abs(moveDelta.x) > 0.1f || abs(moveDelta.y) > 0.1f) {
                  val dYaw = moveDelta.x * 0.012f
                  val dPitch = moveDelta.y * 0.012f
                  touchYaw += dYaw
                  touchPitch = (touchPitch + dPitch).coerceIn(-1.40f, 1.40f)
                  yawVelocity = (dYaw / dtSec).coerceIn(-9f, 9f)
                  pitchVelocity = (dPitch / dtSec).coerceIn(-7f, 7f)

                  val maxShift = 18.dp.toPx()
                  rawCenterShift =
                    Offset(
                      (rawCenterShift.x + moveDelta.x * 0.16f).coerceIn(-maxShift, maxShift),
                      (rawCenterShift.y + moveDelta.y * 0.16f).coerceIn(-maxShift, maxShift),
                    )
                }

                pressed.forEach { if (it.positionChanged()) it.consume() }
              } while (true)
            } finally {
              // Finger lifted (or gesture ended): cancel pre-burst hold ONLY, never cosmicCycleJob!
              steadyHoldJob?.cancel()
              if (cosmicState.phase == CosmicSpherePhase.SWELLING) {
                deflateJob?.cancel()
                deflateJob =
                  scope.launch {
                    while (cosmicState.swellProgress > 0f) {
                      cosmicState.swellProgress = (cosmicState.swellProgress - 0.08f).coerceAtLeast(0f)
                      delay(16L)
                    }
                    if (cosmicState.phase == CosmicSpherePhase.SWELLING) {
                      cosmicState.phase = CosmicSpherePhase.IDLE
                    }
                  }
              }
              isPressed = false
              hasCompletedCycleWithCurrentTouch = false
              cosmicState.hasCompletedCycleWithCurrentTouch = false
              pointerCanvasPositions = emptyList()
              rawCenterShift = Offset.Zero
            }
          }
        }
        .testTag("visualizer_energy_orb_canvas"),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val baseCenter = Offset(size.width / 2f, size.height / 2f - 8.dp.toPx())
      val center = baseCenter + springCenterShift
      val cameraZ = 2.85f

      val bbRadiusScale: Float
      val bbLatticeAlpha: Float

      if (isCosmicTheme) {
        when (cosmicState.phase) {
          CosmicSpherePhase.SWELLING -> {
            val f = cosmicState.swellProgress
            bbRadiusScale = 1f + 1.25f * sin(f * (PI.toFloat() / 2f))
            bbLatticeAlpha = 1f + 2.0f * f
          }
          CosmicSpherePhase.BURST -> {
            bbRadiusScale = 2.4f
            bbLatticeAlpha = 0f
          }
          CosmicSpherePhase.STARFALL -> {
            bbRadiusScale = 0f
            bbLatticeAlpha = 0f
          }
          CosmicSpherePhase.REMERGE -> {
            val p = cosmicState.remergeProgress
            if (p > 0.70f) {
              val f = (p - 0.70f) / 0.30f
              bbRadiusScale = 0.85f + 0.15f * f
              bbLatticeAlpha = f
            } else {
              bbRadiusScale = 0f
              bbLatticeAlpha = 0f
            }
          }
          CosmicSpherePhase.IDLE -> {
            bbRadiusScale = 1f
            bbLatticeAlpha = 1f
          }
        }
      } else {
        bbRadiusScale = 1f
        bbLatticeAlpha = 1f
      }

      val sphereRadius = size.minDimension * 0.36f * smoothZoom * bbRadiusScale

      val orbitRadius = (size.minDimension * 0.36f * smoothZoom) * 1.24f
      drawCircle(
        color = outerBoundaryColor.copy(alpha = if (is8DEnabled) 0.78f else 0.34f),
        radius = orbitRadius,
        center = baseCenter,
        style = Stroke(width = if (is8DEnabled) 1.8.dp.toPx() else 1.dp.toPx()),
      )

      if (is8DEnabled) {
        val panRad = panAngleDegrees * (PI / 180.0)
        val px = baseCenter.x + (orbitRadius * sin(panRad)).toFloat()
        val py = baseCenter.y - (orbitRadius * cos(panRad)).toFloat()
        drawCircle(
          color = outerBoundaryColor.copy(alpha = 0.45f),
          radius = 10.dp.toPx(),
          center = Offset(px, py),
        )
        drawCircle(
          color = outerBoundaryHighlight,
          radius = 4.5.dp.toPx(),
          center = Offset(px, py),
        )
      }

      if (bbRadiusScale > 0.05f) {
        val yaw = rotationPhase + smoothYaw
        val pitch = smoothPitch + sin(rotationPhase * 0.5f) * 0.22f
        val roll = smoothRoll + cos(rotationPhase * 0.5f) * 0.14f

        val cosY = cos(yaw)
        val sinY = sin(yaw)
        val cosP = cos(pitch)
        val sinP = sin(pitch)
        val cosR = cos(roll)
        val sinR = sin(roll)

        val activePointers = pointerCanvasPositions
        val shockWaveR = shockwaveProgress.value
        val panPhaseRad = (panAngleDegrees * (PI / 180.0)).toFloat()

        for (i in nodes.indices) {
          val n = nodes[i]
          val bandEnergy = if (isPlaying) spectrumBins.getOrElse(n.bandIndex) { 0f } else 0f
          val shellKickWeight = if (n.shell == 0) 0.26f else if (n.shell == 1) 0.20f else 0.15f
          val azimuthalRipple =
            if (isPlaying) {
              sin(atan2(n.nz, n.nx) * 2f - panPhaseRad - rotationPhase * 1.4f) *
                (0.03f + smoothAmplitude * 0.08f)
            } else {
              0f
            }
          val radialPulse =
            n.baseRadius *
              (1f + beatAmp * shellKickWeight + bandEnergy * 0.15f + smoothAmplitude * 0.08f + azimuthalRipple + touchEnergy * 0.06f)

          val x0 = n.nx * radialPulse
          val y0 = n.ny * radialPulse
          val z0 = n.nz * radialPulse

          val x1 = x0 * cosY + z0 * sinY
          val z1 = -x0 * sinY + z0 * cosY
          val y1 = y0

          val y2 = y1 * cosP - z1 * sinP
          val z2 = y1 * sinP + z1 * cosP
          val x2 = x1

          val x3 = x2 * cosR - y2 * sinR
          val y3 = x2 * sinR + y2 * cosR
          val z3 = z2

          val persp = cameraZ / (cameraZ - z3.coerceIn(-1.25f, 1.25f))
          var sx = center.x + x3 * sphereRadius * persp
          var sy = center.y + y3 * sphereRadius * persp

          var touchFlare = 0f
          if (activePointers.isNotEmpty() && z3 > -0.35f) {
            val radiusSq = (sphereRadius * sphereRadius * 0.45f).coerceAtLeast(100f)
            for (pointer in activePointers) {
              val dx = sx - pointer.x
              val dy = sy - pointer.y
              val distNorm = (dx * dx + dy * dy) / radiusSq
              val localFlare = exp(-distNorm * 1.5f)
              touchFlare = (touchFlare + localFlare).coerceAtMost(1.3f)
              val push = localFlare * 10.dp.toPx()
              val len = sqrt((dx * dx + dy * dy).coerceAtLeast(1f))
              sx += (dx / len) * push
              sy += (dy / len) * push
            }
          }

          if (shockWaveR < 0.99f) {
            val radialDist =
              sqrt((sx - center.x) * (sx - center.x) + (sy - center.y) * (sy - center.y)) /
                sphereRadius.coerceAtLeast(1f)
            val waveBand =
              1f - (abs(radialDist - shockWaveR * 1.15f) * 3.8f).coerceIn(0f, 1f)
            touchFlare = (touchFlare + waveBand * (1f - shockWaveR)).coerceIn(0f, 1.3f)
          }

          val depthNorm = ((z3 + 1.15f) / 2.30f).coerceIn(0f, 1f)
          val perspectiveDotScale = 0.55f + depthNorm * 1.15f
          val dotRadiusPx =
            ((1.35f + n.shell * 0.48f) * perspectiveDotScale * (1f + beatAmp * 0.30f + touchFlare * 0.55f))
              .dp
              .toPx()
          val alpha = ((0.14f + depthNorm * depthNorm * 0.84f + touchFlare * 0.22f) * bbLatticeAlpha).coerceIn(0f, 1f)

          projected[i] =
            ProjectedParticle3D(
              screenX = sx,
              screenY = sy,
              depthZ = z3,
              radiusPx = dotRadiusPx,
              alpha = alpha,
              energy = (bandEnergy * 0.72f + beatAmp * 0.55f + touchFlare).coerceIn(0f, 1.2f),
              shell = n.shell,
            )
          drawOrder[i] = i
        }

        for (i in 1 until drawOrder.size) {
          val key = drawOrder[i]
          val keyZ = projected[key].depthZ
          var j = i - 1
          while (j >= 0 && projected[drawOrder[j]].depthZ > keyZ) {
            drawOrder[j + 1] = drawOrder[j]
            j--
          }
          drawOrder[j + 1] = key
        }

        for (idx in drawOrder) {
          val p = projected[idx]
          if (p.depthZ >= 0f) break
          if (p.alpha <= 0.01f) continue
          drawCircle(
            color = backParticleColor.copy(alpha = p.alpha),
            radius = p.radiusPx,
            center = Offset(p.screenX, p.screenY),
          )
        }

        val coreCenter = center - Offset(sphereRadius * 0.22f, sphereRadius * 0.22f)
        val coreGlowAlpha = if (cosmicState.phase == CosmicSpherePhase.SWELLING) 1f + 2.5f * cosmicState.swellProgress else 1f
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  frontParticleColor.copy(alpha = ((0.14f + beatAmp * 0.10f + touchEnergy * 0.08f) * coreGlowAlpha).coerceIn(0f, 0.85f)),
                  volumetricGlowColor.copy(alpha = ((0.18f + beatAmp * 0.12f + touchEnergy * 0.08f) * coreGlowAlpha).coerceIn(0f, 0.85f)),
                  Color.Transparent,
                ),
              center = coreCenter,
              radius = sphereRadius * (0.98f + beatAmp * 0.12f),
            ),
          radius = sphereRadius * (0.98f + beatAmp * 0.12f),
          center = center,
        )

        val baseStroke = (0.72f + beatAmp * 0.42f).dp.toPx()
        for ((aIdx, bIdx) in edges) {
          val pa = projected[aIdx]
          val pb = projected[bIdx]
          val avgZ = (pa.depthZ + pb.depthZ) * 0.5f
          val depthFactor = ((avgZ + 1.15f) / 2.30f).coerceIn(0f, 1f)
          val lineAlpha =
            ((0.05f + depthFactor * depthFactor * 0.44f + beatAmp * 0.14f * depthFactor + touchEnergy * 0.12f * depthFactor) * bbLatticeAlpha)
              .coerceIn(0f, 0.65f)
          if (lineAlpha > 0.005f) {
            drawLine(
              color = latticeLineColor.copy(alpha = lineAlpha),
              start = Offset(pa.screenX, pa.screenY),
              end = Offset(pb.screenX, pb.screenY),
              strokeWidth = baseStroke * (0.65f + depthFactor * 0.65f),
              cap = StrokeCap.Round,
            )
          }
        }

        for (idx in drawOrder) {
          val p = projected[idx]
          if (p.depthZ < 0f) continue
          if (p.alpha <= 0.01f) continue
          val nodePos = Offset(p.screenX, p.screenY)
          val frontRatio = (p.depthZ / 1.1f).coerceIn(0f, 1f)
          val dotColor = if (frontRatio > 0.35f || p.energy > 0.65f) frontParticleColor else midParticleColor

          if (frontRatio > 0.25f) {
            drawCircle(
              color = dotColor.copy(alpha = p.alpha * 0.26f),
              radius = p.radiusPx * 2.1f,
              center = nodePos,
            )
          }
          drawCircle(
            color = dotColor.copy(alpha = p.alpha),
            radius = p.radiusPx,
            center = nodePos,
          )
        }
      }

      // Burst explosion shockwave flash in sphere center
      if (isCosmicTheme && cosmicState.phase == CosmicSpherePhase.BURST) {
        val flashAlpha = 0.95f
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color.White.copy(alpha = flashAlpha),
                  Color(0xFF8BB0FF).copy(alpha = flashAlpha * 0.70f),
                  Color.Transparent,
                ),
              center = center,
              radius = size.minDimension * 0.46f,
            ),
          radius = size.minDimension * 0.46f,
          center = center,
        )
      }
    }

    if (!isCosmicTheme) {
      Text(
        text = strings.visualizerTouchHint,
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary,
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 2.dp)
            .testTag("visualizer_3d_sphere_hint"),
      )
    }
  }
}

/**
 * Screen-wide celestial starfall overlay showering sparkling stars across the entire music player screen
 * like snow for 7 seconds, before drawing them into an accelerating cosmic vortex and re-merging cleanly.
 */
@Composable
fun CosmicPlayerScreenStarfallOverlay(
  state: CosmicStarfallState,
  modifier: Modifier = Modifier,
) {
  val seeds =
    remember {
      val list = ArrayList<CosmicSnowStarSeed>(240)
      var seed = 0x738A7Eu
      for (i in 0 until 240) {
        seed = seed * 1664525u + 1013904223u
        val sx = (((seed shr 16) and 0xFFFFu).toFloat() / 65535f)
        seed = seed * 1664525u + 1013904223u
        val sy = (((seed shr 16) and 0xFFFFu).toFloat() / 65535f)
        seed = seed * 1664525u + 1013904223u
        val fallSpeed = 90f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 160f
        seed = seed * 1664525u + 1013904223u
        val swayAmp = 12f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 24f
        seed = seed * 1664525u + 1013904223u
        val swayFreq = 1.4f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 2.2f
        seed = seed * 1664525u + 1013904223u
        val swayPhase = (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * (2f * PI).toFloat()
        seed = seed * 1664525u + 1013904223u
        val rIdx = (seed shr 16) and 0x7u
        val radiusDp =
          when (rIdx.toInt()) {
            0 -> 1.0f
            1 -> 1.3f
            2 -> 1.6f
            3 -> 2.0f
            4 -> 2.4f
            5 -> 2.8f
            else -> 1.5f
          }
        seed = seed * 1664525u + 1013904223u
        val baseAlpha = 0.55f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.43f
        seed = seed * 1664525u + 1013904223u
        val tintType = ((seed shr 16) and 0x3u).toInt()
        val isDiamond = (i % 7 == 0) && radiusDp >= 1.6f
        seed = seed * 1664525u + 1013904223u
        val twinkleSpeed = 2.5f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 3.5f
        seed = seed * 1664525u + 1013904223u
        val twinklePhase = (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * (2f * PI).toFloat()
        val burstAngle = (i.toFloat() / 240f) * (2f * PI).toFloat() + (((seed shr 8) and 0xFFu).toFloat() / 255f) * 0.4f
        seed = seed * 1664525u + 1013904223u
        val burstDistScale = 0.35f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.95f
        list.add(
          CosmicSnowStarSeed(
            seedX = sx,
            seedY = sy,
            fallSpeedDp = fallSpeed,
            swayAmpDp = swayAmp,
            swayFreq = swayFreq,
            swayPhase = swayPhase,
            radiusDp = radiusDp,
            baseAlpha = baseAlpha,
            tintType = tintType,
            isDiamond = isDiamond,
            twinkleSpeed = twinkleSpeed,
            twinklePhase = twinklePhase,
            burstAngle = burstAngle,
            burstDistanceScale = burstDistScale,
          )
        )
      }
      list
    }

  Canvas(modifier = modifier.testTag("cosmic_player_starfall_overlay")) {
    val center = state.sphereCenterInScreen ?: Offset(size.width / 2f, size.height * 0.35f)
    val width = size.width
    val height = size.height

    when (state.phase) {
      CosmicSpherePhase.BURST -> {
        // Flash core explosion & expanding shockwaves
        val flashAlpha = 0.92f
        drawCircle(
          brush =
            Brush.radialGradient(
              listOf(
                Color.White.copy(alpha = flashAlpha),
                Color(0xFF8BB0FF).copy(alpha = flashAlpha * 0.75f),
                Color.Transparent,
              ),
              center = center,
              radius = size.minDimension * 0.42f,
            ),
          radius = size.minDimension * 0.42f,
          center = center,
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.85f),
          radius = size.minDimension * 0.48f,
          center = center,
          style = Stroke(width = 3.dp.toPx()),
        )
        // Draw burst particles radiating out
        seeds.forEach { star ->
          val dist = star.burstDistanceScale * size.minDimension * 0.55f
          val px = center.x + dist * cos(star.burstAngle)
          val py = center.y + dist * sin(star.burstAngle)
          drawCircle(
            color = Color.White.copy(alpha = star.baseAlpha),
            radius = star.radiusDp.dp.toPx(),
            center = Offset(px, py),
          )
        }
      }

      CosmicSpherePhase.STARFALL -> {
        // 7 seconds of sparkling snow-like starfall showering across the entire music player screen
        val tSec = state.starfallProgress * 7.0f
        seeds.forEachIndexed { _, star ->
          val totalFallPx = star.fallSpeedDp.dp.toPx() * tSec
          val rawY = (star.seedY * height + totalFallPx)
          val py = ((rawY % height) + height) % height

          val swayPx = star.swayAmpDp.dp.toPx() * sin(star.swayFreq * tSec + star.swayPhase)
          val rawX = star.seedX * width + swayPx
          val px = ((rawX % width) + width) % width

          val twinkleFactor = 0.78f + 0.22f * sin(tSec * star.twinkleSpeed + star.twinklePhase)
          val alpha = (star.baseAlpha * twinkleFactor).coerceIn(0.35f, 1f)
          val color =
            when (star.tintType) {
              1 -> Color(0xFFE8F1FF).copy(alpha = alpha)
              2 -> Color(0xFFCEE2FF).copy(alpha = alpha)
              3 -> Color(0xFFFFF6E5).copy(alpha = alpha)
              else -> Color.White.copy(alpha = alpha)
            }

          val radiusPx = star.radiusDp.dp.toPx()

          // Soft outer starlight halo
          drawCircle(
            color = color.copy(alpha = alpha * 0.35f),
            radius = radiusPx * 2.2f,
            center = Offset(px, py),
          )
          // Crisp core
          drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(px, py),
          )

          // 4-point diamond sparkle glint
          if (star.isDiamond && alpha > 0.65f) {
            val crossLen = radiusPx * 2.6f
            val crossStroke = 0.85.dp.toPx()
            val glintColor = color.copy(alpha = alpha * 0.75f)
            drawLine(
              color = glintColor,
              start = Offset(px - crossLen, py),
              end = Offset(px + crossLen, py),
              strokeWidth = crossStroke,
              cap = StrokeCap.Round,
            )
            drawLine(
              color = glintColor,
              start = Offset(px, py - crossLen),
              end = Offset(px, py + crossLen),
              strokeWidth = crossStroke,
              cap = StrokeCap.Round,
            )
          }
        }
      }

      CosmicSpherePhase.REMERGE -> {
        // Accelerating orbital vortex: all stars spin with accelerating speed and merge into sphere center
        val p = state.remergeProgress // 0f..1f
        val tSecAtEnd = 7.0f
        val acceleratingAngle = (p.pow(2.6f)) * (14f * PI.toFloat())
        val radiusCollapse = (1f - p.pow(1.8f)).coerceIn(0f, 1f)

        seeds.forEachIndexed { i, star ->
          // Calculate where this star was at the end of the 7s snowfall
          val totalFallPx = star.fallSpeedDp.dp.toPx() * tSecAtEnd
          val rawY = (star.seedY * height + totalFallPx)
          val pyEnd = ((rawY % height) + height) % height

          val swayPx = star.swayAmpDp.dp.toPx() * sin(star.swayFreq * tSecAtEnd + star.swayPhase)
          val rawX = star.seedX * width + swayPx
          val pxEnd = ((rawX % width) + width) % width

          val dx = pxEnd - center.x
          val dy = pyEnd - center.y
          val r0 = hypot(dx, dy)
          val theta0 = atan2(dy, dx)

          val currentAngle = theta0 + acceleratingAngle * (0.85f + (i % 5) * 0.08f)
          val currentRadius = r0 * radiusCollapse
          val px = center.x + currentRadius * cos(currentAngle)
          val py = center.y + currentRadius * sin(currentAngle)

          val alpha = (star.baseAlpha * (1f - p * 0.45f)).coerceIn(0.2f, 1f)
          val color =
            when (star.tintType) {
              1 -> Color(0xFFE8F1FF).copy(alpha = alpha)
              2 -> Color(0xFFCEE2FF).copy(alpha = alpha)
              3 -> Color(0xFFFFF6E5).copy(alpha = alpha)
              else -> Color.White.copy(alpha = alpha)
            }
          val radiusPx = (star.radiusDp * (1f + p * 0.4f)).dp.toPx()

          drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(px, py),
          )
        }

        // Central starlight vortex flare as stars re-merge into core
        if (p > 0.65f) {
          val flareP = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)
          val flareRadius = size.minDimension * 0.36f * (0.4f + flareP * 0.6f)
          val flareAlpha = sin(flareP * PI.toFloat()) * 0.85f
          drawCircle(
            brush =
              Brush.radialGradient(
                listOf(
                  Color.White.copy(alpha = flareAlpha),
                  Color(0xFF8BB0FF).copy(alpha = flareAlpha * 0.65f),
                  Color.Transparent,
                ),
                center = center,
                radius = flareRadius,
              ),
            radius = flareRadius,
            center = center,
          )
        }
      }

      CosmicSpherePhase.IDLE, CosmicSpherePhase.SWELLING -> {
        // In IDLE or SWELLING, screen-wide overlay has nothing to draw
      }
    }
  }
}

private data class CosmicSnowStarSeed(
  val seedX: Float,
  val seedY: Float,
  val fallSpeedDp: Float,
  val swayAmpDp: Float,
  val swayFreq: Float,
  val swayPhase: Float,
  val radiusDp: Float,
  val baseAlpha: Float,
  val tintType: Int,
  val isDiamond: Boolean,
  val twinkleSpeed: Float,
  val twinklePhase: Float,
  val burstAngle: Float,
  val burstDistanceScale: Float,
)
