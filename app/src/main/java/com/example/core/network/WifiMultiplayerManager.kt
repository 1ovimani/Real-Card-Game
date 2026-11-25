package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Real offline Wi-Fi & Mobile Hotspot LAN Multiplayer Transport:
 * - Authoritative TCP ServerSocket host (configurable port, default 45290)
 * - UDP Broadcast room advertiser & scanner (port 45291) for zero-config discovery
 * - Manual IP + Port connection for strict mobile hotspots
 * - Password validation, session tokens, duplicate packet guard, and live diagnostics
 */
class WifiMultiplayerManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(MultiplayerUiState(hostIp = detectLocalIpv4()))
    val state: StateFlow<MultiplayerUiState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var beaconJob: Job? = null
    private var scanJob: Job? = null
    private var clientSocket: Socket? = null
    private var clientWriter: PrintWriter? = null
    private var clientReadJob: Job? = null

    private val clientWriters = ConcurrentHashMap<String, PrintWriter>()
    private val seenSeqs = ConcurrentHashMap<String, Long>()
    private var hostPasswordHash: String = ""

    fun detectLocalIpv4(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var fallback = "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (iface.name.startsWith("wlan") || iface.name.startsWith("ap") || ip.startsWith("192.168.")) {
                            return ip
                        }
                        fallback = ip
                    }
                }
            }
            fallback
        } catch (_: Throwable) {
            "127.0.0.1"
        }
    }

    fun runDiagnostics(btSupported: Boolean, btEnabled: Boolean) {
        scope.launch {
            val ip = detectLocalIpv4()
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val wifiOn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val hotspotLikely = ip.startsWith("192.168.43.") || ip.startsWith("192.168.137.") || ip.startsWith("172.20.")
            val portOpen = serverSocket?.isClosed == false

            val report = NetworkDiagnosticReport(
                localIpv4 = ip,
                wifiConnected = wifiOn,
                hotspotLikelyActive = hotspotLikely,
                bluetoothSupported = btSupported,
                bluetoothEnabled = btEnabled,
                serverPortOpen = portOpen
            )
            _state.update {
                it.copy(
                    hostIp = ip,
                    diagnostics = report,
                    statusLog = (it.statusLog + "Diagnostics: IP=$ip | Wi-Fi=$wifiOn | ServerOpen=$portOpen").takeLast(20)
                )
            }
        }
    }

    fun startHosting(roomName: String, gameId: String, password: String, port: Int = MultiplayerProtocol.DEFAULT_TCP_PORT) {
        stopAll()
        val ip = detectLocalIpv4()
        val roomId = "WCM-" + UUID.randomUUID().toString().take(4).uppercase()
        hostPasswordHash = MultiplayerProtocol.sha256Short(password)

        acceptJob = scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                _state.update {
                    it.copy(
                        isHostingWifi = true,
                        roomId = roomId,
                        roomName = roomName.ifBlank { "Dhaka Card Table" },
                        selectedGameId = gameId,
                        hostIp = ip,
                        hostPort = port,
                        peers = listOf(ConnectedPeer("host", "You (Host)", ip, isReady = true, seatIndex = 0, transport = "Wi-Fi")),
                        activeConnectionMessage = "Hosting $roomId on $ip:$port",
                        statusLog = (it.statusLog + "Host TCP server listening on $ip:$port ($roomId)").takeLast(20)
                    )
                }
                startUdpBeacon(roomId, roomName.ifBlank { "Dhaka Card Table" }, ip, port, gameId, password.isNotEmpty())

                while (isActive && !ss.isClosed) {
                    val socket = ss.accept()
                    handleIncomingClient(socket, roomId, gameId)
                }
            } catch (e: Throwable) {
                if (isActive) {
                    _state.update {
                        it.copy(
                            isHostingWifi = false,
                            activeConnectionMessage = "Host error: ${e.message}",
                            statusLog = (it.statusLog + "Host stopped: ${e.message}").takeLast(20)
                        )
                    }
                }
            }
        }
    }

    private fun handleIncomingClient(socket: Socket, roomId: String, gameId: String) {
        scope.launch {
            val peerId = UUID.randomUUID().toString().take(8)
            val addr = socket.inetAddress.hostAddress ?: "peer"
            try {
                socket.soTimeout = 30000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)
                val firstLine = reader.readLine() ?: return@launch
                val hello = JSONObject(firstLine)
                if (hello.optString("type") != "HELLO") {
                    socket.close(); return@launch
                }
                if (hostPasswordHash.isNotEmpty() && hello.optString("pw") != hostPasswordHash) {
                    writer.println(JSONObject().put("type", "ERROR").put("msg", "Invalid room password").toString())
                    socket.close(); return@launch
                }
                val peerName = hello.optString("name", "Guest").take(24)
                val seatIndex = _state.value.peers.size.coerceAtMost(3)
                val token = UUID.randomUUID().toString().take(12)
                clientWriters[peerId] = writer
                writer.println(MultiplayerProtocol.buildWelcomePacket(seatIndex, roomId, gameId, token))

                _state.update { st ->
                    val updated = st.peers + ConnectedPeer(peerId, peerName, addr, isReady = true, seatIndex = seatIndex, transport = "Wi-Fi")
                    st.copy(
                        peers = updated,
                        statusLog = (st.statusLog + "$peerName joined seat #$seatIndex ($addr)").takeLast(20)
                    )
                }

                // Read subsequent validated messages
                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val msg = JSONObject(line)
                    val seq = msg.optLong("seq", 0L)
                    val prev = seenSeqs[peerId] ?: -1L
                    if (seq > 0 && seq <= prev) continue // Duplicate guard
                    if (seq > 0) seenSeqs[peerId] = seq
                    if (msg.optString("type") == "READY") {
                        val ready = msg.optBoolean("ready", true)
                        _state.update { st ->
                            st.copy(peers = st.peers.map { if (it.peerId == peerId) it.copy(isReady = ready) else it })
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                clientWriters.remove(peerId)
                try { socket.close() } catch (_: Throwable) {}
                _state.update { st ->
                    st.copy(
                        peers = st.peers.filterNot { it.peerId == peerId },
                        statusLog = (st.statusLog + "Peer $addr disconnected").takeLast(20)
                    )
                }
            }
        }
    }

    private fun startUdpBeacon(roomId: String, roomName: String, ip: String, tcpPort: Int, gameId: String, hasPw: Boolean) {
        beaconJob?.cancel()
        beaconJob = scope.launch {
            var ds: DatagramSocket? = null
            try {
                ds = DatagramSocket().apply { broadcast = true }
                while (isActive && _state.value.isHostingWifi) {
                    val payload = JSONObject().apply {
                        put("wcm", 1)
                        put("id", roomId)
                        put("name", roomName)
                        put("ip", ip)
                        put("port", tcpPort)
                        put("game", gameId)
                        put("cnt", _state.value.peers.size)
                        put("max", 4)
                        put("pw", hasPw)
                    }.toString().toByteArray()
                    val packet = DatagramPacket(payload, payload.size, InetAddress.getByName("255.255.255.255"), MultiplayerProtocol.DISCOVERY_UDP_PORT)
                    try { ds.send(packet) } catch (_: Throwable) {}
                    delay(1800L)
                }
            } catch (_: Throwable) {
            } finally {
                try { ds?.close() } catch (_: Throwable) {}
            }
        }
    }

    fun scanLanRooms() {
        scanJob?.cancel()
        scanJob = scope.launch {
            _state.update {
                it.copy(
                    isScanningWifi = true,
                    discoveredRooms = emptyList(),
                    statusLog = (it.statusLog + "Scanning local Wi-Fi / Hotspot LAN (UDP ${MultiplayerProtocol.DISCOVERY_UDP_PORT})...").takeLast(20)
                )
            }
            var ds: DatagramSocket? = null
            try {
                ds = DatagramSocket(MultiplayerProtocol.DISCOVERY_UDP_PORT).apply {
                    broadcast = true
                    soTimeout = 1000
                }
                val buf = ByteArray(1024)
                val endAt = System.currentTimeMillis() + 6000L
                while (isActive && System.currentTimeMillis() < endAt) {
                    try {
                        val pkt = DatagramPacket(buf, buf.size)
                        ds.receive(pkt)
                        val text = String(pkt.data, 0, pkt.length)
                        val obj = JSONObject(text)
                        if (obj.optInt("wcm") == 1) {
                            val room = DiscoveredRoom(
                                roomId = obj.getString("id"),
                                roomName = obj.getString("name"),
                                hostIp = obj.optString("ip", pkt.address.hostAddress ?: ""),
                                port = obj.optInt("port", MultiplayerProtocol.DEFAULT_TCP_PORT),
                                gameId = obj.optString("game", "twenty_nine"),
                                playerCount = obj.optInt("cnt", 1),
                                maxPlayers = obj.optInt("max", 4),
                                hasPassword = obj.optBoolean("pw", false)
                            )
                            _state.update { st ->
                                val filtered = st.discoveredRooms.filterNot { it.roomId == room.roomId }
                                st.copy(discoveredRooms = filtered + room)
                            }
                        }
                    } catch (_: Throwable) { /* Timeout tick */ }
                }
            } catch (e: Throwable) {
                _state.update { st ->
                    st.copy(statusLog = (st.statusLog + "LAN scan note: ${e.message}").takeLast(20))
                }
            } finally {
                try { ds?.close() } catch (_: Throwable) {}
                _state.update { it.copy(isScanningWifi = false) }
            }
        }
    }

    fun connectToHost(ip: String, port: Int, playerName: String, password: String) {
        stopAll()
        clientReadJob = scope.launch {
            try {
                val sock = Socket()
                sock.connect(InetSocketAddress(ip.trim(), port), 5000)
                clientSocket = sock
                val writer = PrintWriter(sock.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                clientWriter = writer

                val token = UUID.randomUUID().toString().take(10)
                writer.println(MultiplayerProtocol.buildHelloPacket(playerName, password, token))
                val respLine = reader.readLine() ?: throw IllegalStateException("No response from host")
                val resp = JSONObject(respLine)
                if (resp.optString("type") == "ERROR") {
                    throw IllegalStateException(resp.optString("msg", "Rejected by host"))
                }
                if (resp.optString("type") == "WELCOME") {
                    val roomId = resp.optString("roomId", "ROOM")
                    val gameId = resp.optString("gameId", "twenty_nine")
                    val seat = resp.optInt("seat", 1)
                    _state.update {
                        it.copy(
                            isConnectedWifiClient = true,
                            roomId = roomId,
                            selectedGameId = gameId,
                            sessionToken = resp.optString("token", token),
                            activeConnectionMessage = "Connected to $ip:$port as Seat #$seat ($roomId)",
                            statusLog = (it.statusLog + "Joined $roomId at $ip:$port (Seat #$seat)").takeLast(20)
                        )
                    }
                    while (isActive && !sock.isClosed) {
                        val line = reader.readLine() ?: break
                        _state.update { st ->
                            st.copy(statusLog = (st.statusLog + "Host sync: ${line.take(64)}").takeLast(20))
                        }
                    }
                }
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isConnectedWifiClient = false,
                        activeConnectionMessage = "Connection failed: ${e.message}",
                        statusLog = (it.statusLog + "Client error: ${e.message}").takeLast(20)
                    )
                }
            }
        }
    }

    fun stopAll() {
        acceptJob?.cancel(); beaconJob?.cancel(); scanJob?.cancel(); clientReadJob?.cancel()
        clientWriters.values.forEach { try { it.close() } catch (_: Throwable) {} }
        clientWriters.clear()
        try { serverSocket?.close() } catch (_: Throwable) {}
        try { clientSocket?.close() } catch (_: Throwable) {}
        serverSocket = null; clientSocket = null; clientWriter = null
        _state.update {
            it.copy(
                isHostingWifi = false,
                isConnectedWifiClient = false,
                isScanningWifi = false,
                peers = emptyList(),
                activeConnectionMessage = ""
            )
        }
    }

    fun release() {
        stopAll()
        scope.cancel()
    }
}
