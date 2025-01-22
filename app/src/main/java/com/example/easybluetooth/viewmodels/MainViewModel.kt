package com.example.easybluetooth.viewmodels

import androidx.lifecycle.ViewModel
import com.example.easybluetooth.data.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject internal constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {
}