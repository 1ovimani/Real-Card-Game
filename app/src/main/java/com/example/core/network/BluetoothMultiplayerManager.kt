package com.example.core.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

data class BluetoothUiState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val isHosting: Boolean = false,
    val isConnected: Boolean = false,
    val pairedDevices: List<Pair<String, String>> = emptyList(), // (DeviceName, MAC)
    val connectedDeviceName: String = "",
    val statusMessage: String = "",
    val logs: List<String> = emptyList()
)

/**
 * Real Bluetooth Classic RFCOMM Multiplayer Transport:
 * - Respects Android 12+ (API 31+) BLUETOOTH_CONNECT / BLUETOOTH_SCAN permissions and legacy APIs
 * - Real BluetoothServerSocket host & BluetoothSocket client over dedicated service UUID
 * - Graceful diagnostics when Bluetooth is off or unavailable
 */
class BluetoothMultiplayerManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        mgr?.adapter
    }

    private val _state = MutableStateFlow(BluetoothUiState())
    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var ioJob: Job? = null

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshStatus() {
        val adapter = bluetoothAdapter
        val perms = hasRequiredPermissions()
        val enabled = try { adapter?.isEnabled == true } catch (_: SecurityException) { false }
        val paired = if (adapter != null && perms && enabled) {
            try {
                adapter.bondedDevices.orEmpty().map { dev ->
                    (dev.name ?: "Bluetooth Device") to dev.address
                }
            } catch (_: Throwable) { emptyList() }
        } else emptyList()

        _state.update {
            it.copy(
                isSupported = adapter != null,
                isEnabled = enabled,
                hasPermissions = perms,
                pairedDevices = paired,
                statusMessage = when {
                    adapter == null -> "Bluetooth hardware is unavailable on this device."
                    !perms -> "Grant Bluetooth permissions to host or connect."
                    !enabled -> "Enable Bluetooth in system settings to play over RFCOMM."
                    else -> "${paired.size} paired Bluetooth device(s) ready."
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startRfcommServer(playerName: String) {
        refreshStatus()
        val adapter = bluetoothAdapter ?: return
        if (!hasRequiredPermissions() || !adapter.isEnabled) return
        stop()

        ioJob = scope.launch {
            try {
                val ss = adapter.listenUsingRfcommWithServiceRecord("WorldCardMaster", MultiplayerProtocol.BLUETOOTH_UUID)
                serverSocket = ss
                _state.update {
                    it.copy(
                        isHosting = true,
                        statusMessage = "Bluetooth RFCOMM server listening for nearby paired player...",
                        logs = (it.logs + "RFCOMM Server started (UUID ${MultiplayerProtocol.BLUETOOTH_UUID})").takeLast(16)
                    )
                }
                val sock = ss.accept()
                clientSocket = sock
                ss.close()
                val remoteName = try { sock.remoteDevice.name ?: sock.remoteDevice.address } catch (_: Throwable) { "Bluetooth Peer" }
                _state.update {
                    it.copy(
                        isHosting = false,
                        isConnected = true,
                        connectedDeviceName = remoteName,
                        statusMessage = "Connected over Bluetooth to $remoteName",
                        logs = (it.logs + "Handshake complete with $remoteName").takeLast(16)
                    )
                }
                val writer = PrintWriter(sock.outputStream, true)
                val reader = BufferedReader(InputStreamReader(sock.inputStream))
                writer.println(MultiplayerProtocol.buildWelcomePacket(1, "BT-ROOM", "twenty_nine", "bt-session"))
                while (isActive && sock.isConnected) {
                    val line = reader.readLine() ?: break
                    _state.update { st -> st.copy(logs = (st.logs + "BT recv: ${line.take(60)}").takeLast(16)) }
                }
            } catch (e: Throwable) {
                if (isActive) {
                    _state.update {
                        it.copy(
                            isHosting = false,
                            isConnected = false,
                            statusMessage = "Bluetooth server ended: ${e.message}",
                            logs = (it.logs + "BT Server: ${e.message}").takeLast(16)
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPairedDevice(macAddress: String, playerName: String) {
        refreshStatus()
        val adapter = bluetoothAdapter ?: return
        if (!hasRequiredPermissions() || !adapter.isEnabled) return
        stop()

        ioJob = scope.launch {
            try {
                val device = adapter.getRemoteDevice(macAddress)
                val sock = device.createRfcommSocketToServiceRecord(MultiplayerProtocol.BLUETOOTH_UUID)
                clientSocket = sock
                _state.update {
                    it.copy(
                        statusMessage = "Connecting RFCOMM to ${device.name ?: macAddress}...",
                        logs = (it.logs + "Dialing ${device.name ?: macAddress}...").takeLast(16)
                    )
                }
                sock.connect()
                val writer = PrintWriter(sock.outputStream, true)
                val reader = BufferedReader(InputStreamReader(sock.inputStream))
                writer.println(MultiplayerProtocol.buildHelloPacket(playerName, "", "bt-client"))
                val first = reader.readLine()
                val devName = device.name ?: macAddress
                _state.update {
                    it.copy(
                        isConnected = true,
                        connectedDeviceName = devName,
                        statusMessage = "Connected to $devName over Bluetooth Classic!",
                        logs = (it.logs + "Connected! ${first?.take(50) ?: ""}").takeLast(16)
                    )
                }
                while (isActive && sock.isConnected) {
                    val line = reader.readLine() ?: break
                    _state.update { st -> st.copy(logs = (st.logs + "BT host: ${line.take(60)}").takeLast(16)) }
                }
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isConnected = false,
                        statusMessage = "Bluetooth connect failed: ${e.message}",
                        logs = (it.logs + "BT connect error: ${e.message}").takeLast(16)
                    )
                }
            }
        }
    }

    fun stop() {
        ioJob?.cancel()
        try { serverSocket?.close() } catch (_: Throwable) {}
        try { clientSocket?.close() } catch (_: Throwable) {}
        serverSocket = null; clientSocket = null
        _state.update { it.copy(isHosting = false, isConnected = false, connectedDeviceName = "") }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
