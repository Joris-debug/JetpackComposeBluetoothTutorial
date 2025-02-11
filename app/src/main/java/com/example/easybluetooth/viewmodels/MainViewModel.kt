package com.example.easybluetooth.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easybluetooth.data.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject internal constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {
    private var isClient: Boolean = false // Used for prefixing messages in the chat
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    private val messagesFlow: StateFlow<List<String>> = _messages
    private val _chatConnected = MutableStateFlow(false)
    private val chatConnectedFlow: StateFlow<Boolean> = _chatConnected

    fun getChatConnectedFlow(): StateFlow<Boolean> {
        return chatConnectedFlow
    }

    fun getMessagesFlow(): StateFlow<List<String>> {
        return messagesFlow
    }

    // This function is called only when a connection is established,
    // ensuring that the necessary permissions are already granted.
    @SuppressLint("MissingPermission")
    private fun addMessage(newMessage: String, fromClient: Boolean) {
        _messages.value += (if (fromClient) "Client: " else "Server: ") + newMessage
    }

    fun startBluetoothServer() {
        viewModelScope.launch {
            bluetoothRepository.listenOnServerSocket()
            isClient = false
            _chatConnected.value = true
            withContext(Dispatchers.IO) {
                readMessages()
            }
        }
    }

    fun stopBluetoothServer() {
        bluetoothRepository.cancelListenOnServerSocket()
    }

    fun enableDiscoverability() = bluetoothRepository.enableBluetoothDiscoverability()

    fun getNecessaryPermissions() = bluetoothRepository.getNecessaryPermissions()

    fun startClientConnection(dev: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothRepository.connectFromClientSocket(dev)
            isClient = true
            _chatConnected.value = true
            withContext(Dispatchers.IO) {
                readMessages()
            }
        }
    }

    fun sendPing() {
        viewModelScope.launch {
            bluetoothRepository.write(PING)
            addMessage(PONG, isClient)
        }
    }

    fun sendPong() {
        viewModelScope.launch {
            bluetoothRepository.write(PONG)
            addMessage(PONG, isClient)
        }
    }

    fun endChat() {
        _chatConnected.value = false
        if (!bluetoothRepository.isConnected()) {
            return
        }
        viewModelScope.launch {
            bluetoothRepository.write(BYE)
            bluetoothRepository.closeConnection()
        }
    }

    private suspend fun readMessages() {
        while (_chatConnected.value) {
            if (!bluetoothRepository.isDataAvailable()) {
                continue
            }
            val mes = bluetoothRepository.read()
            addMessage(mes, !isClient)
            if (mes == BYE) {
                bluetoothRepository.closeConnection()
                break
            }
        }
    }

    fun startBluetoothSearch() {
        viewModelScope.launch {
            bluetoothRepository.startScanning()
            delay(SCAN_DURATION)
            bluetoothRepository.stopScanning()
        }
    }

    fun getDeviceFlow() = bluetoothRepository.getDiscoveredDevicesFlow()

    companion object {
        const val SCAN_DURATION = 8000L
        const val PING = "Ping"
        const val PONG = "Pong"
        const val BYE = "Bye"
    }
}