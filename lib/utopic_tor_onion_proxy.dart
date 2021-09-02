import 'dart:async';

import 'package:flutter/services.dart';

class UtopicTorOnionProxy {
  static const MethodChannel _channel =
      const MethodChannel('utopic_tor_onion_proxy');

  /// This is a blocking call that will try to start the Tor OP, connect it to the network and get it to be fully bootstrapped.
  /// Sometimes the bootstrap process just hangs for no apparent reason so the method will wait for a minute for bootstrap to finish.
  /// Returns the socks port on the IPv4 localhost address that the Tor OP is listening on.
  static Future<int?> startTor() async {
    final int? port = await _channel.invokeMethod('startTor');
    return port;
  }

  /// Checks to see if the Tor OP is running (e.g. fully bootstrapped) and open to network connections.
  static Future<bool?> isTorRunning() async {
    return _channel.invokeMethod<bool>('isTorRunning');
  }

  /// Kills the Tor OP Process. Once you have called this method nothing is going to work until you either call [startTor].
  static Future<bool?> stopTor() async {
    return _channel.invokeMethod<bool>('stopTor');
  }
}
