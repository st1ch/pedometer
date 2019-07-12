import 'dart:async';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'callback_dispatcher.dart';

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
    return _methodChannel.invokeMethod('PedometerPlugin.registerPedometer');
  }

  static Stream<String> _pedometerStream;

  static Stream<String> get stepCountStream {
    if (_pedometerStream == null) {
      final callback = PluginUtilities.getCallbackHandle(callbackDispatcher);

      _pedometerStream = _eventChannel.receiveBroadcastStream(
          <dynamic>[callback.toRawHandle()]).map((stepCount) {
        print('>>>> SERVICE PEDOMETER steps: $stepCount');
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
  /// Promote the pedometer service to a foreground service.
  ///
  /// Will throw an exception if called anywhere except for a pedometer
  /// callback.
  static Future<void> promoteToForeground() async => await _backgroundChannel
      .invokeMethod('PedometerService.promoteToForeground');

  /// Demote the pedometer service from a foreground service to a background
  /// service.
  ///
  /// Will throw an exception if called anywhere except for a pedometer
  /// callback.
  static Future<void> demoteToBackground() async => await _backgroundChannel
      .invokeMethod('PedometerService.demoteToBackground');

  /// Register for pedometer events.
  ///
  /// `callback` is the method to be called when a pedometer event occurs.
  static Future<void> registerPedometerC() async {
    if (Platform.isIOS) {
      throw UnsupportedError("iOS does not support 'PedometerPlugin'");
    }
//    final List<dynamic> args = <dynamic>[
//      PluginUtilities.getCallbackHandle(callback).toRawHandle()
//    ];
    await _methodChannel.invokeMethod('PedometerPlugin.registerPedometer');
  }

  /// Stop receiving pedometer events.
  static Future<bool> removePedometer() async =>
      _methodChannel.invokeMethod('PedometerPlugin.removePedometer');
}
