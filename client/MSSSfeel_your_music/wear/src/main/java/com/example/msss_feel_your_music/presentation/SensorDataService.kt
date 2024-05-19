package com.example.msss_feel_your_music.presentation

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

class SensorDataService :  Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null
    private var sensorSimulator = SensorSimulator()
    private val handlerThread = HandlerThread("SensorDataThread")
    private lateinit var sensorHandler: Handler
    private val sendDataHandler = Handler(Looper.getMainLooper())
    private val interval = 45000L // 45 seconds


    private val hrValues = mutableListOf<Float>()
    private val edaValues = mutableListOf<Float>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "On create SensorDataService")
        /*
       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in sensorList) {
            Log.d(TAG, "Sensor name: ${sensor.name}, type: ${sensor.type}")
        }
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if(heartRateSensor != null){
            Log.d(TAG,"$heartRateSensor.")
        }
        edaSensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)  //EDA TO MODIFY LATER
       */
         sensorSimulator = SensorSimulator()
        sensorSimulator.addHeartRateListener { hr ->
            hrValues.add(hr)
        }
        sensorSimulator.addEDaListener { eda ->
            edaValues.add(eda)
        }
        sensorSimulator.startSimulation()
        handlerThread.start()
        sensorHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: $intent")
        intent?.let { handleIntent(it) }
        return super.onStartCommand(intent, flags, startId)

    }
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        when (action) {
            ACTION_START -> {
                Log.d(TAG, "Received start command")
                //   startSensorReadings()
                scheduleSendData()
            }
            else -> Log.e(TAG, "Unknown action: $action")
        }
    }

    private fun startSensorReadings() {
        Log.e(TAG, "startSensorReading")
        startSensorReading(heartRateSensor)
        startSensorReading(edaSensor)
    }

    private fun startSensorReading(sensor: Sensor?) {
        Log.e(TAG, "Starting reading sensor")

        sensor?.let { s ->
            sensorManager.registerListener(
                this,
                s,
                SensorManager.SENSOR_DELAY_NORMAL,
                sensorHandler
            )
        }
    }

    private fun stopSensorReadings() {
        sensorManager.unregisterListener(this)
        handlerThread.quitSafely()
    }

    private fun scheduleSendData() {
        Log.e(TAG, "ScheduleSendData")
        sendDataHandler.postDelayed({
            sendDataToMobile()
            scheduleSendData()
        }, interval)
    }

    private fun cancelSendData() {
        sendDataHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorSimulator.stopSimulation()
        handlerThread.quitSafely()
        cancelSendData()
        //stopSensorReading
        Log.d(TAG, "SensorDataService destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.e(TAG, "onSensorChanged")
        event?.let {
            Log.e(TAG, "value: ${event.values[0]}")
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> hrValues.add(event.values[0])
                Sensor.TYPE_RELATIVE_HUMIDITY -> edaValues.add(event.values[0])
                else -> {}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun calculateFeatures(values: List<Float>, type: Boolean): List<Float> {
        Log.e(TAG, "calculateFeatures")
        Log.e(TAG, "Values: $values")

        val mean = values.average().toFloat()
        val max = values.maxOrNull() ?: 0f
        val min = values.minOrNull() ?: 0f
        val std = calculateStdDev(values)
        if (type) {
            return listOf(min, max, mean, std)
        }
        return listOf(mean, min, max, std)
    }

    private fun calculateStdDev(values: List<Float>): Float {
        Log.e(TAG, "calculateStdDev")
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.sum() / values.size
        return kotlin.math.sqrt(variance).toFloat()
    }

    private fun sendMessage(messagePath: String, data: String) {
        val nodeIdsTask: Task<List<Node>> = Wearable.getNodeClient(this).connectedNodes
        val byteArray = data.toByteArray(Charsets.UTF_8)
        nodeIdsTask.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d(ContentValues.TAG, "nodo: $node.id")
                val sendMessageTask =
                    Wearable.getMessageClient(this).sendMessage(node.id, messagePath, byteArray)
                sendMessageTask.addOnSuccessListener {
                    Log.d(ContentValues.TAG, "Message sent successfully")

                }.addOnFailureListener { exception ->
                    Log.e(ContentValues.TAG, "Failed to send message", exception)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e(ContentValues.TAG, "Failed to get node IDs", exception)
        }
    }

    private fun sendDataToMobile() {
        Log.e(TAG, "sendDataToMobile")
        Log.e(TAG, "hr: $hrValues")
        Log.e(TAG, "eda: $edaValues")

        if (hrValues.isNotEmpty() && edaValues.isNotEmpty()) {
            val hrFeatures = calculateFeatures(hrValues, type = true)
            val edaFeatures = calculateFeatures(edaValues, type = false)
            val scaledInputFeatureshr = scaleData(hrFeatures)
            val scaledInputFeatureseda = scaleData(edaFeatures)
            val data =
                "${scaledInputFeatureshr.joinToString(",")},${scaledInputFeatureseda.joinToString(",")}"
            Log.e(TAG, "features extracted: $data")
            sendMessage("/data", data)
            hrValues.clear()
            edaValues.clear()
        }
    }

    private fun scaleData(data: List<Float>): List<Float> {
        val mean = data.average().toFloat()
        val stdDev = calculateStdDev(data)
        return data.map { (it - mean) / stdDev }
    }

    companion object {
        private const val TAG = "SensorDataService"
        const val ACTION_START = "com.example.wear.ACTION_START"
        const val ACTION_STOP = "com.example.wear.ACTION_STOP"
    }
}