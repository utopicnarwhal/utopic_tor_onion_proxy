import 'dart:io';

import 'package:dio/adapter.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:http_client/curl.dart';
import 'package:utopic_tor_onion_proxy/utopic_tor_onion_proxy.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _port = 'Unknown';

  @override
  void initState() {
    super.initState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> startTor() async {
    String port;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      port = await UtopicTorOnionProxy.startTor();
    } on PlatformException catch (e) {
      print(e.message ?? '');
      port = 'Failed to get port.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _port = port;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text('Running on: $_port\n'),
              SizedBox(height: 20),
              OutlineButton(
                child: Text('Запрос'),
                onPressed: () async {
                  Socket sock;
                  try {
                    sock = await Socket.connect(
                      InternetAddress.loopbackIPv4,
                      int.parse(_port),
                      timeout: Duration(seconds: 6),
                    );

                    List<int> ipAddressBytes = 'check.torproject.org'.runes.toList();

                    sock.add([0x04, 0x01, 0x00, 0x50, 0x00, 0x00, 0x00, 0x01, 0x00, ...ipAddressBytes, 0x00]);

                    sock.listen((torSocketResponse) {
                      print(torSocketResponse.toList());
                    });
                  } catch (e) {
                    sock?.destroy();
                    print(false);
                  }

                  var dio = new Dio();
                  (dio.httpClientAdapter as DefaultHttpClientAdapter)
                      .onHttpClientCreate = (HttpClient client) {
                    return client;
                  };
                  var response = await dio.get('${InternetAddress.loopbackIPv4.host}:$_port');
                  print(response.data);
                  dio.close();

                  // var client = CurlClient(
                  //   socksHostPort: '127.0.0.1:$_port',
                  // );
                  // var response = await client.send(Request(
                  //   'GET',
                  //   'http://flibustahezeous3.onion',
                  //   timeout: Duration(seconds: 6),
                  // ));
                  // final textContent = await response.readAsString();
                  // print(textContent);
                  // await client.close();
                },
              ),
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.local_activity),
          onPressed: () {
            startTor();
          },
        ),
      ),
    );
  }
}
