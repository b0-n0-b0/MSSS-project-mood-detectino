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
    // Creates a new HandlerThread named "SensorDataThread".
    // This separate thread will be used to perform background operations.
    private val handlerThread = HandlerThread("SensorDataThread")
    private lateinit var sensorHandler: Handler
    //  Creates a Handler associated with the Looper of the main thread (UI thread).
    private val sendDataHandler = Handler(Looper.getMainLooper())
    private val interval = 90000L // 45
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
        //simulator to simulate data without the google pixel watch
//        sensorSimulator = SensorSimulator()
//        sensorSimulator.addHeartRateListener { hr ->
//            hrValues.add(hr)
//        }
//        sensorSimulator.addEDaListener { eda ->
//            edaValues.add(eda)
//        }
//        sensorSimulator.startSimulation()
        // Starts the HandlerThread, creating a separate thread with its own Looper.
        handlerThread.start()
        //Initializes the sensorHandler with the Looper of the newly started HandlerThread.
        // This allows the sensorHandler to manage operations on the separate thread.
        sensorHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSensorReadings()
        scheduleSendData() //schedule every 90 seconds the features's sending
        return START_STICKY
    }

    private fun startSensorReadings() {
        startSensorReading(heartRateSensor)
        startSensorReading(edaSensor)
    }

    //register the listener for the HR and EDA sensor
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
        sendDataHandler.postDelayed({
            sendDataToMobile()
            scheduleSendData()
        }, interval)
    }

    //clear the message queue and remove any pending Runnable tasks associated with sendDataHandler
    private fun cancelSendData() {
        sendDataHandler.removeCallbacksAndMessages(null)
    }

    // call when the user clicks on "stop"
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

    //triggered when the sensors have new values
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    hrValues.add(event.values[0])
                }
                65554 ->{
                    edaValues.add(event.values[0])
                }
                else -> {}
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun calculateFeatures(values: List<Float>, type: Boolean): List<Float> {
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

    //function to normalize data
    private fun scaleData(data: List<Float>): List<Float> {
        val mean = data.average().toFloat()
        val stdDev = calculateStdDev(data)
        return data.map { (it - mean) / stdDev }
    }

    companion object {
        const val TAG = "SensorDataService"
    }
}