package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.CardAudioEngine
import com.example.core.audio.CardSfx
import com.example.core.model.AiDifficulty
import com.example.core.model.AppLanguage
import com.example.core.model.AppSettings
import com.example.core.model.GameCatalog
import com.example.core.network.BluetoothMultiplayerManager
import com.example.core.network.BluetoothUiState
import com.example.core.network.MultiplayerUiState
import com.example.core.network.WifiMultiplayerManager
import com.example.core.storage.AppDatabase
import com.example.core.storage.MatchHistoryEntity
import com.example.core.storage.PlayerProfileEntity
import com.example.core.storage.SavedGameEntity
import com.example.core.storage.WorldCardRepository
import com.example.engine.CardGameEngine
import com.example.engine.EngineFactory
import com.example.engine.GameMove
import com.example.engine.TablePhase
import com.example.engine.UniversalTableState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    HOME, LIBRARY, MULTIPLAYER, MANUAL, SETTINGS
}

data class NavigationUiState(
    val showSplash: Boolean = true,
    val activeTab: MainTab = MainTab.HOME,
    val inGameTable: Boolean = false,
    val selectedManualGameId: String = GameCatalog.GAME_TWENTY_NINE,
    val libraryInitialCategory: String? = null
)

class WorldCardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = WorldCardRepository(db)
    val audioEngine = CardAudioEngine(application)
    val wifiManager = WifiMultiplayerManager(application)
    val bluetoothManager = BluetoothMultiplayerManager(application)

    private val _nav = MutableStateFlow(NavigationUiState())
    val nav: StateFlow<NavigationUiState> = _nav.asStateFlow()

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )
    val profile: StateFlow<PlayerProfileEntity> = repo.playerProfile.stateIn(
        viewModelScope, SharingStarted.Eagerly, PlayerProfileEntity()
    )
    val activeSave: StateFlow<SavedGameEntity?> = repo.activeSave.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )
    val favoriteIds: StateFlow<Set<String>> = repo.favoriteIds.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptySet()
    )
    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repo.matchHistory.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val wifiState: StateFlow<MultiplayerUiState> = wifiManager.state
    val bluetoothState: StateFlow<BluetoothUiState> = bluetoothManager.state

    private var currentEngine: CardGameEngine? = null
    private val _tableState = MutableStateFlow<UniversalTableState?>(null)
    val tableState: StateFlow<UniversalTableState?> = _tableState.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var aiLoopJob: Job? = null

    init {
        audioEngine.initialize()
        viewModelScope.launch {
            repo.ensureDefaultsInitialized()
        }
        viewModelScope.launch {
            settings.collect { s ->
                audioEngine.updateSettings(s)
            }
        }
        bluetoothManager.refreshStatus()
        refreshNetworkDiagnostics()
    }

    fun dismissSplash() {
        _nav.update { it.copy(showSplash = false) }
    }

    fun selectTab(tab: MainTab, categoryFilter: String? = null) {
        audioEngine.play(CardSfx.UI_CLICK)
        _nav.update {
            it.copy(
                activeTab = tab,
                inGameTable = false,
                libraryInitialCategory = categoryFilter
            )
        }
    }

    fun openManualForGame(gameId: String) {
        audioEngine.play(CardSfx.UI_CLICK)
        _nav.update {
            it.copy(
                activeTab = MainTab.MANUAL,
                inGameTable = false,
                selectedManualGameId = gameId
            )
        }
    }

    fun selectManualGame(gameId: String) {
        audioEngine.play(CardSfx.UI_CLICK)
        _nav.update { it.copy(selectedManualGameId = gameId) }
    }

    fun startNewGame(
        gameId: String,
        ruleProfileId: String? = null,
        difficulty: AiDifficulty? = null
    ) {
        val info = GameCatalog.byId(gameId)
        val chosenProfile = ruleProfileId ?: info.ruleProfiles.first().id
        val chosenDiff = difficulty ?: settings.value.defaultAiDifficulty
        val engine = EngineFactory.create(gameId)
        currentEngine = engine
        val state = engine.initializeGame(
            ruleProfileId = chosenProfile,
            difficulty = chosenDiff,
            playerName = profile.value.playerName,
            virtualChips = profile.value.virtualChips
        )
        _tableState.value = state
        _isPaused.value = false
        _nav.update { it.copy(inGameTable = true, selectedManualGameId = gameId) }
        audioEngine.play(CardSfx.CARD_SHUFFLE)
        persistActiveState(engine, state)
        scheduleAiTurnIfNeeded()
    }

    fun resumeSavedGame() {
        val save = activeSave.value ?: return
        val engine = EngineFactory.create(save.gameId)
        val restored = engine.restoreState(save.stateJson)
        if (restored != null) {
            currentEngine = engine
            _tableState.value = restored
            _isPaused.value = false
            _nav.update { it.copy(inGameTable = true, selectedManualGameId = save.gameId) }
            audioEngine.play(CardSfx.CARD_DEAL)
            scheduleAiTurnIfNeeded()
        } else {
            startNewGame(save.gameId, save.ruleProfileId, AiDifficulty.fromLevel(save.difficultyLevel))
        }
    }

    fun dispatchMove(move: GameMove) {
        val engine = currentEngine ?: return
        if (_isPaused.value) return
        val res = engine.applyMove(move)
        if (res.accepted) {
            _tableState.value = res.state
            audioEngine.play(res.sfx)
            if (res.chipDelta != 0L) {
                viewModelScope.launch { repo.updateVirtualChips(res.chipDelta) }
            }
            if (res.state.phase == TablePhase.MATCH_OVER) {
                recordMatchOutcome(res.state)
            }
            persistActiveState(engine, res.state)
            scheduleAiTurnIfNeeded()
        }
    }

    private fun scheduleAiTurnIfNeeded() {
        aiLoopJob?.cancel()
        aiLoopJob = viewModelScope.launch {
            while (true) {
                if (_isPaused.value || !_nav.value.inGameTable) break
                val engine = currentEngine ?: break
                val st = _tableState.value ?: break
                if (st.phase == TablePhase.ROUND_OVER || st.phase == TablePhase.MATCH_OVER) break
                val aiMove = engine.computeAiMove() ?: break

                val speedFactor = settings.value.animationSpeed.coerceIn(0.6f, 1.6f)
                val baseDelay = when {
                    st.gameId == GameCatalog.GAME_HAZARI -> 1350L
                    st.phase == TablePhase.BIDDING || st.phase == TablePhase.SELECT_TRUMP -> 620L
                    else -> 760L
                }
                delay((baseDelay / speedFactor).toLong().coerceIn(280L, 1800L))
                if (_isPaused.value || !_nav.value.inGameTable) break

                val res = engine.applyMove(aiMove)
                if (!res.accepted) break
                _tableState.value = res.state
                audioEngine.play(res.sfx)
                if (res.chipDelta != 0L) {
                    repo.updateVirtualChips(res.chipDelta)
                }
                if (res.state.phase == TablePhase.MATCH_OVER) {
                    recordMatchOutcome(res.state)
                }
                persistActiveState(engine, res.state)
            }
        }
    }

    private fun persistActiveState(engine: CardGameEngine, state: UniversalTableState) {
        viewModelScope.launch {
            try {
                val info = GameCatalog.byId(state.gameId)
                val summaryEn = "${info.nameEn} • Round ${state.roundNumber}"
                val summaryBn = "${info.nameBn} • রাউন্ড ${state.roundNumber}"
                repo.saveActiveGame(
                    gameId = state.gameId,
                    ruleProfileId = state.ruleProfileId,
                    difficultyLevel = state.difficulty.level,
                    stateJson = engine.serializeState(),
                    summaryEn = summaryEn,
                    summaryBn = summaryBn
                )
            } catch (_: Throwable) {}
        }
    }

    private fun recordMatchOutcome(state: UniversalTableState) {
        viewModelScope.launch {
            val won = state.matchWinnerIsHuman == true
            val bonusChips = if (won) 150L else 25L
            repo.recordMatchResult(
                gameId = state.gameId,
                ruleProfileId = state.ruleProfileId,
                difficultyLevel = state.difficulty.level,
                playerWon = won,
                scoreSummary = state.roundResultSummaryEn.ifBlank { if (won) "Victory" else "Finished" },
                chipDelta = bonusChips
            )
        }
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        if (!paused) scheduleAiTurnIfNeeded()
    }

    fun exitGameTable() {
        aiLoopJob?.cancel()
        val eng = currentEngine
        val st = _tableState.value
        if (eng != null && st != null) persistActiveState(eng, st)
        _isPaused.value = false
        _nav.update { it.copy(inGameTable = false) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings.value)
        viewModelScope.launch {
            repo.saveSettings(next)
        }
    }

    fun toggleLanguage() {
        audioEngine.play(CardSfx.UI_CLICK)
        updateSettings {
            it.copy(language = if (it.language == AppLanguage.ENGLISH) AppLanguage.BANGLA else AppLanguage.ENGLISH)
        }
    }

    fun toggleFavorite(gameId: String) {
        audioEngine.play(CardSfx.UI_CLICK)
        val isFav = favoriteIds.value.contains(gameId)
        viewModelScope.launch { repo.toggleFavorite(gameId, isFav) }
    }

    fun refreshNetworkDiagnostics() {
        bluetoothManager.refreshStatus()
        val bt = bluetoothManager.state.value
        wifiManager.runDiagnostics(bt.isSupported, bt.isEnabled)
    }

    override fun onCleared() {
        super.onCleared()
        aiLoopJob?.cancel()
        audioEngine.release()
        wifiManager.release()
        bluetoothManager.release()
    }
}
