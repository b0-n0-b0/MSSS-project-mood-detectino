package com.example.msss_feel_your_music

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.msss_feel_your_music.Classifier.ModelClassifier
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable

class MessageService : Service(), MessageClient.OnMessageReceivedListener {

    private lateinit var mMessageClient: MessageClient
    private lateinit var modelClassifier: ModelClassifier
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MessageService = this@MessageService
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MessageService::class.java)
        }
    }
    override fun onCreate() {
        super.onCreate()
        mMessageClient = Wearable.getMessageClient(this)
        mMessageClient.addListener(this)
        modelClassifier = ModelClassifier(applicationContext)
        sendMessage("/start","start")

    }

    override fun onDestroy() {
        super.onDestroy()
        mMessageClient.removeListener(this)
    }
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = String(messageEvent.data)

        Log.d(ContentValues.TAG, "msg received. Percorso: $path, Dati: $data")
        val features = data.split(",").map { it.toFloat() }
        Log.d(ContentValues.TAG, "features: $features")
        val label = modelClassifier.classify(features)
        // Print the predicted label
        Log.d(ContentValues.TAG, "Predicted label: $label")
    }

    fun sendMessage(messagePath: String, data: String) {
        val nodeIdsTask: Task<List<Node>> = Wearable.getNodeClient(this).connectedNodes
        val byteArray = data.toByteArray(Charsets.UTF_8)
        nodeIdsTask.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d(ContentValues.TAG, "nodo: $node.id")
                val sendMessageTask = Wearable.getMessageClient(this).sendMessage(node.id, messagePath, byteArray)
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
}
