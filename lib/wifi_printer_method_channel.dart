import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'wifi_printer_platform_interface.dart';

/// An implementation of [WifiPrinterPlatform] that uses method channels.
class MethodChannelWifiPrinter extends WifiPrinterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('wifi_printer');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
