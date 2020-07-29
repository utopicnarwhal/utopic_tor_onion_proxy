# Tor Onion Proxy For Flutter (Android only)

## What is this?
This is a Flutter Plugin for Android, based on code from [Tor Onion Proxy Library](https://github.com/thaliproject/Tor_Onion_Proxy_Library).

Check out the example app included in this repo for reference on how to use this library with curl (the only way to connect through SOCKS4a proxy).

## Currently used Tor version

This is built using
* latest tor for May 1, 2020 (0.4.2.7a)

[All releases](https://github.com/guardianproject/gpmaven/tree/master/org/torproject/tor-android-binary)

## Preview

![123](https://user-images.githubusercontent.com/8808766/87232488-23387700-c3c8-11ea-9db1-ae2b4ba9b173.gif)

## How do I use this plugin?

### 1. add dependencies into you project pubspec.yaml file
``` dart
dependencies:
    utopic_tor_onion_proxy: ^0.1.2
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
