package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameTableScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ManualScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WorldCardMasterTheme
import com.example.viewmodel.MainTab
import com.example.viewmodel.WorldCardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: WorldCardViewModel = viewModel()
            WorldCardMasterRoot(vm = vm)
        }
    }
}

private data class NavDest(
    val tab: MainTab,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun WorldCardMasterRoot(vm: WorldCardViewModel) {
    val nav by vm.nav.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val activeSave by vm.activeSave.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val matchHistory by vm.matchHistory.collectAsStateWithLifecycle()
    val tableState by vm.tableState.collectAsStateWithLifecycle()
    val isPaused by vm.isPaused.collectAsStateWithLifecycle()
    val wifiState by vm.wifiState.collectAsStateWithLifecycle()
    val btState by vm.bluetoothState.collectAsStateWithLifecycle()

    WorldCardMasterTheme(
        language = settings.language,
        tableThemeId = settings.tableThemeId,
        highContrast = settings.highContrast,
        textScale = settings.textScale
    ) {
        val strings = LocalAppStrings.current

        when {
            nav.showSplash -> {
                SplashScreen(onFinished = { vm.dismissSplash() })
            }
            nav.inGameTable && tableState != null -> {
                GameTableScreen(
                    state = tableState!!,
                    settings = settings,
                    isPaused = isPaused,
                    onMove = { vm.dispatchMove(it) },
                    onToggleMute = { vm.updateSettings { it.copy(muteAll = !it.muteAll) } },
                    onSetPaused = { vm.setPaused(it) },
                    onExitTable = { vm.exitGameTable() }
                )
            }
            else -> {
                val destinations = listOf(
                    NavDest(MainTab.HOME, Icons.Filled.Home, "nav_home"),
                    NavDest(MainTab.LIBRARY, Icons.Filled.Casino, "nav_library"),
                    NavDest(MainTab.MULTIPLAYER, Icons.Filled.Wifi, "nav_multiplayer"),
                    NavDest(MainTab.MANUAL, Icons.AutoMirrored.Filled.MenuBook, "nav_manual"),
                    NavDest(MainTab.SETTINGS, Icons.Filled.Settings, "nav_settings")
                )
                fun labelFor(tab: MainTab) = when (tab) {
                    MainTab.HOME -> strings.navHome
                    MainTab.LIBRARY -> strings.navLibrary
                    MainTab.MULTIPLAYER -> strings.navMultiplayer
                    MainTab.MANUAL -> strings.navManual
                    MainTab.SETTINGS -> strings.navSettings
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 680.dp

                    if (useRail) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = ObsidianCard,
                                contentColor = ChampagneGold
                            ) {
                                destinations.forEach { d ->
                                    NavigationRailItem(
                                        selected = nav.activeTab == d.tab,
                                        onClick = { vm.selectTab(d.tab) },
                                        icon = { Icon(d.icon, contentDescription = labelFor(d.tab)) },
                                        label = { Text(labelFor(d.tab)) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = EmeraldDeep,
                                            selectedTextColor = ChampagneGold,
                                            indicatorColor = BurnishedGold,
                                            unselectedIconColor = TextSecondaryLight,
                                            unselectedTextColor = TextSecondaryLight
                                        ),
                                        modifier = Modifier.testTag(d.tag)
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                MainTabContent(
                                    nav = nav, vm = vm, settings = settings, profile = profile,
                                    activeSave = activeSave, favoriteIds = favoriteIds,
                                    matchHistory = matchHistory, wifiState = wifiState, btState = btState
                                )
                            }
                        }
                    } else {
                        Scaffold(
                            containerColor = ObsidianNight,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                NavigationBar(
                                    containerColor = ObsidianCard,
                                    tonalElevation = 4.dp
                                ) {
                                    destinations.forEach { d ->
                                        NavigationBarItem(
                                            selected = nav.activeTab == d.tab,
                                            onClick = { vm.selectTab(d.tab) },
                                            icon = { Icon(d.icon, contentDescription = labelFor(d.tab)) },
                                            label = { Text(labelFor(d.tab)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = EmeraldDeep,
                                                selectedTextColor = ChampagneGold,
                                                indicatorColor = BurnishedGold,
                                                unselectedIconColor = TextSecondaryLight,
                                                unselectedTextColor = TextSecondaryLight
                                            ),
                                            modifier = Modifier.testTag(d.tag)
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                MainTabContent(
                                    nav = nav, vm = vm, settings = settings, profile = profile,
                                    activeSave = activeSave, favoriteIds = favoriteIds,
                                    matchHistory = matchHistory, wifiState = wifiState, btState = btState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabContent(
    nav: com.example.viewmodel.NavigationUiState,
    vm: WorldCardViewModel,
    settings: com.example.core.model.AppSettings,
    profile: com.example.core.storage.PlayerProfileEntity,
    activeSave: com.example.core.storage.SavedGameEntity?,
    favoriteIds: Set<String>,
    matchHistory: List<com.example.core.storage.MatchHistoryEntity>,
    wifiState: com.example.core.network.MultiplayerUiState,
    btState: com.example.core.network.BluetoothUiState
) {
    when (nav.activeTab) {
        MainTab.HOME -> HomeScreen(
            language = settings.language,
            profile = profile,
            activeSave = activeSave,
            favoriteIds = favoriteIds,
            onPlayGame = { vm.startNewGame(it) },
            onResumeGame = { vm.resumeSavedGame() },
            onToggleFavorite = { vm.toggleFavorite(it) },
            onToggleLanguage = { vm.toggleLanguage() },
            onNavigateTab = { tab, cat -> vm.selectTab(tab, cat) },
            onOpenManual = { vm.openManualForGame(it) }
        )
        MainTab.LIBRARY -> LibraryScreen(
            language = settings.language,
            initialCategory = nav.libraryInitialCategory,
            defaultDifficulty = settings.defaultAiDifficulty,
            favoriteIds = favoriteIds,
            onStartGame = { id, prof, diff -> vm.startNewGame(id, prof, diff) },
            onToggleFavorite = { vm.toggleFavorite(it) },
            onOpenManual = { vm.openManualForGame(it) }
        )
        MainTab.MULTIPLAYER -> MultiplayerScreen(
            language = settings.language,
            playerName = profile.playerName,
            wifiState = wifiState,
            btState = btState,
            btManager = vm.bluetoothManager,
            onHostWifi = { room, gId, pw, port -> vm.wifiManager.startHosting(room, gId, pw, port) },
            onStopWifi = { vm.wifiManager.stopAll() },
            onScanWifi = { vm.wifiManager.scanLanRooms() },
            onJoinWifi = { ip, port, pw -> vm.wifiManager.connectToHost(ip, port, profile.playerName, pw) },
            onStartBtServer = { vm.bluetoothManager.startRfcommServer(profile.playerName) },
            onConnectBtDevice = { mac -> vm.bluetoothManager.connectToPairedDevice(mac, profile.playerName) },
            onStopBt = { vm.bluetoothManager.stop() },
            onRefreshDiagnostics = { vm.refreshNetworkDiagnostics() },
            onLaunchLocalTable = { vm.startNewGame(it) }
        )
        MainTab.MANUAL -> ManualScreen(
            language = settings.language,
            selectedGameId = nav.selectedManualGameId,
            onSelectGame = { vm.selectManualGame(it) },
            onToggleLanguage = { vm.toggleLanguage() },
            onPlayGame = { vm.startNewGame(it) }
        )
        MainTab.SETTINGS -> SettingsScreen(
            settings = settings,
            profile = profile,
            matchHistory = matchHistory,
            onUpdateSettings = { vm.updateSettings(it) }
        )
    }
}
