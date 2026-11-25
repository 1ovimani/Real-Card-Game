package com.example.core.network

import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class DiscoveredRoom(
    val roomId: String,
    val roomName: String,
    val hostIp: String,
    val port: Int,
    val gameId: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val hasPassword: Boolean,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class ConnectedPeer(
    val peerId: String,
    val name: String,
    val address: String,
    val isReady: Boolean = false,
    val seatIndex: Int = -1,
    val transport: String = "Wi-Fi"
)

data class NetworkDiagnosticReport(
    val localIpv4: String,
    val wifiConnected: Boolean,
    val hotspotLikelyActive: Boolean,
    val bluetoothSupported: Boolean,
    val bluetoothEnabled: Boolean,
    val serverPortOpen: Boolean,
    val lastCheckMs: Long = System.currentTimeMillis()
)

data class MultiplayerUiState(
    val isHostingWifi: Boolean = false,
    val isConnectedWifiClient: Boolean = false,
    val isScanningWifi: Boolean = false,
    val isHostingBluetooth: Boolean = false,
    val isConnectedBluetooth: Boolean = false,
    val roomId: String = "",
    val roomName: String = "Dhaka Card Table",
    val selectedGameId: String = "twenty_nine",
    val hostIp: String = "",
    val hostPort: Int = DEFAULT_TCP_PORT,
    val sessionToken: String = "",
    val peers: List<ConnectedPeer> = emptyList(),
    val discoveredRooms: List<DiscoveredRoom> = emptyList(),
    val pairedBluetoothDevices: List<Pair<String, String>> = emptyList(), // (name, mac)
    val statusLog: List<String> = emptyList(),
    val diagnostics: NetworkDiagnosticReport? = null,
    val activeConnectionMessage: String = ""
)

object MultiplayerProtocol {
    const val DEFAULT_TCP_PORT = 45290
    const val DISCOVERY_UDP_PORT = 45291
    const val PROTOCOL_VERSION = 1
    val BLUETOOTH_UUID: UUID = UUID.fromString("8a3e4f12-9b6d-4c88-a7c2-5e1f8c290029")

    fun sha256Short(input: String): String {
        if (input.isEmpty()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }

    fun buildHelloPacket(playerName: String, password: String, reconnectToken: String): String =
        JSONObject().apply {
            put("type", "HELLO")
            put("v", PROTOCOL_VERSION)
            put("name", playerName.take(24))
            put("pw", sha256Short(password))
            put("token", reconnectToken)
            put("seq", System.currentTimeMillis())
        }.toString()

    fun buildWelcomePacket(seatIndex: Int, roomId: String, gameId: String, token: String): String =
        JSONObject().apply {
            put("type", "WELCOME")
            put("seat", seatIndex)
            put("roomId", roomId)
            put("gameId", gameId)
            put("token", token)
        }.toString()

    fun buildReadyPacket(peerId: String, ready: Boolean): String =
        JSONObject().apply {
            put("type", "READY")
            put("peerId", peerId)
            put("ready", ready)
        }.toString()

    /**
     * Authoritative host privacy filter:
     * Only sends the specific client's own hand + public table cards — NEVER hidden opponent cards!
     */
    fun buildFilteredStatePacket(
        seq: Long,
        gameId: String,
        targetSeat: Int,
        ownHandCardIds: List<String>,
        publicCenterJson: String,
        activeSeat: Int
    ): String = JSONObject().apply {
        put("type", "STATE_SYNC")
        put("seq", seq)
        put("gameId", gameId)
        put("seat", targetSeat)
        put("hand", ownHandCardIds.joinToString(","))
        put("center", publicCenterJson)
        put("activeSeat", activeSeat)
    }.toString()
}

const val DEFAULT_TCP_PORT = MultiplayerProtocol.DEFAULT_TCP_PORT
