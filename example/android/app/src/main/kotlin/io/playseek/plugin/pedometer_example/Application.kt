package io.playseek.plugin.pedometer_example

import io.flutter.app.FlutterApplication
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback
import io.flutter.plugins.GeneratedPluginRegistrant
//import io.playseek.plugin.pedometer.PedometerService
import io.playseek.plugin.pedometer.PedometerPlugin

class Application : FlutterApplication(), PluginRegistrantCallback {
//    override fun onCreate() {
//        super.onCreate();
//        PedometerService.setPluginRegistrant(this);
//    }

    override fun registerWith(registry: PluginRegistry) {
        PedometerPlugin.registerWith(registry.registrarFor("io.playseek.plugin.pedometer.PedometerPlugin"));
    }
}