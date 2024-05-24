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
import java.util.concurrent.ConcurrentLinkedQueue

class SensorDataService :  Service(), SensorEventListener {


    //private var sensorSimulator = SensorSimulator()
    private val handlerThread = HandlerThread("SensorDataThread")
    private lateinit var sensorHandler: Handler
    private val sendDataHandler = Handler(Looper.getMainLooper())
    private val interval = 5000L // 45
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null

//    private val hrValues = mutableListOf<Float>()
//    private val edaValues = mutableListOf<Float>()
    val hrValues = ConcurrentLinkedQueue<Float>()
    val edaValues = ConcurrentLinkedQueue<Float>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "On create SensorDataService")

       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
//        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
//        for (sensor in sensorList) {
//            Log.d(TAG, "Sensor name: ${sensor.name}, " +
//                    "data: ${sensor.type}, ${sensor.id}")
//        }
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if(heartRateSensor != null){
            Log.d(TAG,"heart Sensor: $heartRateSensor")
        }

        edaSensor = sensorManager.getDefaultSensor(65554)
        if(heartRateSensor != null){
            Log.d(TAG,"EDA Sensor: $edaSensor")
        }
//        sensorSimulator = SensorSimulator()
//        sensorSimulator.addHeartRateListener { hr ->
//            hrValues.add(hr)
//        }
//        sensorSimulator.addEDaListener { eda ->
//            edaValues.add(eda)
//        }
//        sensorSimulator.startSimulation()
        handlerThread.start()
        sensorHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSensorReadings()
        scheduleSendData()
        return START_STICKY
    }

    private fun startSensorReadings() {
        Log.d(TAG, "startSensorReading")
        startSensorReading(heartRateSensor)
        startSensorReading(edaSensor)
    }

    private fun startSensorReading(sensor: Sensor?) {
        Log.d(TAG, "Starting reading sensor ${sensor?.type}")
        var delay = SensorManager.SENSOR_DELAY_NORMAL
        if (sensor?.type == 21){
            delay = SensorManager.SENSOR_DELAY_FASTEST
        }
        sensor?.let { s ->
           sensorManager.registerListener(
                    this,
                    s,
                    delay,
                    sensorHandler
                )
        }
    }

    private fun stopSensorReading() {
        sensorManager.unregisterListener(this)
        handlerThread.quitSafely()
    }

    private fun scheduleSendData() {
        Log.d(TAG, "ScheduleSendData")
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
        //sensorSimulator.stopSimulation()
        handlerThread.quitSafely()
        cancelSendData()
        stopSensorReading()
        Log.d(TAG, "SensorDataService destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d(TAG, "onSensorChanged")
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    hrValues.add(event.values[0])
                    Log.d("BVP", event.values[0].toString())
                }
                65554 ->{
                    edaValues.add(event.values[0])
                    Log.d("EDA", event.values[0].toString())
                }
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

    fun sendDataToMobile() {
        val tmpHr=hrValues.toList()
        val tmpEda=edaValues.toList()
        hrValues.clear()
        edaValues.clear()
        Log.d(TAG, "sendDataToMobile")
        Log.d(TAG, "hr: $tmpHr")
        Log.d(TAG, "eda: $tmpEda")
        if (tmpHr.isNotEmpty() || tmpEda.isNotEmpty()) {
            val hrFeatures = calculateFeatures(tmpHr.toList(), type = true)
            val edaFeatures = calculateFeatures(tmpEda.toList(), type = false)
            val scaledInputFeatureshr = scaleData(hrFeatures)
            val scaledInputFeatureseda = scaleData(edaFeatures)
            val data =
                "${scaledInputFeatureshr.joinToString(",")},${scaledInputFeatureseda.joinToString(",")}"
            Log.d(TAG, "features extracted: $data")
            sendMessage("/data", data)
        }
    }

    private fun scaleData(data: List<Float>): List<Float> {
        val mean = data.average().toFloat()
        val stdDev = calculateStdDev(data)
        return data.map { (it - mean) / stdDev }
    }

    companion object {
        const val TAG = "SensorDataService"
    }
}