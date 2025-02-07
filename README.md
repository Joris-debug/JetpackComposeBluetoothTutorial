# Official tutorial/documentation for the BluetoothRepository class by Joris-debug
## About
This class provides a medium-level interface for Bluetooth functionality in your Android application.
It follows the repository design pattern, which gives you an abstraction layer and centralizes all Bluetooth operations.

Follow the tutorial to quickly set up and use the API to integrate Bluetooth into your app.
## Getting Started
### Obtain The BluetoothRepository Class
To begin using the `BluetoothRepository` class, download the [`BluetoothRepository.kt`](./app/src/main/java/com/example/easybluetooth/data/BluetoothRepository.kt) file from the GitHub repository.
Navigate to the `data` folder in the repository, download the file, and then move it into your project's `data` folder.
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
2. Request the permissions from the user during runtime:

   As their name implies, runtime permissions must be obtained during runtime.
   You can do this in several ways, but I won't cover those methods here.

## All Set
With everything configured, you're now ready to add Bluetooth communication to your project.

To continue, head over to the next section: [Communicate With Devices](./communicate.md).