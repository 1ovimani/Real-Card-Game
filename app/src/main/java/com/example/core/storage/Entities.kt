package com.example.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val slotId: String = "active_slot",
    val gameId: String,
    val ruleProfileId: String,
    val difficultyLevel: Int,
    val stateJson: String,
    val summaryEn: String,
    val summaryBn: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val ruleProfileId: String,
    val difficultyLevel: Int,
    val playerWon: Boolean,
    val scoreSummary: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_games")
data class FavoriteGameEntity(
    @PrimaryKey val gameId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Player",
    val virtualChips: Long = 5000L,
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val lastPlayedGameId: String = "twenty_nine"
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val languageCode: String = "en",
    val tableThemeId: String = "classic_green",
    val cardBackId: String = "royal_filigree",
    val graphicsQualityId: String = "high",
    val batterySaver: Boolean = false,
    val particleEffects: Boolean = true,
    val shadowQualityHigh: Boolean = true,
    val masterVolume: Float = 0.85f,
    val musicVolume: Float = 0.35f,
    val sfxVolume: Float = 0.90f,
    val muteAll: Boolean = false,
    val musicEnabled: Boolean = true,
    val musicMoodId: String = "casino",
    val cardSoundsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSortCards: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val dragToPlay: Boolean = false,
    val confirmBeforeQuit: Boolean = true,
    val showHints: Boolean = true,
    val largeCards: Boolean = false,
    val highContrast: Boolean = false,
    val colorBlindSuits: Boolean = false,
    val reducedMotion: Boolean = false,
    val textScale: Float = 1.0f,
    val defaultAiDifficultyLevel: Int = 3
)
