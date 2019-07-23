package io.playseek.plugin.pedometer

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.min

class StepDetector(
    private val context: Context,
    private val cache: Boolean = true,
    private val shouldLog: Boolean = true,
    private val cacheLock: Any
) : SensorEventListener {
    private var accelRingCounter = 0

    private val accelRingX = FloatArray(ACCEL_RING_SIZE)
    private val accelRingY = FloatArray(ACCEL_RING_SIZE)
    private val accelRingZ = FloatArray(ACCEL_RING_SIZE)
    private var velRingCounter = 0
    private val velRing = FloatArray(VEL_RING_SIZE)
    private var lastStepTimeNs: Long = 0
    private var oldVelocityEstimate = 0f

    companion object {
        private const val TAG = "StepDetector"
        private const val ACCEL_RING_SIZE = 50
        private const val VEL_RING_SIZE = 10
        // change this threshold according to your sensitivity preferences
        private const val STEP_THRESHOLD = 30f

        private const val STEP_DELAY_NS = 250000000

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(this) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2])
            }
        }
    }

    fun updateAccel(timeNs: Long, x: Float, y: Float, z: Float) {
        val currentAccel = FloatArray(3)
        currentAccel[0] = x
        currentAccel[1] = y
        currentAccel[2] = z

        // First step is to update our guess of where the global z vector is.
        accelRingCounter++
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0]
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1]
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2]


        val worldZ = FloatArray(3)
        worldZ[0] = SensorFilter.sum(accelRingX) / min(accelRingCounter, ACCEL_RING_SIZE)
        worldZ[1] = SensorFilter.sum(accelRingY) / min(accelRingCounter, ACCEL_RING_SIZE)
        worldZ[2] = SensorFilter.sum(accelRingZ) / min(accelRingCounter, ACCEL_RING_SIZE)

        val normalizationFactor = SensorFilter.norm(worldZ)

        worldZ[0] = worldZ[0] / normalizationFactor
        worldZ[1] = worldZ[1] / normalizationFactor
        worldZ[2] = worldZ[2] / normalizationFactor

        val currentZ = SensorFilter.dot(worldZ, currentAccel) - normalizationFactor
        velRingCounter++
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ

        val velocityEstimate = SensorFilter.sum(velRing)



        if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
            && timeNs - lastStepTimeNs > STEP_DELAY_NS
        ) {


            if (cache) {
                iterateToCache()
            }
            lastStepTimeNs = timeNs
        }
        oldVelocityEstimate = velocityEstimate
    }

    private fun iterateToCache() {
        synchronized(cacheLock) {
            val p = context.getSharedPreferences(PedometerService.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

            val todayPersistentKey = PedometerService.getTodayKey()

            val cachedSessionSteps = p.getInt(PedometerService.PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, 0)
            val cachedTodaySteps = p.getInt(todayPersistentKey, 0)

            val todaySteps = cachedTodaySteps + 1
            val sessionSteps = cachedSessionSteps + 1

            if (shouldLog) {
                Log.i(TAG, ">>> >>> >>> >>> >>> >>> >>>")
                Log.i(TAG, ">>> CACHED SESSION STEPS: $cachedSessionSteps")
                Log.i(TAG, ">>> SESSION STEPS: $sessionSteps")
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
                .putInt(PedometerService.PERSISTENT_PEDOMETER_SESSION_STEPS_KEY, sessionSteps)
                .apply()

            sendBroadcast(todaySteps, sessionSteps)
        }
    }

    private fun sendBroadcast(todaySteps: Int, sessionSteps: Int) {
        val intent = Intent().setAction(PedometerService.BROADCAST_RECEIVER_ID)
        intent.putExtra("today", todaySteps)
        intent.putExtra("session", sessionSteps)
        context.sendBroadcast(intent)
    }
}