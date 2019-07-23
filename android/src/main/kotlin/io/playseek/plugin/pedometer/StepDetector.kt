package io.playseek.plugin.pedometer

import android.content.Context
import android.content.Intent
import java.util.ArrayList

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Detects steps and notifies all listeners (that implement StepListener).
 */
class StepDetector(
        private val context: Context,
        private val cache: Boolean = true,
        private val shouldLog: Boolean = true,
        private val cacheLock: Any
) : SensorEventListener {

    private val mRawAccelValues = FloatArray(3)

    // smoothing accelerometer signal variables
    private val mAccelValueHistory = Array(3) { FloatArray(SMOOTHING_WINDOW_SIZE) }
    private val mRunningAccelTotal = FloatArray(3)
    private val mCurAccelAvg = FloatArray(3)
    private var mCurReadIndex = 0

    private var lastMag = 0.0
    private var avgMag = 0.0
    private var netMag = 0.0
    private var highestXValue = 0.0

    //peak detection variables
    private var lastXPoint = 1.0
    private var stepThreshold = 1.0
    private var noiseThreshold = 2.0
    private val windowSize = 10

    private var dataPoints = ArrayList<DataPoint>()

    companion object {
        private const val TAG = "StepDetector"
        private const val SMOOTHING_WINDOW_SIZE = 20
    }

    override fun onSensorChanged(e: SensorEvent) {
        synchronized(this) {
            if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                mRawAccelValues[0] = e.values[0]
                mRawAccelValues[1] = e.values[1]
                mRawAccelValues[2] = e.values[2]

                lastMag = sqrt(
                        mRawAccelValues[0].toDouble().pow(2.0)
                                + mRawAccelValues[1].toDouble().pow(2.0)
                                + mRawAccelValues[2].toDouble().pow(2.0)
                )

                //Source: https://github.com/jonfroehlich/CSE590Sp2018
                for (i in 0..2) {
                    mRunningAccelTotal[i] = mRunningAccelTotal[i] - mAccelValueHistory[i][mCurReadIndex]
                    mAccelValueHistory[i][mCurReadIndex] = mRawAccelValues[i]
                    mRunningAccelTotal[i] = mRunningAccelTotal[i] + mAccelValueHistory[i][mCurReadIndex]
                    mCurAccelAvg[i] = mRunningAccelTotal[i] / SMOOTHING_WINDOW_SIZE
                }
                mCurReadIndex++
                if (mCurReadIndex >= SMOOTHING_WINDOW_SIZE) {
                    mCurReadIndex = 0
                }

                avgMag = sqrt(
                        mCurAccelAvg[0].toDouble().pow(2.0)
                                + mCurAccelAvg[1].toDouble().pow(2.0)
                                + mCurAccelAvg[2].toDouble().pow(2.0)
                )

                netMag = lastMag - avgMag //removes gravity effect
                highestXValue += 1.0

                dataPoints.add(DataPoint(highestXValue, netMag))
            }
            peakDetection()
        }
    }

    private fun peakDetection() {

        /* Peak detection algorithm derived from: A Step Counter Service for Java-Enabled Devices Using a Built-In Accelerometer, Mladenov et al.
            *Threshold, stepThreshold was derived by observing people's step graph
            * ASSUMPTIONS:
            * Phone is held vertically in portrait orientation for better results
         */

        if (highestXValue - lastXPoint < windowSize) {
            return
        }

        val valuesInWindow = dataPoints.filter { dataPoint -> dataPoint.x in lastXPoint..highestXValue }

        lastXPoint = highestXValue

        val dataPointList = ArrayList<DataPoint>()

        for (point in valuesInWindow) {
            dataPointList.add(point)
        }

        dataPoints.clear()

        for (i in dataPointList.indices) {
            if (i == 0)
                continue
            else if (i < dataPointList.size - 1) {
                val forwardSlope = dataPointList[i + 1].y - dataPointList[i].y
                val downwardSlope = dataPointList[i].y - dataPointList[i - 1].y

                if (forwardSlope < 0 && downwardSlope > 0 && dataPointList[i].y > stepThreshold && dataPointList[i].y < noiseThreshold
                ) {
                    if (cache) {
                        iterateToCache()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

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

    class DataPoint(
            val x: Double,
            val y: Double
    ) {
        override fun toString(): String {
            return "DataPoint(x=$x, y=$y)"
        }
    }
}