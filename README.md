# Official tutorial/documentation for the BluetoothRepository class by Joris-debug
## About
This class provides a medium-level interface for Bluetooth functionality in your Android application.
It follows the repository design pattern, which gives you an abstraction layer and centralizes all Bluetooth operations.

Follow the tutorial to quickly set up and use the API to integrate Bluetooth into your app.
## Getting Started
### Version Requirement
The `BluetoothRepository` class requires a minimum SDK version of 31. Using a lower version may result in undefined behavior.
Be sure to check your `minSdk` setting in your app-level build.gradle file:
```kotlin
android {
   // ...

   defaultConfig {
      // ...
      minSdk = 31  // Ensure this is 31 or higher
      // ...
   }
}
```
### Dependency Injection
If you wish to utilize my `BluetoothRepository` class, it is recommended (but not required) to implement dependency injection using Hilt.
If you haven't set it up yet but would like to, check out [this tutorial](./di-tutorial.md) to quickly get everything configured.
### Permissions
For the use Bluetooth functionality you are required to obtain the `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` and `BLUETOOTH_ADVERTISE` permissions.
These are runtime permissions and require two steps:
1. Add the following entries to your AndroidManifest.xml file:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools">
      <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />   <!-- 1. -->
      <uses-permission android:name="android.permission.BLUETOOTH_SCAN"         
              android:usesPermissionFlags="neverForLocation"/>                  <!-- 2. -->      
      <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" /> <!-- 3. -->
      
      <!-- ... -->
      
   </manifest>
   ```
2. Request the permissions from the user during runtime.
   As their name implies, runtime permissions must be obtained during runtime.
   You can do this in several ways, but I won't cover those methods here.
## Discover Devices
If you wish to transfer data, you need to know which device should receive the information.
There are currently two ways to obtain devices:
1. Iterate over the list of already paired devices:
   You can obtain a set of paired devices with `getPairedDevices()`.
   However, there is no guarantee that the device you're searching for is already paired with the device your app is running on.
   Additionally, paired devices may not be in proximity to your device.
2. Discover nearby devices:
   To do so, start discovering devices by calling the function `startBluetoothScan()`.
   Bluetooth scans take about 12 seconds. Be sure to call `stopBluetoothScan()` after you’re done discovering devices.
   All devices that were found can be obtained by calling `getDiscoveredDevices()`.
### Note
`getPairedDevices()` as well as `getDiscoveredDevices()` will give you access to instances of BluetoothDevice.
You will need the desired device object for the next step.
For the discovery process to work, Bluetooth must be enabled.
You can only discover devices that have made themselves discoverable in the system settings.
This can typically be done through a prompt, similar to the process of enabling Bluetooth.
Additionally, you need the following permissions:
* `BLUETOOTH_ADVERTISE`
* `BLUETOOTH_SCAN`
* `BLUETOOTH_CONNECT`

These permissions are required for all the remaining steps.
## Start a Connection
To start a connection, one device will act as the server, while the other will act as the client.  
A device can become a server by calling the function `listenOnServerSocket()`.  
You can check if a client has successfully connected to the server by calling `isConnected()`, which returns `true` in that case, and `false` otherwise.

The client, on the other hand, needs to call the function `connectFromClientSocket(device)`.  
`device` should be the instance of `BluetoothDevice` discussed in the **Discover Devices** section.
If done correctly, a connection should now be established.
### Note
Keep in mind that `listenOnServerSocket()` is a blocking call, terminating once a client connects.
## Transmit Data
Everything we have done so far has led us to the most important aspect of a Bluetooth connection:  
sharing data between the devices.  
To do so, you can call the functions `write(message)` and `read()`.  
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