package io.playseek.plugin.pedometer_example

import android.os.Bundle
import android.content.Intent

import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import io.playseek.plugin.pedometer.PedometerService

class MainActivity: FlutterActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)

    val service = Intent(this, PedometerService::class.java)
    startService(service)
  }
}
