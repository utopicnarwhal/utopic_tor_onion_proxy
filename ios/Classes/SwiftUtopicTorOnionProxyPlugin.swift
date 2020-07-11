import Flutter
import UIKit

public class SwiftUtopicTorOnionProxyPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "utopic_tor_onion_proxy", binaryMessenger: registrar.messenger())
    let instance = SwiftUtopicTorOnionProxyPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS is not supported")
  }
}
