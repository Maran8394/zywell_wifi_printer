import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'wifi_printer_method_channel.dart';

abstract class WifiPrinterPlatform extends PlatformInterface {
  /// Constructs a WifiPrinterPlatform.
  WifiPrinterPlatform() : super(token: _token);

  static final Object _token = Object();

  static WifiPrinterPlatform _instance = MethodChannelWifiPrinter();

  /// The default instance of [WifiPrinterPlatform] to use.
  ///
  /// Defaults to [MethodChannelWifiPrinter].
  static WifiPrinterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [WifiPrinterPlatform] when
  /// they register themselves.
  static set instance(WifiPrinterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
