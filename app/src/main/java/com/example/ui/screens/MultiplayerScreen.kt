package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.AppLanguage
import com.example.core.model.GameCatalog
import com.example.core.network.BluetoothMultiplayerManager
import com.example.core.network.BluetoothUiState
import com.example.core.network.MultiplayerProtocol
import com.example.core.network.MultiplayerUiState
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.components.SalonChipBadge
import com.example.ui.components.VelvetSecondaryButton
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldElevated
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun MultiplayerScreen(
    language: AppLanguage,
    playerName: String,
    wifiState: MultiplayerUiState,
    btState: BluetoothUiState,
    btManager: BluetoothMultiplayerManager,
    onHostWifi: (roomName: String, gameId: String, password: String, port: Int) -> Unit,
    onStopWifi: () -> Unit,
    onScanWifi: () -> Unit,
    onJoinWifi: (ip: String, port: Int, password: String) -> Unit,
    onStartBtServer: () -> Unit,
    onConnectBtDevice: (mac: String) -> Unit,
    onStopBt: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onLaunchLocalTable: (gameId: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = language == AppLanguage.BANGLA
    var selectedTab by remember { mutableIntStateOf(0) }

    var roomNameInput by remember { mutableStateOf(wifiState.roomName.ifBlank { "Dhaka 29 Table" }) }
    var passwordInput by remember { mutableStateOf("") }
    var joinIpInput by remember { mutableStateOf("192.168.43.1") }
    var portInput by remember { mutableStateOf(MultiplayerProtocol.DEFAULT_TCP_PORT.toString()) }
    var selectedGameId by remember { mutableStateOf(GameCatalog.GAME_TWENTY_NINE) }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        btManager.refreshStatus()
        onRefreshDiagnostics()
    }

    val mpGames = remember { GameCatalog.allGames.filter { it.supportsLocalMultiplayer } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, ObsidianNight)))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.offlineMultiplayerTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = strings.offlineMultiplayerDesc,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianCard,
            contentColor = ChampagneGold,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = BurnishedGold
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.wifiHotspotTab) }, icon = { Icon(Icons.Filled.Wifi, null, modifier = Modifier.size(18.dp)) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; btManager.refreshStatus() }, text = { Text(strings.bluetoothTab) }, icon = { Icon(Icons.Filled.Bluetooth, null, modifier = Modifier.size(18.dp)) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2; onRefreshDiagnostics() }, text = { Text(strings.diagnosticsTab) }, icon = { Icon(Icons.Filled.NetworkCheck, null, modifier = Modifier.size(18.dp)) })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // WI-FI / MOBILE HOTSPOT TAB
                    item {
                        LuxuryGlassCard(borderColor = BurnishedGold.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.hostRoom, style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                                    SalonChipBadge(text = "IP: ${wifiState.hostIp}", accent = SuccessEmerald)
                                }

                                // Game picker row
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(mpGames, key = { it.id }) { g ->
                                        FilterChip(
                                            selected = selectedGameId == g.id,
                                            onClick = { selectedGameId = g.id },
                                            label = { Text(if (isBn) g.nameBn else g.nameEn) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BurnishedGold, selectedLabelColor = EmeraldDeep)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = roomNameInput,
                                        onValueChange = { roomNameInput = it },
                                        label = { Text(strings.roomName) },
                                        singleLine = true,
                                        colors = mpFieldColors(),
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    OutlinedTextField(
                                        value = portInput,
                                        onValueChange = { portInput = it.filter { ch -> ch.isDigit() }.take(5) },
                                        label = { Text(strings.portLabel) },
                                        singleLine = true,
                                        colors = mpFieldColors(),
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(strings.roomPassword) },
                                    singleLine = true,
                                    colors = mpFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (wifiState.isHostingWifi) {
                                    SalonChipBadge(text = wifiState.activeConnectionMessage, accent = SuccessEmerald)
                                    wifiState.peers.forEach { peer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(EmeraldElevated.copy(alpha = 0.5f))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${peer.name} (${peer.address})", color = TextPrimaryLight, style = MaterialTheme.typography.bodySmall)
                                            Icon(Icons.Filled.CheckCircle, contentDescription = strings.readyStatus, tint = SuccessEmerald, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GoldPrimaryButton(
                                            text = strings.startMatchHost,
                                            icon = Icons.Filled.PlayArrow,
                                            onClick = { onLaunchLocalTable(selectedGameId) },
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        VelvetSecondaryButton(
                                            text = strings.stopHosting,
                                            onClick = onStopWifi,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    GoldPrimaryButton(
                                        text = strings.hostRoom,
                                        icon = Icons.Filled.Router,
                                        testTag = "mp_host_wifi_btn",
                                        onClick = {
                                            val port = portInput.toIntOrNull()?.coerceIn(1024, 65535) ?: MultiplayerProtocol.DEFAULT_TCP_PORT
                                            onHostWifi(roomNameInput, selectedGameId, passwordInput, port)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // JOIN VIA LAN DISCOVERY OR MANUAL HOTSPOT IP
                    item {
                        LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.joinRoom, style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                                    VelvetSecondaryButton(
                                        text = if (wifiState.isScanningWifi) "..." else strings.scanRooms,
                                        icon = Icons.Filled.Refresh,
                                        onClick = onScanWifi
                                    )
                                }

                                if (wifiState.discoveredRooms.isNotEmpty()) {
                                    wifiState.discoveredRooms.forEach { room ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(EmeraldElevated.copy(alpha = 0.6f))
                                                .border(1.dp, BurnishedGold, RoundedCornerShape(12.dp))
                                                .clickable { onJoinWifi(room.hostIp, room.port, passwordInput) }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${room.roomName} [${room.roomId}]", color = ChampagneGold, fontWeight = FontWeight.Bold)
                                                Text("${room.hostIp}:${room.port} • ${room.playerCount}/${room.maxPlayers}P", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                                            }
                                            SalonChipBadge(text = strings.connectBtn, accent = SuccessEmerald)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = joinIpInput,
                                    onValueChange = { joinIpInput = it },
                                    label = { Text(strings.hostIpAddress) },
                                    singleLine = true,
                                    colors = mpFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (wifiState.isConnectedWifiClient) {
                                    SalonChipBadge(text = wifiState.activeConnectionMessage, accent = SuccessEmerald)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GoldPrimaryButton(
                                            text = strings.playNow,
                                            onClick = { onLaunchLocalTable(wifiState.selectedGameId) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        VelvetSecondaryButton(
                                            text = strings.disconnectBtn,
                                            onClick = onStopWifi,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    VelvetSecondaryButton(
                                        text = strings.connectBtn,
                                        icon = Icons.Filled.Hub,
                                        onClick = {
                                            val port = portInput.toIntOrNull() ?: MultiplayerProtocol.DEFAULT_TCP_PORT
                                            onJoinWifi(joinIpInput, port, passwordInput)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // BLUETOOTH CLASSIC RFCOMM TAB
                    item {
                        LuxuryGlassCard(borderColor = BurnishedGold.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Bluetooth Classic RFCOMM", style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                                Text(btState.statusMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)

                                if (!btState.hasPermissions) {
                                    Text(strings.btPermissionNeeded, style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                                    GoldPrimaryButton(
                                        text = strings.grantPermissions,
                                        icon = Icons.Filled.Bluetooth,
                                        onClick = { btPermLauncher.launch(btManager.requiredPermissions()) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (btState.isHosting || btState.isConnected) {
                                            VelvetSecondaryButton(text = strings.disconnectBtn, onClick = onStopBt, modifier = Modifier.weight(1f))
                                        } else {
                                            GoldPrimaryButton(
                                                text = strings.btHostServer,
                                                icon = Icons.Filled.Bluetooth,
                                                onClick = onStartBtServer,
                                                modifier = Modifier.weight(1.2f)
                                            )
                                        }
                                        VelvetSecondaryButton(
                                            text = strings.btScanDevices,
                                            icon = Icons.Filled.Refresh,
                                            onClick = { btManager.refreshStatus() },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (btState.pairedDevices.isNotEmpty()) {
                                        Text(
                                            text = if (isBn) "পেয়ার করা ডিভাইস (সংযুক্ত হতে ট্যাপ করুন):" else "Paired Devices (Tap to connect RFCOMM):",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ChampagneGold
                                        )
                                        btState.pairedDevices.forEach { (name, mac) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(EmeraldElevated.copy(alpha = 0.5f))
                                                    .clickable { onConnectBtDevice(mac) }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(name, color = TextPrimaryLight, fontWeight = FontWeight.SemiBold)
                                                    Text(mac, style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                                                }
                                                SalonChipBadge(text = strings.connectBtn, accent = ChampagneGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // DIAGNOSTICS & TROUBLESHOOTING GUIDE
                    item {
                        val diag = wifiState.diagnostics
                        LuxuryGlassCard(borderColor = SuccessEmerald.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isBn) "নেটওয়ার্ক ডায়াগনস্টিকস" else "Live Network Diagnostics", style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                                    VelvetSecondaryButton(text = "Check", icon = Icons.Filled.Refresh, onClick = onRefreshDiagnostics)
                                }
                                DiagRow("Local IPv4", diag?.localIpv4 ?: wifiState.hostIp, true)
                                DiagRow("Wi-Fi Active", if (diag?.wifiConnected == true) "Connected" else "Offline / Hotspot", diag?.wifiConnected == true)
                                DiagRow("TCP Server Port ${wifiState.hostPort}", if (diag?.serverPortOpen == true) "Listening" else "Idle", diag?.serverPortOpen == true)
                                DiagRow("Bluetooth Radio", if (diag?.bluetoothEnabled == true) "Enabled" else if (diag?.bluetoothSupported == true) "Disabled" else "Unavailable", diag?.bluetoothEnabled == true)
                            }
                        }
                    }

                    item {
                        LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Security, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                    Text(if (isBn) "অফলাইন হটস্পট ও নিরাপত্তা গাইড" else "Hotspot, Security & Troubleshooting", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                                }
                                Text(
                                    text = if (isBn) {
                                        "১. মোবাইল হটস্পট: একজনের ফোনে হটস্পট চালু করুন (ইন্টারনেট ডাটা বন্ধ রাখলেও চলবে)।\n" +
                                            "২. বন্ধুরা সেই হটস্পটে যুক্ত হলে হোস্ট ফোনে 'রুম তৈরি করুন' চাপুন।\n" +
                                            "৩. অন্যান্য ফোনে 'রুম খুঁজুন' চাপুন অথবা হোস্টের আইপি ঠিকানা (যেমন 192.168.43.1) লিখে সংযুক্ত হন।\n" +
                                            "৪. গোপনীয়তা: হোস্ট সার্ভার প্রতি খেলোয়াড়কে কেবল তার নিজের তাস পাঠায় — প্রতিপক্ষের গোপন তাস কখনোই নেটওয়ার্কে যায় না।"
                                    } else {
                                        "1. Mobile Hotspot Play: Turn on Personal Hotspot on one Android phone (mobile data can be completely OFF).\n" +
                                            "2. Connect friends' phones to that Hotspot, then tap 'Host Room' on the host phone.\n" +
                                            "3. Friends tap 'Scan LAN' or enter the Host IP shown at the top (usually 192.168.43.1) and tap Connect.\n" +
                                            "4. Hidden-Card Security: The authoritative host validates every turn sequence and transmits filtered state packets so hidden opponent cards are never leaked."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                                )
                            }
                        }
                    }
                }
            }

            // Live Transport Event Log
            val combinedLogs = (wifiState.statusLog + btState.logs).takeLast(8)
            if (combinedLogs.isNotEmpty()) {
                item {
                    LuxuryGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Session Log", style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                            combinedLogs.forEach { line ->
                                Text("• $line", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(color = if (ok) SuccessEmerald else ChampagneGold, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun mpFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BurnishedGold,
    unfocusedBorderColor = ObsidianBorder,
    focusedTextColor = TextPrimaryLight,
    unfocusedTextColor = TextPrimaryLight,
    focusedLabelColor = ChampagneGold,
    unfocusedLabelColor = TextSecondaryLight
)
