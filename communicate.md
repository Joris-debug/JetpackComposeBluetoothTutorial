# Communicate With Devices
## Discover Devices
In the example below, bluetoothRepository should be the instance of BluetoothRepository provided by your [dependency injection](./di-tutorial.md) setup.

If you wish to transfer data, you need to know which device should receive the information.
There are currently two ways to obtain devices: 
1. You can obtain a set of paired devices with `getPairedDevices()`:
   ```kotlin
   Set<BluetoothDevice> bondedDevices = bluetoothRepository.getPairedDevices()
   ```
   However, there is no guarantee that the device you're searching for is already paired with the device your app is running on.
   Additionally, paired devices may not be in proximity to your device.

2. Discover nearby devices:
   ```kotlin
   bluetoothRepository.startBluetoothScan()
   delay(12000) // Wait for 12 seconds
   bluetoothRepository.stopBluetoothScan()
   Set<BluetoothDevice> disscoveredDevices = bluetoothRepository.getDiscoveredDevices()
   ```
   Start discovering devices by calling the function `startBluetoothScan()`.
   Bluetooth scans take about 12 seconds. Be sure to call `stopBluetoothScan()` after you’re done discovering devices.
   All devices that were found can be obtained by calling `getDiscoveredDevices()`.
### Note
`getPairedDevices()` as well as `getDiscoveredDevices()` will give you access to instances of BluetoothDevice.
You will need the desired device object for the next step.

For the discovery process to work, Bluetooth must be enabled.
You can only discover devices that have made themselves discoverable in the system settings.
This can typically be done through a prompt, similar to the process of enabling Bluetooth.
## Start a Connection
To start a connection, one device will act as the server, while the other will act as the client.
1. A device can become a server by calling the function `listenOnServerSocket()`:
   ```kotlin
   viewModelScope.launch {
       bluetoothRepository.listenOnServerSocket()
   }
   viewModelScope.launch { 
      delay(3000) // Wait for a few seconds
      if (bluetoothRepository.isConnected()) { 
          println("Connection established!")
      } else { 
          println("Waiting for connection...")
      }
   }
   ```
   You can check if a client has successfully connected to the server by calling `isConnected()`, which returns `true` in that case, and `false` otherwise.

2. The client, on the other hand, needs to call the function `connectFromClientSocket(device)`:
   ```kotlin
   bluetoothRepository.connectFromClientSocket(device)
   delay(3000) // Wait for a few seconds
   if (bluetoothRepository.isConnected()) { 
       println("Connection established!")
   } else { 
       println("Waiting for connection...")
   }
   ```
`device` should be the instance of `BluetoothDevice` discussed in the **Discover Devices** section.
If done correctly, a connection should now be established.
### Note
Keep in mind that `listenOnServerSocket()` is a blocking call, terminating once a client connects.
## Transmit Data
Everything we have done so far has led us to the most important aspect of a Bluetooth connection:  
sharing data between the devices.
To do so, you can call the functions `write(message)` and `read()`.
```kotlin
bluetoothRepository.write("Hello world!")
if (bluetoothRepository.isDataAvailable()) { 
    String message = bluetoothRepository.read()
    println(message)
}
``` 
`write()` takes a message as a `String`. You can read the message on the receiver side by calling `read()`,  
which reads everything from the buffer and returns it as a `String`.
### Note
`read()` will block until it has successfully read data from the input stream of the socket.
To prevent blocking, you can use the `isDataAvailable()` function first.
It will return `true` if there is data available in the input stream, allowing you to check before calling `read()`.
## Closing a Connection
All good things must come to an end.  
Sooner or later, your devices must part ways.  
To ensure that your repository keeps working as intended, call the function `closeConnection()` on both devices.
```kotlin
bluetoothRepository.closeConnection()
``` 