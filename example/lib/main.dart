import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:pedometer/pedometer.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _currentSteps = 0;
  StreamSubscription _pedometerSubscription;

  @override
  void initState() {
    super.initState();
    setUpPedometer();
  }

  @override
  void dispose() {
    _pedometerSubscription.cancel();
    super.dispose();
  }

  Future<void> setUpPedometer() async {
    if (_pedometerSubscription == null || _pedometerSubscription.isPaused) {
      _pedometerSubscription = PedometerPlugin.stepCountStream.listen(
        (steps) async {
          setState(() {
            _currentSteps = steps;
          });
        },
        onError: (e) {},
        cancelOnError: true,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(
                'Current steps: ',
              ),
              Text(
                '$_currentSteps',
                style: TextStyle(fontSize: 24.0),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
