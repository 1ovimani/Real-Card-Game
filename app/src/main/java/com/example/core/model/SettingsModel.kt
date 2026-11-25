package com.example.core.model

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val tableThemeId: String = "classic_green",
    val cardBackId: String = CardBackStyle.ROYAL_FILIGREE.id,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val batterySaver: Boolean = false,
    val particleEffects: Boolean = true,
    val shadowQualityHigh: Boolean = true,
    val masterVolume: Float = 0.85f,
    val musicVolume: Float = 0.35f,
    val sfxVolume: Float = 0.90f,
    val muteAll: Boolean = false,
    val musicEnabled: Boolean = true,
    val musicMood: MusicMood = MusicMood.CASINO_AMBIENCE,
    val cardSoundsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSortCards: Boolean = true,
    val animationSpeed: Float = 1.0f, // 0.6 = slow, 1.0 = normal, 1.5 = fast
    val dragToPlay: Boolean = false,
    val confirmBeforeQuit: Boolean = true,
    val showHints: Boolean = true,
    val largeCards: Boolean = false,
    val highContrast: Boolean = false,
    val colorBlindSuits: Boolean = false,
    val reducedMotion: Boolean = false,
    val textScale: Float = 1.0f,
    val defaultAiDifficulty: AiDifficulty = AiDifficulty.NORMAL,
    val playerName: String = "Player"
)
