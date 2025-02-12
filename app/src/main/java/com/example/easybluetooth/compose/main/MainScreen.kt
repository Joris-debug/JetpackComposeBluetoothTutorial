package com.example.easybluetooth.compose.main

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
    val messages = viewModel.getMessagesFlow().collectAsState()
    val chatConnected = viewModel.getChatConnectedFlow().collectAsState()

    // Intents used to request the user activate Bluetooth or make his device discoverable
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val discoverableDuration = 60 // How long will the device be discoverable
    val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverableDuration)
    }
    discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    Column(modifier = modifier.padding(16.dp)) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Request runtime permissions")
        }

        Button(
            onClick = {
                if (!permissionGranted.value) {
                    return@Button
                }
                if (!viewModel.isBluetoothEnabled()) { // User did not activate Bluetooth
                    context.startActivity(enableBtIntent)
                    return@Button
                }
                context.startActivity(discoverableIntent)
                viewModel.startBluetoothServer()
                showServerDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Be the Bluetooth server")
        }

        Button(
            onClick = {
                if (!permissionGranted.value) {
                    return@Button
                }
                if (!viewModel.isBluetoothEnabled()) { // User did not activate Bluetooth
                    context.startActivity(enableBtIntent)
                    return@Button
                }
                viewModel.startBluetoothSearch()
                showClientDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Be the Bluetooth client")
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = {
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
            },
            title = { Text("Client Dialog") },
            text = {
                Column {
                    Text("You are now looking for devices in your area")
                    if (devices.value.isEmpty()) {
                        Text("No devices found.")
                    } else {
                        devices.value.forEach { dev ->
                            Text(
                                text = "Name: ${dev.name ?: "Unknown"}, Address: ${dev.address}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.startClientConnection(dev)
                                        showClientDialog = false
                                    }
                                    .padding(8.dp)
                            )
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
    if (chatConnected.value) {
        AlertDialog(
            onDismissRequest = {
            },
            title = { Text("Chatting") },
            text = {
                Column {
                    messages.value.forEach { mes ->
                        Text(text = mes)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showServerDialog = false // Could still be open in the background
                        viewModel.endChat()
                    }
                ) {
                    Text("End chat")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.sendPing()
                        }
                    ) {
                        Text("Ping")
                    }

                    TextButton(
                        onClick = {
                            viewModel.sendPong()
                        }
                    ) {
                        Text("Pong")
                    }
                }
            }
        )
    }
}
