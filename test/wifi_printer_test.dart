import 'package:flutter_test/flutter_test.dart';
import 'package:wifi_printer/wifi_printer.dart';
import 'package:wifi_printer/wifi_printer_platform_interface.dart';
import 'package:wifi_printer/wifi_printer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockWifiPrinterPlatform
    with MockPlatformInterfaceMixin
    implements WifiPrinterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final WifiPrinterPlatform initialPlatform = WifiPrinterPlatform.instance;

  test('$MethodChannelWifiPrinter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelWifiPrinter>());
  });

  test('getPlatformVersion', () async {
    WifiPrinter wifiPrinterPlugin = WifiPrinter();
    MockWifiPrinterPlatform fakePlatform = MockWifiPrinterPlatform();
    WifiPrinterPlatform.instance = fakePlatform;

    expect(await wifiPrinterPlugin.getPlatformVersion(), '42');
  });
}
