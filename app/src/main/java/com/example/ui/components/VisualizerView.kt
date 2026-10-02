package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.VisualizerMode
import com.example.ui.theme.PlayerSkinTheme
import kotlin.math.cos
import kotlin.math.sin

// Precomputed decibel reference lines for the spectrum bars canvas (0dB top, -6dB, -12dB, -18dB)
private val SPECTRUM_GRID_LINES = floatArrayOf(0.15f, 0.40f, 0.65f, 0.85f)

// Precomputed 16 spoke unit vectors (cos, sin) to eliminate all runtime trigonometry allocations/calculations
private val SPOKE_COS_SIN = Array(16) { k ->
    val angle = (k.toDouble() / 16.0) * 2.0 * Math.PI
    Pair(cos(angle).toFloat(), sin(angle).toFloat())
}

@Composable
fun RealTimeVisualizerView(
    bars: FloatArray,
    waveform: FloatArray,
    mode: VisualizerMode,
    skin: PlayerSkinTheme,
    isPlaying: Boolean,
    onModeChange: (VisualizerMode) -> Unit,
    albumArtUri: Uri? = null,
    peaks: FloatArray? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(skin.surfaceColor)
            .border(1.dp, skin.cardBorderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("visualizer_container")
    ) {
        // Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF00FF66) else Color(0xFFFF5252))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "REAL-TIME SPECTRUM" else "SPECTRUM PAUSED",
                    color = skin.textCyanColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = mode == VisualizerMode.SPECTRUM_BARS,
                    onClick = { onModeChange(VisualizerMode.SPECTRUM_BARS) },
                    label = { Text("Bars", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = skin.albumFrameColor.copy(alpha = 0.3f),
                        selectedLabelColor = skin.albumFrameColor,
                        selectedLeadingIconColor = skin.albumFrameColor,
                        containerColor = Color.Transparent,
                        labelColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("visualizer_mode_bars")
                )

                FilterChip(
                    selected = mode == VisualizerMode.WAVEFORM,
                    onClick = { onModeChange(VisualizerMode.WAVEFORM) },
                    label = { Text("Wave", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = skin.textCyanColor.copy(alpha = 0.3f),
                        selectedLabelColor = skin.textCyanColor,
                        selectedLeadingIconColor = skin.textCyanColor,
                        containerColor = Color.Transparent,
                        labelColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("visualizer_mode_wave")
                )

                FilterChip(
                    selected = mode == VisualizerMode.ROCK_PULSE,
                    onClick = { onModeChange(VisualizerMode.ROCK_PULSE) },
                    label = { Text("Art Pulse", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.SurroundSound, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = skin.playButtonColor.copy(alpha = 0.3f),
                        selectedLabelColor = skin.playButtonColor,
                        selectedLeadingIconColor = skin.playButtonColor,
                        containerColor = Color.Transparent,
                        labelColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("visualizer_mode_pulse")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visualizer Canvas & Artwork Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF070709))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
        ) {
            when (mode) {
                VisualizerMode.SPECTRUM_BARS -> {
                    SpectrumBarsCanvas(bars = bars, peaks = peaks, skin = skin)
                }
                VisualizerMode.WAVEFORM -> {
                    WaveformCanvas(waveform = waveform, skin = skin)
                }
                VisualizerMode.ROCK_PULSE -> {
                    RockPulseCanvas(
                        bars = bars,
                        skin = skin,
                        isPlaying = isPlaying,
                        albumArtUri = albumArtUri
                    )
                }
            }
        }
    }
}

@Composable
private fun SpectrumBarsCanvas(
    bars: FloatArray,
    peaks: FloatArray? = null,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    val barColors = androidx.compose.runtime.remember(skin.playButtonColor, skin.albumFrameColor, skin.visualizerColor) {
        listOf(
            skin.playButtonColor,  // Hot red peak
            skin.albumFrameColor, // Rock yellow
            skin.visualizerColor  // Cyan base
        )
    }

    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        val count = bars.size.coerceAtLeast(1)
        val spacing = 2.5.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2f)

        // Precomputed Studio Decibel Reference Lines (0dB top, -6dB, -12dB, -18dB)
        for (i in SPECTRUM_GRID_LINES.indices) {
            val yPos = size.height * SPECTRUM_GRID_LINES[i]
            drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(0f, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 1f
            )
        }

        // Single vertical gradient brush created once per draw pass instead of per bar
        val barBrush = Brush.verticalGradient(
            colors = barColors,
            startY = 0f,
            endY = size.height
        )

        for (i in bars.indices) {
            val magnitude = bars[i].coerceIn(0.04f, 1f)
            val barHeight = size.height * magnitude
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Precision Floating Peak-Hold Indicator Cap
            val peakMagnitude = (peaks?.getOrNull(i) ?: magnitude).coerceIn(magnitude, 1f)
            val peakY = (size.height - (size.height * peakMagnitude) - 1.5.dp.toPx()).coerceAtLeast(0f)

            // High-visibility illuminated peak cap
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun WaveformCanvas(
    waveform: FloatArray,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    val path = androidx.compose.runtime.remember { Path() }
    val fillPath = androidx.compose.runtime.remember { Path() }

    val fillColors = androidx.compose.runtime.remember(skin.visualizerColor) {
        listOf(
            skin.visualizerColor.copy(alpha = 0.25f),
            Color.Transparent
        )
    }

    val waveColors = androidx.compose.runtime.remember(skin.albumFrameColor, skin.visualizerColor, skin.playButtonColor) {
        listOf(
            skin.albumFrameColor,
            skin.visualizerColor,
            skin.playButtonColor
        )
    }

    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
        if (waveform.isEmpty()) return@Canvas

        val centerY = size.height / 2f
        val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)

        path.reset()
        fillPath.reset()
        fillPath.moveTo(0f, centerY)

        for (i in waveform.indices) {
            val x = i * stepX
            val amp = waveform[i].coerceIn(-1f, 1f)
            val y = centerY + amp * (size.height / 2.2f)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(size.width, centerY)
        fillPath.close()

        // Subtle glow fill with memoized colors
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(colors = fillColors)
        )

        // Wave line with memoized colors
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(colors = waveColors),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Center zero-line
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun RockPulseCanvas(
    bars: FloatArray,
    skin: PlayerSkinTheme,
    isPlaying: Boolean,
    albumArtUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    // Zero-allocation bass energy calculation (first 4 bars)
    val bassEnergy = if (bars.size >= 4) {
        ((bars[0] + bars[1] + bars[2] + bars[3]) / 4f).coerceIn(0.1f, 1f)
    } else if (bars.isNotEmpty()) {
        var sum = 0f
        for (i in bars.indices) sum += bars[i]
        (sum / bars.size).coerceIn(0.1f, 1f)
    } else 0.2f

    val animatedRadius by animateFloatAsState(
        targetValue = if (isPlaying) bassEnergy else 0.15f,
        animationSpec = tween(durationMillis = 80),
        label = "pulse_anim"
    )

    // Continuous vinyl spinning animation when music is actively playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_art_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "spin_rotation"
    )
    val currentRotation = if (isPlaying) spinAngle else 0f

    val context = LocalContext.current

    val fallbackCoreColors = androidx.compose.runtime.remember(skin.albumFrameColor, skin.playButtonColor) {
        listOf(skin.albumFrameColor, skin.playButtonColor)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxR = size.height / 2.1f

            // Outer pulsing rings
            val ring1 = (animatedRadius * maxR).coerceAtLeast(10f)
            val ring2 = (animatedRadius * maxR * 0.7f).coerceAtLeast(8f)
            val ring3 = (animatedRadius * maxR * 0.4f).coerceAtLeast(6f)

            drawCircle(
                color = skin.playButtonColor.copy(alpha = 0.15f),
                radius = ring1,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = skin.albumFrameColor.copy(alpha = 0.25f),
                radius = ring2,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = skin.visualizerColor.copy(alpha = 0.4f),
                radius = ring3,
                center = Offset(centerX, centerY)
            )

            // Fallback central core if no album art URI is present
            if (albumArtUri == null) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = fallbackCoreColors,
                        center = Offset(centerX, centerY),
                        radius = 16.dp.toPx()
                    ),
                    radius = 14.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }

            // Sound radiating spokes using precomputed SPOKE_COS_SIN unit vectors (0 trig calls per frame)
            val innerSpokeRadius = if (albumArtUri != null) 28.dp.toPx() else ring3
            val spokeLenBase = ring2
            for (k in 0 until 16) {
                val cosSin = SPOKE_COS_SIN[k]
                val mag = bars.getOrElse(k % bars.size) { 0.3f }
                val spokeLen = spokeLenBase + (mag * 22.dp.toPx())

                val startX = centerX + (innerSpokeRadius * cosSin.first)
                val startY = centerY + (innerSpokeRadius * cosSin.second)
                val endX = centerX + (spokeLen * cosSin.first)
                val endY = centerY + (spokeLen * cosSin.second)

                drawLine(
                    color = skin.visualizerColor.copy(alpha = 0.8f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center Album Art Vinyl Badge for all picture extensions
        if (albumArtUri != null) {
            val pulsingSize = (44.dp + (animatedRadius * 14).dp).coerceIn(42.dp, 58.dp)

            Box(
                modifier = Modifier
                    .size(pulsingSize)
                    .graphicsLayer(rotationZ = currentRotation)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0F12))
                    .border(2.dp, skin.albumFrameColor, CircleShape)
                    .shadow(elevation = 6.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Album Art Visualizer",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                // Vinyl Center Hole & Ring Accent
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418))
                        .border(1.5.dp, skin.albumFrameColor, CircleShape)
                )
            }
        }
    }
}
