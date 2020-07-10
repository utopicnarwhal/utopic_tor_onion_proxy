import 'dart:async';

import 'package:flutter/services.dart';

class UtopicTorOnionProxy {
  static const MethodChannel _channel =
      const MethodChannel('utopic_tor_onion_proxy');

  static Future<String> startTor() async {
    final String port = await _channel.invokeMethod('startTor');
    return port;
  }
}
