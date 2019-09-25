package io.playseek.plugin.pedometer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.os.Build
import android.app.PendingIntent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class PedometerService : Service() {
    companion object {
        @JvmStatic
        private val TAG = "PedometerService"
        @JvmStatic
        private val CHANNEL_ID = "pedometer_plugin_channel"
        @JvmStatic
        val BROADCAST_RECEIVER_ID = "pedometer_broadcast_receiver"
        @JvmStatic
        private val pedometerCacheLock = Object()
        @JvmStatic
        val PERSISTENT_PEDOMETER_KEY = "persistent_pedometer"
        @JvmStatic
        val PERSISTENT_PEDOMETER_INITIALIZED_KEY = "$PERSISTENT_PEDOMETER_KEY/initialized"
        @JvmStatic
        val PERSISTENT_PEDOMETER_SESSION_STEPS_KEY = "$PERSISTENT_PEDOMETER_KEY/session_steps"
        @JvmStatic
        val SHARED_PREFERENCES_KEY = "pedometer_plugin_cache"
        @JvmStatic
        val TITLE_KEY = "notification_title"
        @JvmStatic
        val DESCRIPTION_KEY = "notification_description"
        @JvmStatic
        private var sensorEventListener: SensorEventListener? = null

        @JvmStatic
        fun getPersistentPedometerKey(date: String): String {
            return "$PERSISTENT_PEDOMETER_KEY/$date"
        }

        @JvmStatic
        fun getTodayKey(date: Date = Date()): String {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy")
            val today = dateFormat.format(date)
            return getPersistentPedometerKey(today)
        }
    }

    override fun onBind(p0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        createNotificationChannel()

        val notificationIntent = Intent(this, getMainActivityClass(this))
        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        )
        val imageId = resources.getIdentifier(
                "ic_launcher",
                "mipmap",
                packageName
        )

//        var title = intent.getStringExtra("notification_title")
//        if (title == null || title.isEmpty()) {
//            title = "Pedometer"
//        }
//
//        var description = intent.getStringExtra("notification_description")
//        if (description == null || description.isEmpty()) {
//            description = "Steps recognition enabled"
//        }

        val title = getTitle()
        val description = getDescription()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(imageId)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build()

        try {
            startForeground(1, notification)
        } catch (e: Exception) {
            Log.e(TAG, ">>> Start service error")
        }

        registerPedometer()

        return START_STICKY
    }

    override fun onDestroy() {
        removePedometer()
        super.onDestroy()
    }

    private fun getMainActivityClass(context: Context): Class<*>? {
        val packageName = context.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        val className = launchIntent!!.component!!.className
        try {
            return Class.forName(className)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    private fun getTitle(): String {
        synchronized(pedometerCacheLock) {
            val p = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            val title = p.getString(TITLE_KEY, "")
            if (title == null || title.isEmpty()) {
                return "Pedometer"
            }
            return title
        }
    }

    private fun saveTitle(title: String) {
        synchronized(pedometerCacheLock) {
            getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(TITLE_KEY, title)
                    .apply()
        }
    }

    private fun getDescription(): String {
        synchronized(pedometerCacheLock) {
            val p = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            val title = p.getString(DESCRIPTION_KEY, "")
            if (title == null || title.isEmpty()) {
                return "Steps recognition enabled"
            }
            return title
        }
    }

    private fun saveDescription(description: String) {
        synchronized(pedometerCacheLock) {
            getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(DESCRIPTION_KEY, description)
                    .apply()
        }
    }

    private fun getCachedSessionSteps(): Int {
        synchronized(pedometerCacheLock) {
            val p = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            return p.getInt(PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, 0)
        }
    }

    private fun getCachedTodaySteps(): Int {
        synchronized(pedometerCacheLock) {
            val p = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            return p.getInt(getTodayKey(), 0)
        }
    }

    private fun getCachedDaySteps(date: Date): Int {
        synchronized(pedometerCacheLock) {
            val p = getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            return p.getInt(getTodayKey(date), 0)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Pedometer Plugin",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun registerPedometer() {
        val useStepCounter = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
        if (useStepCounter) {
            registerStepCounterPedometer()
        } else {
            registerAccelerometerPedometer()
        }
    }

    private fun registerStepCounterPedometer() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (sensorEventListener == null) {
            sensorEventListener = sensorEventListener(this, true)
        }

        sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    private fun registerAccelerometerPedometer() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorEventListener == null) {
            sensorEventListener = StepDetector(
                    this,
                    cache = true,
                    shouldLog = false,
                    cacheLock = pedometerCacheLock
            )
        }

        sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun removePedometer() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun sensorEventListener(context: Context, cache: Boolean): SensorEventListener {
        return object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent) {
                val stepCount = event.values[0].toInt()
                if (cache) {
                    addPedometerToCache(context, stepCount, shouldLog = false)
                }
            }
        }
    }

    private fun addPedometerToCache(context: Context, sessionSteps: Int, shouldLog: Boolean = false) {
        synchronized(pedometerCacheLock) {
            val p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

            val initialized = p.getBoolean(PERSISTENT_PEDOMETER_INITIALIZED_KEY, false)

            if (!initialized) {
                // save session initial steps
                p
                        .edit()
                        .putInt(PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, sessionSteps)
                        .apply()

                p
                        .edit()
                        .putBoolean(PERSISTENT_PEDOMETER_INITIALIZED_KEY, true)
                        .apply()
            }

            val todayPersistentKey = getTodayKey()

            var cachedSessionSteps = p.getInt(PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, 0)
            val cachedTodaySteps = p.getInt(todayPersistentKey, 0)

            if (cachedSessionSteps > sessionSteps) {
                // reset cached session steps
                p
                        .edit()
                        .putInt(PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, sessionSteps)
                        .apply()

                cachedSessionSteps = sessionSteps
            }

            val newSteps = sessionSteps - cachedSessionSteps
            val todaySteps = cachedTodaySteps + newSteps

            if (shouldLog) {
                Log.i(TAG, ">>> >>> >>> >>> >>> >>> >>>")
                Log.i(TAG, ">>> CACHED SESSION STEPS: $cachedSessionSteps")
                Log.i(TAG, ">>> SESSION STEPS: $sessionSteps")
                Log.i(TAG, ">>> NEW STEPS: $newSteps")
                Log.i(TAG, ">>> CACHED TODAY STEPS: $cachedTodaySteps")
                Log.i(TAG, ">>> TODAY STEPS: $todaySteps")
                Log.i(TAG, ">>> >>> >>> >>> >>> >>> >>>")
            }

            // save today steps
            p
                    .edit()
                    .putInt(todayPersistentKey, todaySteps)
                    .apply()

            // save session steps
            p
                    .edit()
                    .putInt(PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, sessionSteps)
                    .apply()

            sendBroadcast(todaySteps, sessionSteps)
        }
    }

    private fun sendBroadcast(todaySteps: Int, sessionSteps: Int) {
        val intent = Intent().setAction(BROADCAST_RECEIVER_ID)
        intent.putExtra("today", todaySteps)
        intent.putExtra("session", sessionSteps)
        sendBroadcast(intent)
    }

    private fun removePedometerFromCache(context: Context, date: String) {
        synchronized(pedometerCacheLock) {
            val p = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            val persistentPedometer = p.getInt(getPersistentPedometerKey(date), 0)
            if (persistentPedometer == 0) {
                return
            }
            p.edit()
                    .remove(getPersistentPedometerKey(date))
                    .apply()
        }
    }
}