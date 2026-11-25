package com.example.core.storage

import com.example.core.model.AiDifficulty
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.GraphicsQuality
import com.example.core.model.MusicMood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorldCardRepository(private val db: AppDatabase) {

    val activeSave: Flow<SavedGameEntity?> = db.savedGameDao().observeActiveSave()
    val matchHistory: Flow<List<MatchHistoryEntity>> = db.matchHistoryDao().observeHistory()
    val favoriteIds: Flow<Set<String>> = db.favoriteGameDao().observeFavoriteIds().map { it.toSet() }
    val playerProfile: Flow<PlayerProfileEntity> = db.playerProfileDao().observeProfile()
        .map { it ?: PlayerProfileEntity() }

    val settings: Flow<AppSettings> = db.appSettingsDao().observeSettings().map { entity ->
        if (entity == null) AppSettings() else entity.toModel()
    }

    suspend fun ensureDefaultsInitialized() {
        if (db.playerProfileDao().getProfile() == null) {
            db.playerProfileDao().upsertProfile(PlayerProfileEntity())
        }
        if (db.appSettingsDao().getSettings() == null) {
            db.appSettingsDao().upsertSettings(AppSettingsEntity())
        }
    }

    suspend fun saveSettings(s: AppSettings) {
        db.appSettingsDao().upsertSettings(s.toEntity())
        val existingProfile = db.playerProfileDao().getProfile() ?: PlayerProfileEntity()
        if (existingProfile.playerName != s.playerName) {
            db.playerProfileDao().upsertProfile(existingProfile.copy(playerName = s.playerName))
        }
    }

    suspend fun toggleFavorite(gameId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            db.favoriteGameDao().removeFavorite(gameId)
        } else {
            db.favoriteGameDao().addFavorite(FavoriteGameEntity(gameId))
        }
    }

    suspend fun saveActiveGame(
        gameId: String,
        ruleProfileId: String,
        difficultyLevel: Int,
        stateJson: String,
        summaryEn: String,
        summaryBn: String
    ) {
        db.savedGameDao().upsertSave(
            SavedGameEntity(
                slotId = "active_slot",
                gameId = gameId,
                ruleProfileId = ruleProfileId,
                difficultyLevel = difficultyLevel,
                stateJson = stateJson,
                summaryEn = summaryEn,
                summaryBn = summaryBn,
                updatedAt = System.currentTimeMillis()
            )
        )
        val profile = db.playerProfileDao().getProfile() ?: PlayerProfileEntity()
        db.playerProfileDao().upsertProfile(profile.copy(lastPlayedGameId = gameId))
    }

    suspend fun getActiveSave(): SavedGameEntity? = db.savedGameDao().getActiveSave()

    suspend fun clearActiveSave() = db.savedGameDao().clearActiveSave()

    suspend fun recordMatchResult(
        gameId: String,
        ruleProfileId: String,
        difficultyLevel: Int,
        playerWon: Boolean,
        scoreSummary: String,
        chipDelta: Long = 0L
    ) {
        db.matchHistoryDao().insertMatch(
            MatchHistoryEntity(
                gameId = gameId,
                ruleProfileId = ruleProfileId,
                difficultyLevel = difficultyLevel,
                playerWon = playerWon,
                scoreSummary = scoreSummary
            )
        )
        val profile = db.playerProfileDao().getProfile() ?: PlayerProfileEntity()
        val updatedChips = (profile.virtualChips + chipDelta).coerceAtLeast(100L)
        db.playerProfileDao().upsertProfile(
            profile.copy(
                virtualChips = updatedChips,
                totalGamesPlayed = profile.totalGamesPlayed + 1,
                totalWins = profile.totalWins + if (playerWon) 1 else 0,
                lastPlayedGameId = gameId
            )
        )
    }

    suspend fun updateVirtualChips(delta: Long) {
        val profile = db.playerProfileDao().getProfile() ?: PlayerProfileEntity()
        val next = (profile.virtualChips + delta).coerceAtLeast(200L)
        db.playerProfileDao().upsertProfile(profile.copy(virtualChips = next))
    }

    private fun AppSettingsEntity.toModel() = AppSettings(
        language = if (languageCode == "bn") AppLanguage.BANGLA else AppLanguage.ENGLISH,
        tableThemeId = tableThemeId,
        cardBackId = cardBackId,
        graphicsQuality = GraphicsQuality.fromId(graphicsQualityId),
        batterySaver = batterySaver,
        particleEffects = particleEffects,
        shadowQualityHigh = shadowQualityHigh,
        masterVolume = masterVolume,
        musicVolume = musicVolume,
        sfxVolume = sfxVolume,
        muteAll = muteAll,
        musicEnabled = musicEnabled,
        musicMood = MusicMood.fromId(musicMoodId),
        cardSoundsEnabled = cardSoundsEnabled,
        vibrationEnabled = vibrationEnabled,
        autoSortCards = autoSortCards,
        animationSpeed = animationSpeed,
        dragToPlay = dragToPlay,
        confirmBeforeQuit = confirmBeforeQuit,
        showHints = showHints,
        largeCards = largeCards,
        highContrast = highContrast,
        colorBlindSuits = colorBlindSuits,
        reducedMotion = reducedMotion,
        textScale = textScale,
        defaultAiDifficulty = AiDifficulty.fromLevel(defaultAiDifficultyLevel)
    )

    private fun AppSettings.toEntity() = AppSettingsEntity(
        id = 1,
        languageCode = language.code,
        tableThemeId = tableThemeId,
        cardBackId = cardBackId,
        graphicsQualityId = graphicsQuality.id,
        batterySaver = batterySaver,
        particleEffects = particleEffects,
        shadowQualityHigh = shadowQualityHigh,
        masterVolume = masterVolume,
        musicVolume = musicVolume,
        sfxVolume = sfxVolume,
        muteAll = muteAll,
        musicEnabled = musicEnabled,
        musicMoodId = musicMood.id,
        cardSoundsEnabled = cardSoundsEnabled,
        vibrationEnabled = vibrationEnabled,
        autoSortCards = autoSortCards,
        animationSpeed = animationSpeed,
        dragToPlay = dragToPlay,
        confirmBeforeQuit = confirmBeforeQuit,
        showHints = showHints,
        largeCards = largeCards,
        highContrast = highContrast,
        colorBlindSuits = colorBlindSuits,
        reducedMotion = reducedMotion,
        textScale = textScale,
        defaultAiDifficultyLevel = defaultAiDifficulty.level
    )
}
