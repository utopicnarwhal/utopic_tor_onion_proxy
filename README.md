# Tor Onion Proxy For Flutter (Android only)

## What is this?
This is a Flutter Plugin for Android, based on code from [Tor Onion Proxy Library](https://github.com/thaliproject/Tor_Onion_Proxy_Library) and [Tor Android](https://github.com/guardianproject/tor-android).

Check out the example app included in this repo for reference on how to use this library with sockets.

## Currently used Tor version

This is built using tor 0.4.3.6

[All releases](https://github.com/guardianproject/gpmaven/tree/master/org/torproject/tor-android-binary)

## Preview

![123](https://user-images.githubusercontent.com/8808766/87232488-23387700-c3c8-11ea-9db1-ae2b4ba9b173.gif)

## How do I use this plugin?

### Preparation (for Android App Bundling)
1. Add `android.bundle.enableUncompressedNativeLibs=false` to your `android -> gradle.properties` file.
```
org.gradle.jvmargs=-Xmx1536M
...
android.bundle.enableUncompressedNativeLibs=false
```
2. Add `android:extractNativeLibs="true"` to your `AndroidManifest.xml`.
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="...">

    ...

    <application
        ...
        android:extractNativeLibs="true">
        ...
    </application>

    ...
</manifest>
```
3. Add shrink, zip aligned and minify settings into your `android/app/build.gradle` file.
```
android {
    compileSdkVersion 29

    defaultConfig {
        ...
        minSdkVersion 16
        targetSdkVersion 29
    }

    buildTypes {
        ...
        release {
            signingConfig signingConfigs.debug
            shrinkResources false
            zipAlignEnabled false
            minifyEnabled false
            ...
        }
    }
}
```
4. Use `--no-shrink` param in build appbundle command.
```
flutter build appbundle --no-shrink ...
```

### 1. add dependencies into you project pubspec.yaml file
``` dart
dependencies:
    utopic_tor_onion_proxy: version_number
```

### 2. import lib
``` dart
import 'package:utopic_tor_onion_proxy/utopic_tor_onion_proxy.dart';
```

### 3. start Tor Onion Proxy
``` dart
try {
    port = await UtopicTorOnionProxy.startTor();
} on PlatformException catch (e) {
    print('Failed to get port. Message: ${e.message}');
}
```

### 4. check is Tor OP running
``` dart
try {
    await UtopicTorOnionProxy.isTorRunning();
} on PlatformException catch (e) {
    print('Failed to get is tor running. Message: ${e.message}');
}
```

### 5. stop Tor Onion Proxy when done
``` dart
try {
    await UtopicTorOnionProxy.stopTor();
} on PlatformException catch (e) {
    print('Failed to stop tor. Message: ${e.message}');
}
```
