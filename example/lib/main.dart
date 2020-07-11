import 'dart:io';
import 'dart:async';

import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'package:utopic_tor_onion_proxy/utopic_tor_onion_proxy.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _port;
  String _responseString;

  Future<void> startTor() async {
    String port;
    try {
      port = await UtopicTorOnionProxy.startTor();
    } on PlatformException catch (e) {
      print(e.message ?? '');
      port = 'Failed to get port';
    }

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
          title: const Text('Tor Onion Proxy example'),
        ),
        body: LayoutBuilder(builder: (context, constrains) {
          return Scrollbar(
            child: SingleChildScrollView(
              child: Container(
                constraints: BoxConstraints(minHeight: constrains.maxHeight),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    SizedBox(height: 20),
                    Text('Tor running on: ${_port ?? 'Unknown'} port'),
                    SizedBox(height: 20),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Wrap(
                        runSpacing: 20,
                        spacing: 20,
                        children: <Widget>[
                          OutlineButton(
                            child: Text('Start Tor Onion Proxy'),
                            onPressed: startTor,
                          ),
                          OutlineButton(
                            child: Text('Send request to check.torproject.org'),
                            onPressed: _port != null
                                ? () async {
                                    final args = <String>[];
                                    args.add('-L');

                                    args.add('--max-redirs');
                                    args.add('3');

                                    args.addAll([
                                      '--socks4a',
                                      '${InternetAddress.loopbackIPv4.host}:$_port',
                                    ]);

                                    final method = 'GET';
                                    args.addAll(['-X', method.toUpperCase()]);

                                    args.add(Uri(
                                      scheme: 'HTTPS',
                                      host: 'check.torproject.org',
                                      path: '/',
                                    ).toString());

                                    var prf = Process.run('curl', args,
                                        stdoutEncoding: null);

                                    prf = prf.timeout(Duration(seconds: 6));

                                    final pr = await prf;
                                    final list =
                                        (pr.stdout as List).cast<int>();

                                    if (!mounted) return;
                                    setState(() {
                                      _responseString =
                                          String.fromCharCodes(list).trim();
                                    });
                                  }
                                : null,
                          ),
                        ],
                      ),
                    ),
                    if (_responseString != null)
                      Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Text('Response: \n\n$_responseString'),
                      ),
                  ],
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
