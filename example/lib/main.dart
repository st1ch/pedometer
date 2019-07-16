import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
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
  int _dateSteps = 0;
  DateTime _date;
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

  Future<void> getCachedTodaySteps() async {
    var steps = await PedometerPlugin.getTodaySteps();
    setState(() {
      _todaySteps = steps;
    });
  }

  Future<void> getCachedSessionSteps() async {
    var steps = await PedometerPlugin.getSessionSteps();
    setState(() {
      _sessionSteps = steps;
    });
  }

  Future<void> getCachedDateSteps(BuildContext context) async {
    var date = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime(2019, 1, 1),
      lastDate: DateTime.now(),
    );
    if (date != null) {
      var steps = await PedometerPlugin.getDateSteps(date);
      setState(() {
        _date = date;
        _dateSteps = steps;
      });
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
              const SizedBox(height: 16.0),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  RaisedButton(
                    child: Text('Cached today steps'),
                    onPressed: () {
                      getCachedTodaySteps();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 16.0),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  RaisedButton(
                    child: Text('Cached session steps'),
                    onPressed: () {
                      getCachedSessionSteps();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 16.0),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Text(
                    _date != null
                        ? '${DateFormat('dd-MM-yyyy').format(_date)} steps: '
                        : 'Select date',
                  ),
                  Text(
                    '$_dateSteps',
                    style: TextStyle(fontSize: 24.0),
                  ),
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Builder(
                    builder: (context) => RaisedButton(
                          child: Text('Cached DATE steps'),
                          onPressed: () {
                            getCachedDateSteps(context);
                          },
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
