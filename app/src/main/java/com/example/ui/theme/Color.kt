package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Luxury Salon Brand Palette
val EmeraldDeep = Color(0xFF071E16)
val EmeraldVelvet = Color(0xFF0D3326)
val EmeraldSurface = Color(0xFF123F2F)
val EmeraldElevated = Color(0xFF19513D)

val BurnishedGold = Color(0xFFD8B258)
val ChampagneGold = Color(0xFFF3D98E)
val AntiqueBrass = Color(0xFF9F7D31)

val ObsidianNight = Color(0xFF0A100E)
val ObsidianCard = Color(0xFF131D19)
val ObsidianBorder = Color(0xFF283D34)

val IvoryStock = Color(0xFFFAF6EC)
val IvoryWarm = Color(0xFFF1E9D6)
val TextPrimaryLight = Color(0xFFF7F4EC)
val TextSecondaryLight = Color(0xFFB7C7BF)

val RubyCrimson = Color(0xFFC61E2B)
val OnyxInk = Color(0xFF161922)
val SapphireSuit = Color(0xFF1565C0) // Color-blind friendly Diamonds
val ForestSuit = Color(0xFF1B7A43)   // Color-blind friendly Clubs

val SuccessEmerald = Color(0xFF39C682)
val WarningAmber = Color(0xFFF0B43A)
val ErrorCrimson = Color(0xFFE05252)

// Table Environment Color Sets
data class TablePalette(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val feltCenter: Color,
    val feltMid: Color,
    val feltEdge: Color,
    val railOuter: Color,
    val railAccent: Color,
    val lineGold: Color
)

val TablePalettes = listOf(
    TablePalette(
        id = "classic_green",
        nameEn = "Classic Casino Felt",
        nameBn = "ক্লাসিক সবুজ ক্যাসিনো টেবিল",
        feltCenter = Color(0xFF156347),
        feltMid = Color(0xFF0D4430),
        feltEdge = Color(0xFF06251A),
        railOuter = Color(0xFF261710),
        railAccent = Color(0xFFD8B258),
        lineGold = Color(0x55E9C97B)
    ),
    TablePalette(
        id = "royal_red",
        nameEn = "Royal Red Velvet",
        nameBn = "রয়েল লাল মখমল টেবিল",
        feltCenter = Color(0xFF7B1B26),
        feltMid = Color(0xFF531018),
        feltEdge = Color(0xFF2D070C),
        railOuter = Color(0xFF1E1311),
        railAccent = Color(0xFFE5C169),
        lineGold = Color(0x55F0D284)
    ),
    TablePalette(
        id = "luxury_black",
        nameEn = "Luxury Obsidian",
        nameBn = "লাক্সারি ব্ল্যাক টেবিল",
        feltCenter = Color(0xFF23282D),
        feltMid = Color(0xFF15191D),
        feltEdge = Color(0xFF090B0D),
        railOuter = Color(0xFF0E0F12),
        railAccent = Color(0xFFD8B258),
        lineGold = Color(0x4DD8B258)
    ),
    TablePalette(
        id = "blue_tournament",
        nameEn = "Blue Tournament",
        nameBn = "নীল টুর্নামেন্ট টেবিল",
        feltCenter = Color(0xFF144875),
        feltMid = Color(0xFF0D3052),
        feltEdge = Color(0xFF071A2F),
        railOuter = Color(0xFF141B24),
        railAccent = Color(0xFF8EC5FC),
        lineGold = Color(0x55A7D1FA)
    ),
    TablePalette(
        id = "natural_wood",
        nameEn = "Natural Mahogany",
        nameBn = "প্রাকৃতিক কাঠের টেবিল",
        feltCenter = Color(0xFF6B442A),
        feltMid = Color(0xFF4A2E1B),
        feltEdge = Color(0xFF2C190E),
        railOuter = Color(0xFF1D1008),
        railAccent = Color(0xFFE0B866),
        lineGold = Color(0x55E8C77D)
    ),
    TablePalette(
        id = "dark_gold",
        nameEn = "Dark Gold Sovereign",
        nameBn = "ডার্ক গোল্ড প্রিমিয়াম টেবিল",
        feltCenter = Color(0xFF423618),
        feltMid = Color(0xFF2B220E),
        feltEdge = Color(0xFF141006),
        railOuter = Color(0xFF0F0C05),
        railAccent = Color(0xFFF3D98E),
        lineGold = Color(0x66F5DC93)
    ),
    TablePalette(
        id = "traditional_dhaka",
        nameEn = "Traditional Heritage",
        nameBn = "ঐতিহ্যবাহী বাংলা আসর",
        feltCenter = Color(0xFF14594C),
        feltMid = Color(0xFF0C3C34),
        feltEdge = Color(0xFF07231E),
        railOuter = Color(0xFF331B12),
        railAccent = Color(0xFFE8A948),
        lineGold = Color(0x60F1BE62)
    ),
    TablePalette(
        id = "custom_dusk",
        nameEn = "Midnight Indigo",
        nameBn = "রাতের নীলচে আভা",
        feltCenter = Color(0xFF273158),
        feltMid = Color(0xFF19203B),
        feltEdge = Color(0xFF0C1021),
        railOuter = Color(0xFF12121A),
        railAccent = Color(0xFFE6C068),
        lineGold = Color(0x55D8B258)
    )
)

fun tablePaletteById(id: String): TablePalette =
    TablePalettes.firstOrNull { it.id == id } ?: TablePalettes.first()
