package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class PlayerSkinTheme(
    val id: String,
    val name: String,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val albumFrameColor: Color, // The yellow square in sfondo.jpg
    val textCyanColor: Color,    // Song Name, Artist, Album text
    val textSecondaryColor: Color,
    val playButtonColor: Color,  // The red triangle in sfondo.jpg
    val controlIconsColor: Color,
    val visualizerColor: Color,
    val cardBorderColor: Color
)

object PlayerSkins {
    val ClassicSfondo = PlayerSkinTheme(
        id = "classic_sfondo",
        name = "Classic Rock (Sfondo)",
        backgroundColor = Color(0xFF070709),
        surfaceColor = Color(0xFF111115),
        albumFrameColor = Color(0xFFFFF000), // Vibrant yellow square as in sfondo.jpg
        textCyanColor = Color(0xFF00F0FF),   // Cyan text as in sfondo.jpg
        textSecondaryColor = Color(0xFF8AE8F2),
        playButtonColor = Color(0xFFFF2600), // Red triangle play button as in sfondo.jpg
        controlIconsColor = Color(0xFFE2E2E6),
        visualizerColor = Color(0xFF00F0FF),
        cardBorderColor = Color(0x33FFF000)
    )

    val HeavyMetal = PlayerSkinTheme(
        id = "heavy_metal",
        name = "Heavy Metal Blood",
        backgroundColor = Color(0xFF0A0808),
        surfaceColor = Color(0xFF161212),
        albumFrameColor = Color(0xFFFF334B),
        textCyanColor = Color(0xFFFF6B6B),
        textSecondaryColor = Color(0xFFFFA8A8),
        playButtonColor = Color(0xFFE50914),
        controlIconsColor = Color(0xFFF0E6E6),
        visualizerColor = Color(0xFFFF2A42),
        cardBorderColor = Color(0x44FF334B)
    )

    val ElectricNeon = PlayerSkinTheme(
        id = "electric_neon",
        name = "Electric Neon Purple",
        backgroundColor = Color(0xFF090614),
        surfaceColor = Color(0xFF140E24),
        albumFrameColor = Color(0xFFC77DFF),
        textCyanColor = Color(0xFF00F5D4),
        textSecondaryColor = Color(0xFF7B2CBF),
        playButtonColor = Color(0xFFF72585),
        controlIconsColor = Color(0xFFE0AAFF),
        visualizerColor = Color(0xFF00F5D4),
        cardBorderColor = Color(0x44C77DFF)
    )

    val CyberRock = PlayerSkinTheme(
        id = "cyber_rock",
        name = "Cyber Acid Rock",
        backgroundColor = Color(0xFF060B08),
        surfaceColor = Color(0xFF0C1710),
        albumFrameColor = Color(0xFF39FF14),
        textCyanColor = Color(0xFF00FFB2),
        textSecondaryColor = Color(0xFF70E000),
        playButtonColor = Color(0xFFFFCC00),
        controlIconsColor = Color(0xFFCCFF33),
        visualizerColor = Color(0xFF39FF14),
        cardBorderColor = Color(0x4439FF14)
    )

    val GoldenAcoustic = PlayerSkinTheme(
        id = "golden_acoustic",
        name = "Golden Amber",
        backgroundColor = Color(0xFF0C0A06),
        surfaceColor = Color(0xFF1A150D),
        albumFrameColor = Color(0xFFFFC107),
        textCyanColor = Color(0xFFFFD166),
        textSecondaryColor = Color(0xFFE0A96D),
        playButtonColor = Color(0xFFFB8500),
        controlIconsColor = Color(0xFFFFE082),
        visualizerColor = Color(0xFFFFB703),
        cardBorderColor = Color(0x44FFC107)
    )

    val allSkins = listOf(
        ClassicSfondo,
        HeavyMetal,
        ElectricNeon,
        CyberRock,
        GoldenAcoustic
    )
}
