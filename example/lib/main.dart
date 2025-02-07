import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:wifi_printer/wifi_printer.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _wifiPrinterPlugin = WifiPrinter();
  TextEditingController ip = TextEditingController(text: "192.168.1.9");
  @override
  void initState() {
    super.initState();
    _wifiPrinterPlugin.events.listen((event) {
      print("${event['statusCode']}: ${event['message']}");
    });
  }

  void _printImage() async {
    ByteData imageData =
        await DefaultAssetBundle.of(context).load("assets/sample.png");
    Uint8List imageBytes = imageData.buffer.asUint8List();

    // await _wifiPrinterPlugin.printBitmap(imageBytes);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin doublelogic app'),
        ),
        body: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            TextField(controller: ip),
            ElevatedButton(
              child: const Text("Connect"),
              onPressed: () async {
                Map<String, dynamic> platformVersion =
                    await _wifiPrinterPlugin.connectPrinter(ip.text);
                print(platformVersion);
              },
            ),
            ElevatedButton(
              child: const Text("PrintBitMap"),
              onPressed: () async {
                _printImage();
                // await _wifiPrinterPlugin.printSample();
              },
            ),
            ElevatedButton(
              child: const Text("Print Sample"),
              onPressed: () async {
                await _wifiPrinterPlugin.printRowData(
                  rowData: {
                    "no": "1",
                    "service_name": "Haircut(1)",
                    "amount": "RM 20"
                  },
                );
              },
            ),
            ElevatedButton(
              child: const Text("Status"),
              onPressed: () async {
                await _wifiPrinterPlugin.getPrinterStatus();
              },
            ),
          ],
        ),
      ),
    );
  }
}
