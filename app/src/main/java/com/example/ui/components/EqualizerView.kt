package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.EqualizerBandInfo
import com.example.data.model.VisualizerMode
import com.example.ui.theme.PlayerSkinTheme
import kotlin.math.roundToInt

@Composable
fun EqualizerView(
    bands: List<EqualizerBandInfo>,
    currentPreset: String,
    visualizerBars: FloatArray,
    waveform: FloatArray,
    visualizerMode: VisualizerMode,
    isPlaying: Boolean,
    skin: PlayerSkinTheme,
    onBandChange: (bandIndex: Short, dbLevel: Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onVisualizerModeChange: (VisualizerMode) -> Unit,
    albumArtUri: android.net.Uri? = null,
    visualizerPeaks: FloatArray? = null,
    modifier: Modifier = Modifier
) {
    val presets = listOf("Classic Rock", "Heavy Metal", "Hard Rock", "Bass Boost", "Electronic", "Acoustic", "Vocal Lead", "Flat")
    val verticalScroll = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skin.backgroundColor)
    ) {
        val isWide = maxWidth >= 760.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Header and Brushed Aluminum 10-band Equalizer Panel
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    EqualizerHeaderSection(
                        skin = skin,
                        onPresetSelect = onPresetSelect
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    BrushedAluminumEqualizerPanel(
                        bands = bands,
                        onBandChange = onBandChange
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(skin.albumFrameColor.copy(alpha = 0.25f))
                )

                // Right Column: Visualizer, Presets, Tuning Guide
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    RealTimeVisualizerView(
                        bars = visualizerBars,
                        waveform = waveform,
                        mode = visualizerMode,
                        skin = skin,
                        isPlaying = isPlaying,
                        onModeChange = onVisualizerModeChange,
                        albumArtUri = albumArtUri,
                        peaks = visualizerPeaks
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    EqualizerPresetsSection(
                        presets = presets,
                        currentPreset = currentPreset,
                        skin = skin,
                        onPresetSelect = onPresetSelect
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    EqualizerGuideSection(skin = skin)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(verticalScroll)
                    .padding(16.dp)
            ) {
                EqualizerHeaderSection(
                    skin = skin,
                    onPresetSelect = onPresetSelect
                )
                Spacer(modifier = Modifier.height(14.dp))
                RealTimeVisualizerView(
                    bars = visualizerBars,
                    waveform = waveform,
                    mode = visualizerMode,
                    skin = skin,
                    isPlaying = isPlaying,
                    onModeChange = onVisualizerModeChange,
                    albumArtUri = albumArtUri,
                    peaks = visualizerPeaks
                )
                Spacer(modifier = Modifier.height(16.dp))
                EqualizerPresetsSection(
                    presets = presets,
                    currentPreset = currentPreset,
                    skin = skin,
                    onPresetSelect = onPresetSelect
                )
                Spacer(modifier = Modifier.height(18.dp))
                BrushedAluminumEqualizerPanel(
                    bands = bands,
                    onBandChange = onBandChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                EqualizerGuideSection(skin = skin)
            }
        }
    }
}

@Composable
private fun EqualizerHeaderSection(
    skin: PlayerSkinTheme,
    onPresetSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(skin.albumFrameColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = skin.albumFrameColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "10-BAND GRAPHIC EQUALIZER",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Brushed aluminum audiophile faceplate with LED faders",
                    color = skin.textSecondaryColor,
                    fontSize = 11.sp
                )
            }
        }

        OutlinedButton(
            onClick = { onPresetSelect("Flat") },
            shape = RoundedCornerShape(8.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(skin.albumFrameColor.copy(alpha = 0.5f))
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.albumFrameColor),
            modifier = Modifier.testTag("reset_eq_button")
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Flat EQ", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Flat", fontSize = 12.sp)
        }
    }
}

@Composable
private fun EqualizerPresetsSection(
    presets: List<String>,
    currentPreset: String,
    skin: PlayerSkinTheme,
    onPresetSelect: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EQ PRESETS",
                color = skin.textCyanColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            // "Match Photo Setup" Quick Action
            OutlinedButton(
                onClick = { onPresetSelect("Classic Rock") },
                shape = RoundedCornerShape(6.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00E5FF).copy(alpha = 0.6f))
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("photo_setup_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Photo Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("eq_preset_row")
        ) {
            items(presets) { preset ->
                val isSelected = currentPreset == preset || (currentPreset == "Rock" && preset == "Classic Rock") || (currentPreset == "Electronic / Dance" && preset == "Electronic")
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        val key = when (preset) {
                            "Classic Rock" -> "Rock"
                            "Electronic" -> "Electronic / Dance"
                            else -> preset
                        }
                        onPresetSelect(key)
                    },
                    label = {
                        Text(
                            text = preset,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = skin.albumFrameColor,
                        selectedLabelColor = Color.Black,
                        containerColor = skin.surfaceColor,
                        labelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = skin.cardBorderColor,
                        selectedBorderColor = skin.albumFrameColor
                    )
                )
            }
        }
    }
}

@Composable
private fun EqualizerGuideSection(skin: PlayerSkinTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF101014))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "⚡ Graphic EQ Setup: 10 octave bands (31.25Hz to 16kHz). Photo setup boosts 62.5Hz & 125Hz (+7 to +10 dB) for punchy drums, keeps mids at unity (0 dB), and lifts 2k-8k (+5 to +9 dB) for razor-sharp electric rock leads.",
            color = skin.textSecondaryColor,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * High-fidelity representation of the vintage brushed aluminum 10-band graphic equalizer
 * from the uploaded image `grafick equalizer.png`.
 * Includes:
 * - Brushed aluminum faceplate with corner rack screws
 * - 10 Frequency headers: 31.25, 62.5, 125, 250, 500, 1K, 2K, 4K, 8K, 16K
 * - Center dB scale: +12, +6, 0, -6, -12
 * - Left/Right tick lines flanking each vertical slider
 * - Capsule recessed slots
 * - Dark fader knobs with glowing blue LED lenses in the center
 */
@Composable
fun BrushedAluminumEqualizerPanel(
    bands: List<EqualizerBandInfo>,
    onBandChange: (bandIndex: Short, dbLevel: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()

    // 10 bands default fallback if list is shorter
    val safeBands = remember(bands) {
        if (bands.size >= 10) {
            bands.take(10)
        } else {
            val defaultFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
            val defaultLabels = listOf("31.25", "62.5", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
            val defaultDbs = listOf(0, 7, 10, 9, 0, 0, 5, 9, 5, 0)
            (0 until 10).map { i ->
                bands.getOrNull(i) ?: EqualizerBandInfo(
                    bandIndex = i.toShort(),
                    centerFreqHz = defaultFreqs[i],
                    levelDb = defaultDbs[i],
                    freqLabel = defaultLabels[i]
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFF8E939D), RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE4E7EE),
                        Color(0xFFD5D8E0),
                        Color(0xFFE0E3EB),
                        Color(0xFFCBCFD7),
                        Color(0xFFDFE2EA)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .drawBehind {
                drawBrushedAluminumTexture()
                drawRackScrews()
            }
            .padding(horizontal = 8.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Real-Time Frequency Response Curve Display (Continuous Spline with 0dB Unity Grid)
            RealTimeEqCurveDisplay(
                bands = safeBands,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal scroll container ensures 10 bands and center scale fit comfortably on any screen width
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val minFaceplateWidth = 400.dp
                val useScroll = maxWidth < minFaceplateWidth

                Row(
                    modifier = if (useScroll) {
                        Modifier
                            .width(minFaceplateWidth)
                            .horizontalScroll(hScroll)
                            .padding(horizontal = 4.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left 5 bands: 31.25, 62.5, 125, 250, 500
                    safeBands.take(5).forEach { band ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            VintageGraphicFaderColumn(
                                band = band,
                                onValueChange = { newDb -> onBandChange(band.bandIndex, newDb) }
                            )
                        }
                    }

                    // Center dB Scale (+12, +6, 0, -6, -12) positioned exactly between 500 and 1K
                    Box(
                        modifier = Modifier.width(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CenterScaleColumn()
                    }

                    // Right 5 bands: 1K, 2K, 4K, 8K, 16K
                    safeBands.drop(5).take(5).forEach { band ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            VintageGraphicFaderColumn(
                                band = band,
                                onValueChange = { newDb -> onBandChange(band.bandIndex, newDb) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single vertical graphic equalizer band column:
 * - Bold black frequency label at top (31.25, 62.5, etc.)
 * - Deep capsule recessed track slot with tick lines flanking both sides
 * - Draggable fader knob with ribbed metal grip and illuminated LED lens
 * - Zero-lag touch tracking: sub-pixel continuous visual dragging with debounced audio engine updates
 * - Double-tap anywhere to instantly reset to 0 dB unity gain!
 * - Clickable dB readout to quickly toggle / reset to 0 dB
 */
@Composable
private fun VintageGraphicFaderColumn(
    band: EqualizerBandInfo,
    onValueChange: (Int) -> Unit
) {
    val trackHeight = 200.dp
    val density = LocalDensity.current
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val knobHeightPx = with(density) { 22.dp.toPx() }
    val travelRange = (trackHeightPx - knobHeightPx).coerceAtLeast(0f)

    // Smooth continuous touch tracking (eliminates stair-step visual snapping while dragging)
    var isDragging by remember { mutableStateOf(false) }
    var dragYOffset by remember { mutableFloatStateOf(0f) }
    var lastReportedDb by remember(band.levelDb) { mutableStateOf(band.levelDb) }

    val nominalFraction = ((band.levelDb - (-12)) / 24f).coerceIn(0f, 1f)
    val nominalKnobTopPx = (travelRange * (1f - nominalFraction)).coerceAtLeast(0f)
    val currentKnobTopPx = if (isDragging) dragYOffset.coerceIn(0f, travelRange) else nominalKnobTopPx
    val knobTopDp = with(density) { currentKnobTopPx.toDp() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 1.dp)
    ) {
        // 1. Frequency Label at top (bold black sans-serif font)
        Text(
            text = band.displayLabel,
            color = Color(0xFF141416),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(20.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Fader Track Area (Track + Flanking Ticks + Sliding Knob)
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(trackHeight)
                .testTag("eq_fader_${band.displayLabel}")
                .pointerInput(band.bandIndex) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Double-tap instantly zeroes out this band to unity (0 dB)!
                            onValueChange(0)
                        },
                        onTap = { offset ->
                            val clampedY = (offset.y - knobHeightPx / 2f).coerceIn(0f, travelRange)
                            val fraction = 1f - (clampedY / travelRange.coerceAtLeast(1f))
                            val newDb = (-12 + (fraction * 24f)).roundToInt().coerceIn(-12, 12)
                            onValueChange(newDb)
                        }
                    )
                }
                .pointerInput(band.bandIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragYOffset = (offset.y - knobHeightPx / 2f).coerceIn(0f, travelRange)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newY = (dragYOffset + dragAmount.y).coerceIn(0f, travelRange)
                            dragYOffset = newY
                            val fraction = 1f - (newY / travelRange.coerceAtLeast(1f))
                            val newDb = (-12 + (fraction * 24f)).roundToInt().coerceIn(-12, 12)
                            if (newDb != lastReportedDb) {
                                lastReportedDb = newDb
                                onValueChange(newDb)
                            }
                        }
                    )
                }
        ) {
            // Background Canvas: Ticks and Recessed Slot
            Canvas(modifier = Modifier.fillMaxSize()) {
                val slotWidth = 10.dp.toPx()
                val centerX = size.width / 2f
                val slotLeft = centerX - (slotWidth / 2f)
                val cornerRadius = CornerRadius(slotWidth / 2f, slotWidth / 2f)

                // Draw Horizontal Tick Marks flanking the slot on both sides
                val tickSteps = listOf(
                    1.000f to true,  // +12
                    0.875f to false, // +9
                    0.750f to true,  // +6
                    0.625f to false, // +3
                    0.500f to true,  // 0 (Center Unity)
                    0.375f to false, // -3
                    0.250f to true,  // -6
                    0.125f to false, // -9
                    0.000f to true   // -12
                )

                tickSteps.forEach { (fraction, isMajor) ->
                    val tickY = size.height * (1f - fraction)
                    val tickLength = if (fraction == 0.5f) 7.dp.toPx() else if (isMajor) 5.5.dp.toPx() else 3.5.dp.toPx()
                    val strokeWidth = if (fraction == 0.5f) 2.dp.toPx() else if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    val tickColor = Color(0xFF1E2024)

                    // Left tick
                    drawLine(
                        color = tickColor,
                        start = Offset(slotLeft - 2.dp.toPx() - tickLength, tickY),
                        end = Offset(slotLeft - 2.dp.toPx(), tickY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )

                    // Right tick
                    val slotRight = centerX + (slotWidth / 2f)
                    drawLine(
                        color = tickColor,
                        start = Offset(slotRight + 2.dp.toPx(), tickY),
                        end = Offset(slotRight + 2.dp.toPx() + tickLength, tickY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Square
                    )
                }

                // Recessed Capsule Slot (Deep dark groove cut into the faceplate)
                drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(slotLeft - 1f, -1f),
                    size = Size(slotWidth + 2f, size.height + 2f),
                    cornerRadius = cornerRadius
                )

                // Dark slot interior
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0E),
                            Color(0xFF141418),
                            Color(0xFF0A0A0E)
                        )
                    ),
                    topLeft = Offset(slotLeft, 0f),
                    size = Size(slotWidth, size.height),
                    cornerRadius = cornerRadius
                )

                // Center guide line inside slot
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(centerX, 8.dp.toPx()),
                    end = Offset(centerX, size.height - 8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draggable Knob with Glowing LED Jewel
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = knobTopDp)
                    .size(width = 28.dp, height = 22.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF32343A),
                                Color(0xFF1C1D22),
                                Color(0xFF121316),
                                Color(0xFF282A30)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF4A4E58), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Top and Bottom Tactile Grip Ridges
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(3.dp.toPx(), 2.5.dp.toPx()),
                        end = Offset(size.width - 3.dp.toPx(), 2.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(3.dp.toPx(), size.height - 2.5.dp.toPx()),
                        end = Offset(size.width - 3.dp.toPx(), size.height - 2.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Illuminated LED Jewel / Lens
                val ledCoreColor = when {
                    band.levelDb > 0 -> Color(0xFFE0F7FA) // Glowing cyan core for boost
                    band.levelDb < 0 -> Color(0xFFFFE0B2) // Warm amber core for cut
                    else -> Color(0xFFE1F5FE)             // Ice blue for unity
                }
                val ledGlowColor = when {
                    band.levelDb > 0 -> Color(0xFF00E5FF)
                    band.levelDb < 0 -> Color(0xFFFF9100)
                    else -> Color(0xFF40C4FF)
                }
                val ledBezelColor = when {
                    band.levelDb > 0 -> Color(0xFF0288D1)
                    band.levelDb < 0 -> Color(0xFFE65100)
                    else -> Color(0xFF007799)
                }

                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ledCoreColor,
                                    ledGlowColor,
                                    ledBezelColor,
                                    Color(0xFF0A0C10)
                                ),
                                center = Offset(11.dp.value * 0.5f, 13.dp.value * 0.5f)
                            )
                        )
                        .border(1.dp, ledGlowColor.copy(alpha = 0.8f), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Current Level dB Readout (Tap to reset to 0dB!)
        val dbText = if (band.levelDb > 0) "+${band.levelDb}" else "${band.levelDb}"
        val dbColor = when {
            band.levelDb > 0 -> Color(0xFF007799)
            band.levelDb < 0 -> Color(0xFFB71C1C)
            else -> Color(0xFF333333)
        }

        Text(
            text = dbText,
            color = dbColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .clickable { onValueChange(0) }
                .padding(horizontal = 2.dp, vertical = 1.dp)
        )

        // Precision 1 dB Step Stepper Buttons (- / +)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 15.dp, height = 15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF23252B))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(2.dp))
                    .clickable { onValueChange((band.levelDb - 1).coerceAtLeast(-12)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(width = 15.dp, height = 15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF23252B))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(2.dp))
                    .clickable { onValueChange((band.levelDb + 1).coerceAtMost(12)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Real-Time Continuous Frequency Response Spline Display.
 * Calculates and visualizes the exact interpolated acoustic EQ curve across the 10 bands.
 */
@Composable
fun RealTimeEqCurveDisplay(
    bands: List<EqualizerBandInfo>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0B0E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val zeroY = h / 2f

            // Reference lines (+12dB, 0dB center unity line, -12dB)
            drawLine(
                color = Color(0x14FFFFFF),
                start = Offset(0f, 4.dp.toPx()),
                end = Offset(w, 4.dp.toPx()),
                strokeWidth = 1f
            )
            // Center 0 dB unity gain dashed/subtle line
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, zeroY),
                end = Offset(w, zeroY),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color(0x14FFFFFF),
                start = Offset(0f, h - 4.dp.toPx()),
                end = Offset(w, h - 4.dp.toPx()),
                strokeWidth = 1f
            )

            if (bands.isEmpty()) return@Canvas

            val count = bands.size
            val points = ArrayList<Offset>(count)
            val paddingX = 12.dp.toPx()
            val usableW = w - paddingX * 2

            for (i in bands.indices) {
                val x = paddingX + (i.toFloat() / (count - 1).coerceAtLeast(1)) * usableW
                val db = bands[i].levelDb.coerceIn(-12, 12)
                val y = zeroY - (db / 12f) * (zeroY - 6.dp.toPx())
                points.add(Offset(x, y))
            }

            // Smooth cubic spline
            val path = Path()
            val fillPath = Path()
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, zeroY)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
            fillPath.lineTo(points.last().x, zeroY)
            fillPath.close()

            // Translucent glowing gradient fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.22f),
                        Color(0xFF00E5FF).copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )

            // Multi-color neon response trace
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF69F0AE),
                        Color(0xFFFFD54F),
                        Color(0xFFFF5252)
                    )
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Band anchor dots
            points.forEachIndexed { idx, pt ->
                val level = bands[idx].levelDb
                val dotColor = when {
                    level > 0 -> Color(0xFF00E5FF)
                    level < 0 -> Color(0xFFFF9100)
                    else -> Color.White.copy(alpha = 0.75f)
                }
                drawCircle(color = dotColor, radius = 2.5.dp.toPx(), center = pt)
            }
        }

        // dB indicators on right margin
        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("+12", color = Color(0x55FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(" 0dB", color = Color(0x8800E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("-12", color = Color(0x55FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

/**
 * Center scale column displaying +12, +6, 0, -6, -12 aligned with the fader ticks.
 */
@Composable
private fun CenterScaleColumn() {
    val trackHeight = 200.dp
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        // Blank top space matching frequency labels
        Spacer(modifier = Modifier.height(26.dp))

        // Vertical column containing +12, +6, 0, -6, -12
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(trackHeight)
        ) {
            val steps = listOf(
                1.00f to "+12",
                0.75f to "+6",
                0.50f to "0",
                0.25f to "-6",
                0.00f to "-12"
            )

            steps.forEach { (fraction, label) ->
                val topPercent = 1f - fraction
                val verticalOffset = ((200.dp * topPercent) - 7.dp).coerceIn(0.dp, 186.dp)

                Text(
                    text = label,
                    color = Color(0xFF101014),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = verticalOffset)
                )
            }
        }

        // Bottom space matching dB readout
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Realistic fine brushed aluminum texture drawn across the faceplate.
 */
private fun DrawScope.drawBrushedAluminumTexture() {
    val step = 10.dp.toPx()
    val numLines = (size.height / step).toInt()

    for (i in 0..numLines) {
        val y = i * step
        drawLine(
            color = Color(0x14000000),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0x18FFFFFF),
            start = Offset(0f, y + 1f),
            end = Offset(size.width, y + 1f),
            strokeWidth = 1f
        )
    }

    // Top bevel highlight (metallic shine)
    drawLine(
        color = Color(0x80FFFFFF),
        start = Offset(0f, 1f),
        end = Offset(size.width, 1f),
        strokeWidth = 2f
    )

    // Bottom shadow
    drawLine(
        color = Color(0x55000000),
        start = Offset(0f, size.height - 1f),
        end = Offset(size.width, size.height - 1f),
        strokeWidth = 2f
    )
}

/**
 * 4 Vintage rack screws in the corners for that studio equipment feel.
 */
private fun DrawScope.drawRackScrews() {
    val screwRadius = 5.dp.toPx()
    val margin = 10.dp.toPx()
    val screwCenters = listOf(
        Offset(margin, margin),
        Offset(size.width - margin, margin),
        Offset(margin, size.height - margin),
        Offset(size.width - margin, size.height - margin)
    )

    screwCenters.forEach { center ->
        // Screw head base
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE2E4E8), Color(0xFF9EA3AC), Color(0xFF63676E)),
                center = center,
                radius = screwRadius
            ),
            radius = screwRadius,
            center = center
        )
        // Outer dark bezel
        drawCircle(
            color = Color(0xFF3B3E45),
            radius = screwRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        // Screw cross slot
        val slotLen = screwRadius * 0.6f
        drawLine(
            color = Color(0xFF28292E),
            start = Offset(center.x - slotLen, center.y),
            end = Offset(center.x + slotLen, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFF28292E),
            start = Offset(center.x, center.y - slotLen),
            end = Offset(center.x, center.y + slotLen),
            strokeWidth = 1.5f
        )
    }
}
