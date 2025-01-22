# Dependency Injection With Hilt
## About
Hilt is a library for Android applications that helps you reduce boilerplate code in your project by automatically handling the creation and injection of dependencies,
making your code more modular and easier to maintain.

In this tutorial, we will configure your project to use Hilt for dependency injection,
with the goal of providing your ViewModels with an instance of the `BluetoothRepository` class.
## Adding Dependencies
Begin by adding the `hilt-android-gradle-plugin` plugin to your project-level build.gradle file:
```kotlin
plugins {
  //...
  id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
```
Afterwards, add the following dependecies to your app-level build.gradle file:
```kotlin
plugins {
    //...
  id("kotlin-kapt")
  id("com.google.dagger.hilt.android")
}

android {
    //...
}

dependencies {
    //...
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
```
### Note
Hilt requires Java 8 features,
so ensure that you have set both `sourceCompatibility` and `targetCompatibility` to at least Java 8 in your app-level build.gradle file.
## Create An Application Class
Hilt requires that your project contains an Application class annotated with `@HiltAndroidApp`
It tells Hilt to generate the necessary code to manage and inject dependencies into Android components throughout your app.
In the following code, I've created an `Application` class and arbitrarily named it `MainApplication`:
```kotlin
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
}
```
Place this class file in the **root** package of your app (typically in `src/main/java/com/*YOUR_APP*/`).
## Annotate Activity
Hilt provides dependencies to Android classes annotated with `@AndroidEntryPoint`.
Be sure to add this annotation to your activity as shown in the following example for `MainActivity`:
```kotlin
//...
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Add this annotation
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //...
        }
    }
}
```
### Note
Make sure to add the android:name=".MainApplication" entry inside the <application> tag in your AndroidManifest.xml file
to register your Application class:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".MainApplication"  <!-- Add this line -->
        <!-- ... -->
    </application>
</manifest>
```
## Start Injecting
Now that everything is set up, you can begin injecting the `BluetoothRepository` into your `ViewModel`.
### Steps to Inject the BluetoothRepository into the ViewModel:
1. **Extend ViewModel**: Ensure that your custom `ViewModel` class extends `ViewModel`.
2. **Add Annotations**:
    - Add the `@HiltViewModel` annotation to your `ViewModel` class.
    - Add the `@Inject` annotation to the constructor of the `ViewModel` to allow Hilt to inject dependencies.
3. **Inject the Repository**: Add the `BluetoothRepository` as a parameter in the constructor of the `ViewModel`.

Here’s the full code for my custom `MainViewModel` class:
```kotlin
@HiltViewModel[README.md](README.md)
class MainViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {
    // Your code goes here
}
```
## Injecting ViewModel into Composable
You can also provide your composable functions access to the `ViewModel` through dependency injection.
Simply add the following parameter to your function:
```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    //...
) {
   // Your code goes here
}
```
# All Set
you have followed all the steps correctly, your dependency injection should now be set up and working.
You can now return to the main [tutorial](./README.md) to continue with the `BluetoothRepository` class.