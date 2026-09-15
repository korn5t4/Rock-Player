package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    modifier: Modifier = Modifier
) {
    val presets = listOf("Classic Rock", "Heavy Metal", "Bass Boost", "Acoustic", "Vocal Lead", "Flat")
    val verticalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.backgroundColor)
            .verticalScroll(verticalScroll)
            .padding(16.dp)
    ) {
        // Top Header
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

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Visualizer
        RealTimeVisualizerView(
            bars = visualizerBars,
            waveform = waveform,
            mode = visualizerMode,
            skin = skin,
            isPlaying = isPlaying,
            onModeChange = onVisualizerModeChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Selector
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
                val isSelected = currentPreset == preset || (currentPreset == "Rock" && preset == "Classic Rock")
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        val key = if (preset == "Classic Rock") "Rock" else preset
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

        Spacer(modifier = Modifier.height(18.dp))

        // THE BRUSHED ALUMINUM GRAPHIC EQUALIZER FACEPLATE
        BrushedAluminumEqualizerPanel(
            bands = bands,
            onBandChange = onBandChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Audiophile tuning guide
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

/**
 * A single vertical graphic equalizer band column:
 * - Bold black frequency label at top (31.25, 62.5, etc.)
 * - Deep capsule recessed track slot with tick lines flanking both sides
 * - Draggable fader knob with ribbed metal grip and illuminated blue LED lens
 * - Current dB indicator at bottom
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

    // Live dragging tracking
    var isDragging by remember { mutableStateOf(false) }

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
                .width(34.dp)
                .height(trackHeight)
                .testTag("eq_fader_${band.displayLabel}")
                .pointerInput(band.bandIndex) {
                    detectTapGestures { offset ->
                        val clampedY = offset.y.coerceIn(0f, trackHeightPx)
                        val fraction = 1f - (clampedY / trackHeightPx)
                        val newDb = (-12 + (fraction * 24f)).roundToInt().coerceIn(-12, 12)
                        onValueChange(newDb)
                    }
                }
                .pointerInput(band.bandIndex) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val clampedY = change.position.y.coerceIn(0f, trackHeightPx)
                            val fraction = 1f - (clampedY / trackHeightPx)
                            val newDb = (-12 + (fraction * 24f)).roundToInt().coerceIn(-12, 12)
                            onValueChange(newDb)
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
                    0.500f to true,  // 0 (Center)
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
                // Outer subtle bevel/shadow
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

            // Draggable Knob with Glowing Blue LED Jewel
            val fraction = ((band.levelDb - (-12)) / 24f).coerceIn(0f, 1f)
            // Available travel for knob center
            val travelRange = (trackHeightPx - knobHeightPx).coerceAtLeast(0f)
            val knobTopPx = (travelRange * (1f - fraction)).coerceAtLeast(0f)
            val knobTopDp = with(density) { knobTopPx.toDp() }

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
                    // Top grip lines
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(3.dp.toPx(), 2.5.dp.toPx()),
                        end = Offset(size.width - 3.dp.toPx(), 2.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Bottom grip lines
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(3.dp.toPx(), size.height - 2.5.dp.toPx()),
                        end = Offset(size.width - 3.dp.toPx(), size.height - 2.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Illuminated Blue LED Jewel / Lens (in the center of each knob, exact to photo)
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE0F7FA), // Bright cyan/white LED core
                                    Color(0xFF00E5FF), // Electric cyan
                                    Color(0xFF0288D1), // Deep blue bezel
                                    Color(0xFF004466)  // Outer housing
                                ),
                                center = Offset(11.dp.value * 0.5f, 13.dp.value * 0.5f)
                            )
                        )
                        .border(1.dp, Color(0xFF00B0FF), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Internal glowing LED lens element
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFFFFF).copy(alpha = 0.85f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Current Level dB Readout
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
            textAlign = TextAlign.Center
        )
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
    val hairlineColor1 = Color(0x15000000)
    val hairlineColor2 = Color(0x22FFFFFF)
    val numLines = (size.height / 3f).toInt()

    for (i in 0 until numLines) {
        val y = i * 3f
        val color = if (i % 2 == 0) hairlineColor1 else hairlineColor2
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.75f
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
