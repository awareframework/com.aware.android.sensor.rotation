package com.awareframework.android.sensor.rotation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ROTATION_VECTOR
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.db.model.DbSyncConfig
import com.awareframework.android.core.model.SensorConfig
import com.awareframework.android.sensor.rotation.model.RotationData
import com.awareframework.android.sensor.rotation.model.RotationDevice

/**
 * AWARE Rotation module
 * - Rotation raw data
 * - Rotation sensor information
 *
 * @author  sercant
 * @date 21/08/2018
 */
class RotationSensor : AwareSensor(), SensorEventListener {

    companion object {
        const val TAG = "AWARE::Rotation"

        const val ACTION_AWARE_ROTATION = "ACTION_AWARE_ROTATION"

        const val ACTION_AWARE_ROTATION_START = "com.awareframework.android.sensor.rotation.SENSOR_START"
        const val ACTION_AWARE_ROTATION_STOP = "com.awareframework.android.sensor.rotation.SENSOR_STOP"

        const val ACTION_AWARE_ROTATION_SET_LABEL = "com.awareframework.android.sensor.rotation.ACTION_AWARE_ROTATION_SET_LABEL"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_ROTATION_SYNC = "com.awareframework.android.sensor.rotation.SENSOR_SYNC"

        val CONFIG = Config()

        var currentInterval: Int = 0
            private set

        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)
            context.startService(Intent(context, RotationSensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RotationSensor::class.java))
        }
    }

    private lateinit var mSensorManager: SensorManager
    private var mRotation: Sensor? = null
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler

    private var lastSave = 0L

    private var lastValues = arrayOf(0f, 0f, 0f)
    private var lastTimestamp: Long = 0
    private var lastSavedAt: Long = 0

    private val dataBuffer = ArrayList<RotationData>()

    private var dataCount: Int = 0
    private var lastDataCountTimestamp: Long = 0

    private val rotationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                ACTION_AWARE_ROTATION_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let {
                        CONFIG.label = it
                    }
                }

                ACTION_AWARE_ROTATION_SYNC -> onSync(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        initializeDbEngine(CONFIG)

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mRotation = mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR)

        sensorThread = HandlerThread(TAG)
        sensorThread.start()

        sensorHandler = Handler(sensorThread.looper)

        registerReceiver(rotationReceiver, IntentFilter().apply {
            addAction(ACTION_AWARE_ROTATION_SET_LABEL)
            addAction(ACTION_AWARE_ROTATION_SYNC)
        })

        logd("Rotation service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        return if (mRotation != null) {
            saveSensorDevice(mRotation)

            val samplingFreqUs = if (CONFIG.interval > 0) 1000000 / CONFIG.interval else 0
            mSensorManager.registerListener(
                    this,
                    mRotation,
                    samplingFreqUs,
                    sensorHandler)

            lastSave = System.currentTimeMillis()

            logd("Rotation service active: ${CONFIG.interval} samples per second.")

            START_STICKY
        } else {
            logw("This device doesn't have a rotation sensor!")

            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        sensorHandler.removeCallbacksAndMessages(null)
        mSensorManager.unregisterListener(this, mRotation)
        sensorThread.quit()

        dbEngine?.close()

        unregisterReceiver(rotationReceiver)

        logd("Rotation service terminated...")
    }

    private fun saveSensorDevice(sensor: Sensor?) {
        sensor ?: return

        val device = RotationDevice().apply {
            deviceId = CONFIG.deviceId
            timestamp = System.currentTimeMillis()

            maxRange = sensor.maximumRange
            minDelay = sensor.minDelay.toFloat()
            name = sensor.name
            power = sensor.power
            resolution = sensor.resolution
            type = sensor.type.toString()
            vendor = sensor.vendor
            version = sensor.version.toString()
        }

        dbEngine?.save(device, RotationDevice.TABLE_NAME, 0)

        logd("Rotation sensor info: $device")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //We log current accuracy on the sensor changed event
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastDataCountTimestamp >= 1000) {
            currentInterval = dataCount
            dataCount = 0
            lastDataCountTimestamp = currentTime
        }

        if (currentTime - lastTimestamp < (900.0 / CONFIG.interval)) {
            // skip this event
            return
        }
        lastTimestamp = currentTime

        if (CONFIG.threshold > 0 &&
                Math.abs(event.values[0] - lastValues[0]) < CONFIG.threshold &&
                Math.abs(event.values[1] - lastValues[1]) < CONFIG.threshold &&
                Math.abs(event.values[2] - lastValues[2]) < CONFIG.threshold) {
            return
        }

        lastValues.forEachIndexed { index, _ ->
            lastValues[index] = event.values[index]
        }

        val data = RotationData().apply {
            timestamp = currentTime
            deviceId = CONFIG.deviceId
            label = CONFIG.label

            x = event.values[0]
            y = event.values[1]
            z = event.values[2]

            if (event.values.size == 4)
                w = event.values[3]

            accuracy = event.accuracy
            eventTimestamp = event.timestamp
        }

        CONFIG.sensorObserver?.onDataChanged(data)

        dataBuffer.add(data)
        dataCount++

        if (currentTime - lastSavedAt < CONFIG.period * 60000) { // convert minute to ms
            // not ready to save yet
            return
        }
        lastSavedAt = currentTime

        val dataBuffer = this.dataBuffer.toTypedArray()
        this.dataBuffer.clear()

        try {
            logd("Saving buffer to database.")
            dbEngine?.save(dataBuffer, RotationData.TABLE_NAME)

            sendBroadcast(Intent(ACTION_AWARE_ROTATION))
        } catch (e: Exception) {
            e.message ?: logw(e.message!!)
            e.printStackTrace()
        }
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(RotationData.TABLE_NAME)
        dbEngine?.startSync(RotationDevice.TABLE_NAME, DbSyncConfig(removeAfterSync = false))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    interface Observer {
        fun onDataChanged(data: RotationData)
    }

    data class Config(
            /**
             * For real-time observation of the sensor data collection.
             */
            var sensorObserver: Observer? = null,

            /**
             * Rotation interval in hertz per second: e.g.
             *
             * 0 - fastest
             * 1 - sample per second
             * 5 - sample per second
             * 20 - sample per second
             */
            var interval: Int = 5,

            /**
             * Period to save data in minutes. (optional)
             */
            var period: Float = 1f,

            /**
             * Rotation threshold (float).  Do not record consecutive points if
             * change in value is less than the set value.
             */
            var threshold: Double = 0.0

            // TODO wakelock?

    ) : SensorConfig(dbPath = "aware_rotation") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
                interval = config.interval
                period = config.period
                threshold = config.threshold
            }
        }
    }

    class RotationSensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_ROTATION_STOP,
                SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stop(context)
                }

                ACTION_AWARE_ROTATION_START -> {
                    start(context)
                }
            }
        }
    }
}

private fun logd(text: String) {
    if (RotationSensor.CONFIG.debug) Log.d(RotationSensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(RotationSensor.TAG, text)
}