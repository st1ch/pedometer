import 'dart:convert';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:pedometer/pedometer.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _todaySteps = 0;
  int _sessionSteps = 0;
  StreamSubscription _pedometerSubscription;
  final titleController = TextEditingController();
  final descriptionController = TextEditingController();

  @override
  void dispose() {
    _pedometerSubscription?.cancel();
    super.dispose();
  }

  Future<void> setUpPedometer() async {
    PedometerPlugin.setNotificationTitle(titleController.text);
    PedometerPlugin.setNotificationDescription(descriptionController.text);
    if (_pedometerSubscription == null || _pedometerSubscription.isPaused) {
      _pedometerSubscription = PedometerPlugin.stepCountStream.listen(
        (stepsJson) async {
          var steps = json.decode(stepsJson);
          setState(() {
            _todaySteps = steps['today'];
            _sessionSteps = steps['session'];
          });
        },
        onError: (e) {},
        cancelOnError: true,
      );
    }
  }

  Future<void> stopPedometer() async {
    PedometerPlugin.removePedometer();
    _pedometerSubscription?.cancel();
    _pedometerSubscription = null;
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
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Text(
                    'Today steps: ',
                  ),
                  Text(
                    '$_todaySteps',
                    style: TextStyle(fontSize: 24.0),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Text(
                    'Session steps: ',
                  ),
                  Text(
                    '$_sessionSteps',
                    style: TextStyle(fontSize: 24.0),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  RaisedButton(
                    child: Text('Start service'),
                    onPressed: () {
                      setUpPedometer();
                    },
                  ),
                  RaisedButton(
                    child: Text('Stop service'),
                    onPressed: () {
                      stopPedometer();
                    },
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      controller: titleController,
                      decoration: InputDecoration.collapsed(hintText: "Title"),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16.0),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      controller: descriptionController,
                      decoration:
                          InputDecoration.collapsed(hintText: "Description"),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
