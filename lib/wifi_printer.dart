import 'dart:async';
import 'dart:developer';

import 'package:flutter/services.dart';

import 'wifi_printer_platform_interface.dart';

class WifiPrinter {
  static const MethodChannel _channel = MethodChannel('wifi_printer');
  Stream<Map<String, dynamic>> get events => _eventStreamController.stream;
  static final StreamController<Map<String, dynamic>> _eventStreamController =
      StreamController.broadcast();

  WifiPrinter() {
    _channel.setMethodCallHandler(_handleNativeMethodCall);
  }

  Future<void> _handleNativeMethodCall(MethodCall call) async {
    final Map<String, dynamic> result =
        Map<String, dynamic>.from(call.arguments);
    _eventStreamController.add(result);
  }

  Future<String?> getPlatformVersion() {
    return WifiPrinterPlatform.instance.getPlatformVersion();
  }

  Future<Map<String, dynamic>> connectPrinter(String ipAddress) async {
    try {
      Map<dynamic, dynamic> result =
          await _channel.invokeMethod('connectPrinter', {'ip': ipAddress});
      Map<String, dynamic> dataMap = Map<String, dynamic>.from(result);
      return dataMap;
    } on Exception {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> disconnectPrinter(String ipAddress) async {
    try {
      Map<dynamic, dynamic> result =
          await _channel.invokeMethod('disconnectPrinter');
      Map<String, dynamic> dataMap = Map<String, dynamic>.from(result);
      return dataMap;
    } on Exception {
      rethrow;
    }
  }

  Future<void> printCut() async {
    try {
      await _channel.invokeMethod("printCut");
    } on Exception {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getPrinterStatus() async {
    try {
      Map<dynamic, dynamic> result =
          await _channel.invokeMethod('getPrinterStatus');
      Map<String, dynamic> dataMap = Map<String, dynamic>.from(result);
      log(dataMap.toString());
      return dataMap;
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printImage(String base64Image) async {
    try {
      await _channel.invokeMethod('printImage', {"image": base64Image});
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printRowData(
      {required Map<String, String> rowData, int? fontSize}) async {
    try {
      await _channel.invokeMethod(
        'printRowData',
        {
          "rowData": rowData,
          "fontSize": fontSize ?? 10,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printText({
    required String text,
    int? fontSize,
    int? left,
    int? right,
    int? isBold,
  }) async {
    try {
      await _channel.invokeMethod(
        'printText',
        {
          "text": text,
          "fontSize": fontSize ?? 5,
          "left": left ?? 0,
          "right": right ?? 60,
          "isBold": isBold ?? 0,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> cutPaper() async {
    try {
      await _channel.invokeMethod('cutPaper');
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printEmptyline({int? lineCount}) async {
    try {
      await _channel.invokeMethod(
        'printEmptyLine',
        {
          "lineCount": lineCount ?? 1,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printColData({
    required String text1,
    required String text2,
    int? leftMargin,
    int? leftAlign,
    int? rightMargin,
    int? rightAlign,
    int? leftBold,
    int? rightBold,
  }) async {
    try {
      await _channel.invokeMethod(
        'printColData',
        {
          "text1": text1,
          "text2": text2,
          "leftMargin": leftMargin,
          "rightMargin": rightMargin,
          "leftAlign": leftAlign,
          "rightAlign": rightAlign,
          "leftBold": leftBold,
          "rightBold": rightBold,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printNormalText({required String text, int? align}) async {
    try {
      await _channel.invokeMethod(
        'printNormalText',
        {
          "text": text,
          "align": align ?? 0,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printTitle(
      {required String text, int? align, int? fontSize, int? bold}) async {
    try {
      await _channel.invokeMethod(
        'printTitle',
        {
          "text": text,
          "align": align ?? 0,
          "fontSize": fontSize ?? 17,
          "bold": bold ?? 0,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printTwoColumnData(
      {required Map<String, String> rowData}) async {
    try {
      await _channel.invokeMethod('printTwoColumnData', {"rowData": rowData});
    } catch (e) {
      rethrow;
    }
  }

  Future<void> printThreeColumnData(
      {required Map<String, String> rowData}) async {
    try {
      await _channel.invokeMethod('printThreeColumnData', {"rowData": rowData});
    } catch (e) {
      rethrow;
    }
  }
}
