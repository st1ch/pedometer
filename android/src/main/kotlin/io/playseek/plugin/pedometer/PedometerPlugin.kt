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

class PedometerPlugin(context: Context, activity: Activity?): StreamHandler, MethodCallHandler {

  private val mContext = context
  private val mActivity = activity

  companion object {
    @JvmStatic
    private val TAG = "PedometerPlugin"
    @JvmStatic
    val SHARED_PREFERENCES_KEY = "pedometer_plugin_cache"
    @JvmStatic
    val CALLBACK_HANDLE_KEY = "callback_handle"
    @JvmStatic
    val CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatch_handler"
    @JvmStatic
    val PERSISTENT_PEDOMETER_KEY = "persistent_pedometer"
    @JvmStatic
    private val pedometerCacheLock = Object()
    @JvmStatic
    val EVENT_CHANNEL = "io.playseek.plugin.pedometer.eventChannel"
    @JvmStatic
    val METHOD_CHANNEL = "io.playseek.plugin.pedometer.methodChannel"
    @JvmStatic
    private var sensorEventListener: SensorEventListener? = null

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
      synchronized(pedometerCacheLock) {
        registerPedometer(context, false, null, null)
      }
    }

    @JvmStatic
    private fun registerPedometer(context: Context,
                                  cache: Boolean,
                                  result: Result?,
                                  events: EventChannel.EventSink?) {
      val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
      val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
      sensorEventListener = sensorEventListener(context, cache, events)
      sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
      result?.success(true)
    }

    @JvmStatic
    private fun addPedometerToCache(context: Context, steps: Int) {
      synchronized(pedometerCacheLock) {
        var p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

        val date = Date()
        val dateFormat = SimpleDateFormat("dd-mm-yyyy")
        val today = dateFormat.format(date)

        var persistentPedometer = p.getInt(getPersistentPedometerKey(today), 0)

        var result = steps;

        if (persistentPedometer != 0) {
          result = steps - persistentPedometer;
        }

        context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .edit()
                .putInt(getPersistentPedometerKey(today), result)
                .apply()
      }
    }

    @JvmStatic
    private fun initializeService(context: Context, args: ArrayList<*>?) {
      Log.d(TAG, "Initializing PedometerService")
      val callbackHandle = args!![0] as Long
      context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
              .edit()
              .putLong(CALLBACK_DISPATCHER_HANDLE_KEY, callbackHandle)
              .apply()
    }

    @JvmStatic
    private fun getPedometerPendingIndent(context: Context, callbackHandle: Long): PendingIntent {
      val intent = Intent(context, PedometerBroadcastReceiver::class.java)
              .putExtra(CALLBACK_HANDLE_KEY, callbackHandle)
      return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @JvmStatic
    private fun removePedometer(context: Context,
                                result: Result) {
      result.success(true)
      val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
      sensorManager.unregisterListener(sensorEventListener)
    }


    @JvmStatic
    private fun removePedometerFromCache(context: Context, date: String) {
      synchronized(pedometerCacheLock) {
        var p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        var persistentPedometer = p.getInt(getPersistentPedometerKey(date), 0)
        if (persistentPedometer == 0) {
          return
        }
        p.edit()
                .remove(getPersistentPedometerKey(date))
                .apply()
      }
    }

    @JvmStatic
    private fun getPersistentPedometerKey(date: String): String {
      return PERSISTENT_PEDOMETER_KEY + "/" + date
    }

    @JvmStatic
    private fun sensorEventListener(context: Context, cache: Boolean, events: EventChannel.EventSink?): SensorEventListener
    {
      return object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
          Log.i(TAG, "Sensor changed: " + event.values[0])
          val stepCount = event.values[0].toInt()
          if (cache) {
            addPedometerToCache(context, stepCount)
          }
          events?.success(stepCount)
        }
      }
    }
  }

  init {
    Log.i(TAG, "onCreate")
  }

  override fun onListen(arguments: Any, events: EventChannel.EventSink) {
    Log.i(TAG, "onListen")
    val args = arguments as? ArrayList<*>
    initializeService(mContext, args)
    registerPedometer(mContext, true, null, events)
  }

  override fun onCancel(arguments: Any) {
//    sensorManager.unregisterListener(sensorEventListener)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    val args = call.arguments() as? ArrayList<*>
    when (call.method) {
      "PedometerPlugin.initializeService" -> {
        initializeService(mContext, args)
        result.success(true)
      }
      "PedometerPlugin.registerPedometer" -> registerPedometer(mContext,
              true,
              result,
              null)
      "PedometerPlugin.removePedometer" -> removePedometer(mContext,
              result)
      else -> result.notImplemented()
    }
  }
}