package io.playseek.plugin.pedometer

import android.app.Activity
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.Manifest
import android.content.Intent
import android.app.PendingIntent
import android.annotation.TargetApi
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import android.os.Build
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.EventChannel
import org.json.JSONArray
import org.json.JSONObject
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.support.v4.content.ContextCompat
import android.app.ActivityManager

class PedometerPlugin(context: Context, activity: Activity?) : StreamHandler, MethodCallHandler {

    private val mContext = context
    private val mActivity = activity

    companion object {
        @JvmStatic
        private val TAG = "PedometerPlugin"
        @JvmStatic
        val EVENT_CHANNEL = "io.playseek.plugin.pedometer.eventChannel"
        @JvmStatic
        val METHOD_CHANNEL = "io.playseek.plugin.pedometer.methodChannel"
        @JvmStatic
        private var activityReceiver: BroadcastReceiver? = null

        @JvmStatic
        private fun receiver(events: EventChannel.EventSink?): BroadcastReceiver {
            return object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    val todaySteps = intent.getIntExtra("today", 0)
                    val sessionSteps = intent.getIntExtra("session", 0)
                    events?.success("{\"today\":$todaySteps, \"session\":$sessionSteps}")
                }
            }
        }

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val plugin = PedometerPlugin(registrar.context(), registrar.activity())
            val methodChannel = MethodChannel(registrar.messenger(), METHOD_CHANNEL)
            methodChannel.setMethodCallHandler(plugin)
            val eventChannel = EventChannel(registrar.messenger(), EVENT_CHANNEL)
            eventChannel.setStreamHandler(plugin)
        }

        @JvmStatic
        fun reRegisterAfterReboot(context: Context) {
            startService(context, null)
        }

        @JvmStatic
        private fun startService(context: Context,
                                 events: EventChannel.EventSink?
        ) {
            if (!isMyServiceRunning(context, PedometerService::class.java)) {
                Log.i(TAG, ">>> START")
                val serviceIntent = Intent(context, PedometerService::class.java)
                serviceIntent.putExtra("notification_title", "My Title")
                serviceIntent.putExtra("notification_description", "My Description >>> Service running")
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            if (activityReceiver != null) {
                context.unregisterReceiver(activityReceiver)
            }
            activityReceiver = receiver(events)
            val intentFilter = IntentFilter(PedometerService.BROADCAST_RECEIVER_ID)
            context.registerReceiver(activityReceiver, intentFilter)
        }

        @JvmStatic
        private fun stopService(context: Context) {
            if (isMyServiceRunning(context, PedometerService::class.java)) {
                Log.i(TAG, ">>> STOP")
                val serviceIntent = Intent(context, PedometerService::class.java)
                context.stopService(serviceIntent)
                context.unregisterReceiver(activityReceiver)
                activityReceiver = null
            }
        }

        @JvmStatic
        private fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        private fun registerPedometer(context: Context,
                                      cache: Boolean,
                                      result: Result?,
                                      events: EventChannel.EventSink?) {
            startService(context, events)
            result?.success(true)
        }

        @JvmStatic
        private fun saveTitle(context: Context,
                              title: String) {
            context.getSharedPreferences(PedometerService.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PedometerService.TITLE_KEY, title)
                    .apply()
        }

        @JvmStatic
        private fun saveDescription(context: Context,
                                    description: String) {
            context.getSharedPreferences(PedometerService.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PedometerService.DESCRIPTION_KEY, description)
                    .apply()
        }
    }

    override fun onListen(arguments: Any, events: EventChannel.EventSink) {
        Log.i(TAG, "onListen")
        val args = arguments as? ArrayList<*>
        startService(mContext, events)
    }

    override fun onCancel(arguments: Any) {
//    sensorManager.unregisterListener(sensorEventListener)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val args = call.arguments() as? ArrayList<*>
        when (call.method) {
            "PedometerPlugin.initializeService" -> {
                startService(mContext, null)
                result.success(true)
            }
            "PedometerPlugin.registerPedometer" -> startService(mContext, null)
            "PedometerPlugin.removePedometer" -> stopService(mContext)
            "PedometerPlugin.setNotificationTitle" -> saveTitle(mContext, call.arguments as String)
            "PedometerPlugin.setNotificationDescription" -> saveDescription(mContext, call.arguments as String)
            else -> result.notImplemented()
        }
    }
}