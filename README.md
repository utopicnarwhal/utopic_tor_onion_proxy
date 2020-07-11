# Tor Onion Proxy For Flutter (Android only)

## What is this?
This is a Flutter Plugin for Android, based on [this](https://github.com/jehy/Tor-Onion-Proxy-Library) Android library.

Check out the example app included in this repo for reference on how to use this library with curl.

## Currently used Tor version

This is built using
* latest tor for jan 5, 2018 (0.3.1.9)

## Preview

![123](https://user-images.githubusercontent.com/8808766/87232488-23387700-c3c8-11ea-9db1-ae2b4ba9b173.gif)

## How do I use this plugin?

### 1. add dependencies into you project pubspec.yaml file
``` dart
dependencies:
    utopic_tor_onion_proxy: ^0.1.0
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

### 4. stop Tor Onion Proxy when done
``` dart
try {
    await UtopicTorOnionProxy.stopTor();
} on PlatformException catch (e) {
    print('Failed to stop tor. Message: ${e.message}');
}
```
