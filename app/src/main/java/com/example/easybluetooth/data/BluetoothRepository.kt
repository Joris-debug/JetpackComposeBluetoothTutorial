package com.example.easybluetooth.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class BluetoothRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // TODO: handle this error gracefully
    private val bluetooth = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager
        ?: throw Exception("Bluetooth is not supported by this device")

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetooth.adapter

    private val foundDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { dev ->
                        discoveredDevices.add(dev)
                        _discoveredDevicesFlow.value =
                            _discoveredDevicesFlow.value.toMutableSet().apply { add(dev) }
                    }
                }
            }
        }
    }

    private val discoveredDevices: MutableSet<BluetoothDevice> = mutableSetOf()
    private var connectionEstablished = false
    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null
    private val readBuffer: ByteArray = ByteArray(BUFFER_SIZE) // Buffer store for the stream

    private val _discoveredDevicesFlow = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    private val discoveredDevicesFlow: StateFlow<Set<BluetoothDevice>> = _discoveredDevicesFlow

    fun isConnected(): Boolean {
        return connectionEstablished
    }

    fun getDiscoveredDevices(): Set<BluetoothDevice> {
        return discoveredDevices.toSet()
    }

    fun getDiscoveredDevicesFlow(): StateFlow<Set<BluetoothDevice>> {
        return discoveredDevicesFlow
    }

    // Returns true if the device has activated bluetooth
    fun isBluetoothEnabled(): Boolean {
        return bluetooth.adapter.isEnabled
    }

    // Returns true if the app has obtained all runtime permissions
    private fun checkBluetoothPermissions(): Boolean {
        val bluetoothConnectPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        val bluetoothScanPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        val bluetoothAdvertisePermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) == PackageManager.PERMISSION_GRANTED

        return bluetoothConnectPermission && bluetoothScanPermission && bluetoothAdvertisePermission
    }

    fun getNecessaryPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice> {
        if (!checkBluetoothPermissions()) {
            Log.d("getPairedDevices()", "Missing permissions")
            return setOf()
        }
        return bluetoothAdapter?.bondedDevices ?: setOf()
    }

    @SuppressLint("MissingPermission")
    private fun createServerSocket() {
        if (!checkBluetoothPermissions()) {
            Log.e("createServerSocket()", "Missing permissions")
            return
        }
        connectionEstablished = false
        serverSocket =
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
    }

    @SuppressLint("MissingPermission")
    private fun createClientSocket(device: BluetoothDevice) {
        if (!checkBluetoothPermissions()) {
            Log.e("createClientSocket()", "Missing permissions")
            return
        }
        connectionEstablished = false
        socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
    }

    suspend fun listenOnServerSocket() {
        if (serverSocket == null) {
            createServerSocket()
            if (serverSocket == null) {
                // Socket creation was not successful
                return
            }
        }
        // Keep listening until exception occurs or a socket is returned.
        var shouldLoop = true
        withContext(Dispatchers.IO) {
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e("listenOnServerSocket()", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    connectionEstablished = true
                    this@BluetoothRepository.socket = socket // Use fully qualified reference
                    serverSocket?.close()
                    shouldLoop = false
                }
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    fun cancelListenOnServerSocket() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("cancelListenOnServerSocket()", "Could not close the connect socket", e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectFromClientSocket(device: BluetoothDevice) {
        if (!checkBluetoothPermissions()) {
            return
        }
        if (socket == null) {
            createClientSocket(device)
            if (socket == null) {
                // Socket creation was not successful
                return
            }
        }
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter?.cancelDiscovery()

        socket?.let { socket ->
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            try {
                withContext(Dispatchers.IO) {
                    socket.connect()
                }
                // The connection attempt succeeded.
                connectionEstablished = true
            } catch (e: IOException) {
                Log.d("connectFromClientSocket", "Connection failed: ${e.message}", e)
            }
        }
    }

    // Returns true if data is available to read from the input stream of the socket.
    fun isDataAvailable(): Boolean {
        try {
            return socket!!.inputStream.available() > 0
        } catch (e: IOException) {
            Log.d("isDataAvailable()", "Error accessing socket input stream", e)
        } catch (e: NullPointerException) {
            Log.d("isDataAvailable()", "Socket is null", e)
        }
        return false
    }

    suspend fun read(): String {
        var message: String
        try {
            withContext(Dispatchers.IO) {
                val numBytes: Int = socket!!.inputStream.read(readBuffer)
                val data = String(readBuffer, 0, numBytes)
                message = data
            }
        } catch (e: IOException) {
            Log.d("read()", "Input stream was disconnected", e)
            return "Error: ${e.message}"
        }
        return message
    }

    suspend fun write(message: String) {
        val bytes = message.toByteArray()
        try {
            withContext(Dispatchers.IO) {
                socket!!.outputStream.write(bytes)
            }
        } catch (e: IOException) {
            Log.e("write(...)", "Error occurred when sending data", e)
            return
        }
    }

    // Call this method to shut down the connection.
    fun closeConnection() {
        if (!checkBluetoothPermissions()) {
            return
        }
        try {
            socket?.close()
            socket = null
            serverSocket = null
            connectionEstablished = false
        } catch (e: IOException) {
            Log.e("closeConnection()", "Could not close the connect socket", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if(!checkBluetoothPermissions()) {
            return
        }
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        bluetoothAdapter?.startDiscovery()
    }

    // Only for debugging, activities are not supposed to be started here
    @SuppressLint("MissingPermission")
    fun enableBluetoothConnection() {
        if (!isBluetoothEnabled()) {
            if (!checkBluetoothPermissions()) {
                Log.d("enableBluetoothConnection():", "Missing permissions")
                return
            }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableBtIntent)
        }
    }

    // Only for debugging, activities are not supposed to be started here
    @SuppressLint("MissingPermission")
    fun enableBluetoothDiscoverability() {
        if (isBluetoothEnabled()) {
            if (!checkBluetoothPermissions()) {
                Log.d("enableBluetoothDiscoverability():", "Missing permissions")
                return
            }
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(discoverableIntent)
        }
    }

    // Stop Bluetooth discovery after scanning
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (checkBluetoothPermissions() && bluetooth.adapter.isDiscovering) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    companion object {
        const val BUFFER_SIZE = 2048
        const val SERVICE_NAME = "CoinCraze"
        val SERVICE_UUID: UUID = UUID.nameUUIDFromBytes(SERVICE_NAME.toByteArray())
    }
}