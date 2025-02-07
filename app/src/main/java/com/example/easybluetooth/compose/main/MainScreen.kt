package com.example.easybluetooth.compose.main

import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.easybluetooth.viewmodels.MainViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    var showServerDialog by remember { mutableStateOf(false) }
    var showClientDialog by remember { mutableStateOf(false) }
    val permissionGranted = remember { mutableStateOf(false) }

    // Launcher responsible for obtaining the runtime permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted.value = permissions.all { it.value }
    }

    // Get the necessary permission and check if their are already fulfilled
    val permissionsToRequest = viewModel.getNecessaryPermissions()
    val context = LocalContext.current
    var permissionsNeeded = permissionsToRequest.filter {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) != PackageManager.PERMISSION_GRANTED
    }
    if (permissionsNeeded.isEmpty()) {
        permissionGranted.value = true
    }

    val devices = viewModel.getDeviceFlow().collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Welcome on the main screen!")

        // Button responsible for asking for the runtime permissions
        Button(
            onClick = {
                permissionsNeeded = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }
                if (permissionsNeeded.isEmpty()) {
                    permissionGranted.value = true
                } else {
                    launcher.launch(permissionsNeeded.toTypedArray())
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Request runtime permissions")
        }

        Button(
            onClick = {
                if (!permissionGranted.value) {
                    return@Button
                }
                viewModel.enableDiscoverability()
                viewModel.startBluetoothServer()
                showServerDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Be the Bluetooth server")
        }

        Button(
            onClick = {
                if (!permissionGranted.value) {
                    return@Button
                }
                viewModel.startBluetoothSearch()
                showClientDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Be the Bluetooth client")
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = {
                showServerDialog = false
                viewModel.stopBluetoothServer()
            },
            title = { Text("Server Dialog") },
            text = { Text("You are now listening to connections...") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showServerDialog = false
                        viewModel.stopBluetoothServer()
                    }
                ) {
                    Text("Close server")
                }
            },
        )
    }

    if (showClientDialog) {
        AlertDialog(
            onDismissRequest = {
                showClientDialog = false
            },
            title = { Text("Client Dialog") },
            text = {
                Column {
                    Text("You are now looking for devices in your area")
                    if (devices.value.isEmpty()) {
                        Text("No devices found.")
                    } else {
                        devices.value.forEach { dev ->
                            Text(text = "Name: ${dev.name ?: "Unknown"}, Address: ${dev.address}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClientDialog = false
                    }
                ) {
                    Text("Close client")
                }
            },
        )
    }
}
