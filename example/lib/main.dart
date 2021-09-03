import 'dart:convert';
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
  String? _torLocalPort;
  String? _error;
  String? _responseString;
  Socket? _socket;

  Future<void> _startTor() async {
    String? port;
    try {
      port = (await UtopicTorOnionProxy.startTor()).toString();
    } on Exception catch (e) {
      print(e);
      _error = 'Failed to get port';
    }

    if (!mounted) return;
    setState(() {
      _torLocalPort = port;
    });
  }

  Future<void> _stopTor() async {
    try {
      if (await (UtopicTorOnionProxy.stopTor() as FutureOr<bool>)) {
        if (!mounted) return;
        setState(() {
          _torLocalPort = null;
        });
      }
    } on PlatformException catch (e) {
      print(e.message ?? '');
    }
  }

  Future<void> _sendGetRequest(Uri uri) async {
    if (mounted) {
      setState(() {
        _responseString = null;
      });
    }
    _socket?.destroy();

    _socket = await Socket.connect(
      InternetAddress.loopbackIPv4,
      int.tryParse(_torLocalPort!)!,
      timeout: Duration(seconds: 5),
    );
    _socket!.setOption(SocketOption.tcpNoDelay, true);

    _socksConnectionRequest(uri, _socket!);

    List<int> responseIntList = [];

    void onSocketDone() {
      print('socket done');
      if (mounted) {
        setState(() {
          _responseString = String.fromCharCodes(responseIntList);
        });
      }
    }

    _socket!.listen((event) async {
      if (event.length == 8 && event[0] == 0x00 && event[1] == 0x5A) {
        print('Connection open');

        if (uri.scheme == 'https') {
          _socket = await SecureSocket.secure(
            _socket!,
            host: uri.authority,
          );
          _socket!.listen((event) {
            responseIntList.addAll(event);
          }).onDone(onSocketDone);
        }

        var requestString = 'GET ${uri.path} HTTP/1.1\r\n'
            'Host: ${uri.authority}\r\n\r\n';
        _socket!.write(requestString);
        return;
      }
      responseIntList.addAll(event);
    }).onDone(onSocketDone);
  }

  void _socksConnectionRequest(Uri uri, Socket socket) {
    var uriPortBytes = [(uri.port >> 8) & 0xFF, uri.port & 0xFF];
    var uriAuthorityAscii = ascii.encode(uri.authority);

    socket.add([
      0x04, // SOCKS version
      0x01, // request establish a TCP/IP stream connection
      ...uriPortBytes, // 2 bytes destination port
      0x00, // 4 bytes of destination ip
      0x00, // if socks4a and destination ip equals 0.0.0.NonZero
      0x00, // then we can pass destination domen after first 0x00 byte
      0x01,
      0x00,
      ...uriAuthorityAscii, // destination domen
      0x00,
    ]);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Tor Onion Proxy example'),
        ),
        body: LayoutBuilder(
          builder: (context, constrains) {
            return Scrollbar(
              child: SingleChildScrollView(
                child: Container(
                  constraints: BoxConstraints(minHeight: constrains.maxHeight),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      SizedBox(height: 20),
                      Text(
                          'Tor running on: ${_torLocalPort ?? _error ?? 'Unknown'}'),
                      SizedBox(height: 20),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16.0),
                        child: Wrap(
                          runSpacing: 20,
                          spacing: 20,
                          children: <Widget>[
                            OutlinedButton(
                              child: Text('Start Tor Onion Proxy'),
                              onPressed:
                                  _torLocalPort == null ? _startTor : null,
                            ),
                            OutlinedButton(
                              child: Text('Stop Tor Onion Proxy'),
                              onPressed:
                                  _torLocalPort != null ? _stopTor : null,
                            ),
                            OutlinedButton(
                              child:
                                  Text('Send request to check.torproject.org'),
                              onPressed: _torLocalPort != null
                                  ? () => _sendGetRequest(
                                      Uri.https('check.torproject.org', '/'))
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
          },
        ),
      ),
    );
  }

  @override
  void dispose() {
    _socket!.close();
    super.dispose();
  }
}
