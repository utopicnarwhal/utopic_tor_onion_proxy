import 'dart:async';

import 'package:flutter/services.dart';

class UtopicTorOnionProxy {
  static const MethodChannel _channel =
      const MethodChannel('utopic_tor_onion_proxy');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
