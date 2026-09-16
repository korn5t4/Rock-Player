package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VisualizerMode
import com.example.ui.theme.PlayerSkinTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RealTimeVisualizerView(
    bars: FloatArray,
    waveform: FloatArray,
    mode: VisualizerMode,
    skin: PlayerSkinTheme,
    isPlaying: Boolean,
    onModeChange: (VisualizerMode) -> Unit,
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
                    label = { Text("Pulse", fontSize = 10.sp) },
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

        // Visualizer Canvas
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
                    SpectrumBarsCanvas(bars = bars, skin = skin)
                }
                VisualizerMode.WAVEFORM -> {
                    WaveformCanvas(waveform = waveform, skin = skin)
                }
                VisualizerMode.ROCK_PULSE -> {
                    RockPulseCanvas(bars = bars, skin = skin, isPlaying = isPlaying)
                }
            }
        }
    }
}

@Composable
private fun SpectrumBarsCanvas(
    bars: FloatArray,
    skin: PlayerSkinTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        val count = bars.size.coerceAtLeast(1)
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2f)

        for (i in bars.indices) {
            val magnitude = bars[i].coerceIn(0.04f, 1f)
            val barHeight = size.height * magnitude
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            // Gradient: Bottom cyan -> Mid Yellow -> Top Red (Rock Heat Spectrum!)
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    skin.playButtonColor,   // Hot red peak
                    skin.albumFrameColor,  // Rock yellow
                    skin.visualizerColor   // Cyan base
                ),
                startY = y,
                endY = size.height
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Floating peak cap dot
            val peakY = (y - 3.dp.toPx()).coerceAtLeast(0f)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = (barWidth / 2.5f).coerceIn(1.5f, 3.5f),
                center = Offset(x + barWidth / 2f, peakY)
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

        // Subtle glow fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    skin.visualizerColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )

        // Wave line
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    skin.albumFrameColor,
                    skin.visualizerColor,
                    skin.playButtonColor
                )
            ),
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
    modifier: Modifier = Modifier
) {
    // Average bass energy (first 4 bars)
    val bassEnergy = if (bars.isNotEmpty()) {
        (bars.take(4).sum() / 4f).coerceIn(0.1f, 1f)
    } else 0.2f

    val animatedRadius by animateFloatAsState(
        targetValue = if (isPlaying) bassEnergy else 0.15f,
        animationSpec = tween(durationMillis = 80),
        label = "pulse_anim"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
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

        // Central core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skin.albumFrameColor, skin.playButtonColor),
                center = Offset(centerX, centerY),
                radius = 16.dp.toPx()
            ),
            radius = 14.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        // Sound radiating spokes
        val spokeCount = 16
        for (k in 0 until spokeCount) {
            val angle = (k.toDouble() / spokeCount) * 2 * Math.PI
            val mag = bars.getOrElse(k % bars.size) { 0.3f }
            val spokeLen = ring2 + (mag * 20.dp.toPx())

            val startX = centerX + (ring3 * cos(angle)).toFloat()
            val startY = centerY + (ring3 * sin(angle)).toFloat()
            val endX = centerX + (spokeLen * cos(angle)).toFloat()
            val endY = centerY + (spokeLen * sin(angle)).toFloat()

            drawLine(
                color = skin.visualizerColor.copy(alpha = 0.8f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
