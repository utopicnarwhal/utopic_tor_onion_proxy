#import "UtopicTorOnionProxyPlugin.h"
#if __has_include(<utopic_tor_onion_proxy/utopic_tor_onion_proxy-Swift.h>)
#import <utopic_tor_onion_proxy/utopic_tor_onion_proxy-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "utopic_tor_onion_proxy-Swift.h"
#endif

@implementation UtopicTorOnionProxyPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftUtopicTorOnionProxyPlugin registerWithRegistrar:registrar];
}
@end
