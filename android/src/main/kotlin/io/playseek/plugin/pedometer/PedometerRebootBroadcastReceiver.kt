package io.playseek.plugin.pedometer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PedometerRebootBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PedometerRebootBroadcastReceiver"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.e(TAG, "Reregistering Pedometer!")
            PedometerPlugin.reRegisterAfterReboot(context)
        }
    }
}