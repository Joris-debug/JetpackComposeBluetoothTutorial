package com.example.easybluetooth.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easybluetooth.data.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject internal constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    fun startBluetoothServer() {
        viewModelScope.launch {
            bluetoothRepository.listenOnServerSocket()
        }
    }

    fun stopBluetoothServer() {
        bluetoothRepository.cancelListenOnServerSocket()
    }

    fun enableDiscoverability() = bluetoothRepository.enableBluetoothDiscoverability()

    fun getNecessaryPermissions() = bluetoothRepository.getNecessaryPermissions()

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
    }
}