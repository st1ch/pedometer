package io.playseek.plugin.pedometer

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.view.FlutterMain

class PedometerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PedometerBroadcastReceiver"
    }
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive PedometerBroadcastReceiver")
        FlutterMain.ensureInitializationComplete(context, null)
        PedometerService.enqueueWork(context, intent)
    }
}