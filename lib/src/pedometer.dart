import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'callback_dispatcher.dart';
import 'package:intl/intl.dart';

class PedometerPlugin {
  static const EventChannel _eventChannel =
      const EventChannel('io.playseek.plugin.pedometer.eventChannel');
  static const MethodChannel _backgroundChannel =
      MethodChannel('io.playseek.plugin.pedometer.background');
  static const MethodChannel _methodChannel =
      const MethodChannel('io.playseek.plugin.pedometer.methodChannel');

  static Future<bool> initialize() async {
    final callback = PluginUtilities.getCallbackHandle(callbackDispatcher);
    return _methodChannel.invokeMethod(
        'PedometerPlugin.initializeService', <dynamic>[callback.toRawHandle()]);
  }

  static Future<bool> registerPedometer() {
    if (Platform.isIOS) {
      throw UnsupportedError("iOS does not support 'PedometerPlugin'");
    }
    return _methodChannel.invokeMethod('PedometerPlugin.registerPedometer');
  }

  static Stream<String> _pedometerStream;

  static Stream<String> get stepCountStream {
    if (_pedometerStream == null) {
      final callback = PluginUtilities.getCallbackHandle(callbackDispatcher);

      _pedometerStream = _eventChannel.receiveBroadcastStream(
          <dynamic>[callback.toRawHandle()]).map((stepCount) {
        return stepCount;
      });
    }
    return _pedometerStream;
  }

//  Future<void> connect() async {
//    try {
//      await _methodChannel.invokeMethod<void>('connect');
//      print('Connected to service');
//    } on Exception catch (e) {
//      print(e.toString());
//      return;
//    }
//  }
//
//  Future<void> start() async {
//    try {
//      await _methodChannel.invokeMethod<void>('start');
//    } on PlatformException catch (e) {
//      debugPrint(e.toString());
//      rethrow;
//    }
//  }
//
//  Future<void> stop() async {
//    try {
//      await _methodChannel.invokeMethod<void>('stop');
//    } on PlatformException catch (e) {
//      debugPrint(e.toString());
//      rethrow;
//    }
//  }
//
//  Future<int> getCurrentSteps() async {
//    try {
//      final int result =
//          await _methodChannel.invokeMethod<int>('getCurrentSteps');
//      return result;
//    } on PlatformException catch (e) {
//      print(e.toString());
//    }
//
//    return 0;
//  }

  /// Stop receiving pedometer events.
  static Future<bool> removePedometer() async =>
      _methodChannel.invokeMethod('PedometerPlugin.removePedometer');

  static Future<bool> setNotificationTitle(String title) async =>
      _methodChannel.invokeMethod(
        'PedometerPlugin.setNotificationTitle',
        title,
      );

  static Future<bool> setNotificationDescription(String description) async =>
      _methodChannel.invokeMethod(
        'PedometerPlugin.setNotificationDescription',
        description,
      );

  static Future<int> getTodaySteps() async => _methodChannel.invokeMethod(
        'PedometerPlugin.getTodaySteps',
      );

  static Future<int> getDateSteps(DateTime date) async {
    DateFormat format = DateFormat('dd-MM-yyyy');
    return _methodChannel.invokeMethod(
      'PedometerPlugin.getDateSteps',
      format.format(date),
    );
  }

  static Future<int> getSessionSteps() async => _methodChannel.invokeMethod(
        'PedometerPlugin.getSessionSteps',
      );
}
