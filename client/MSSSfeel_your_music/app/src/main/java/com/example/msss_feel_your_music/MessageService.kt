package com.example.msss_feel_your_music

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.msss_feel_your_music.Classifier.ModelClassifier
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

// Service that simulates a bridge between wearable device and main activity of this application
class MessageService : Service(), MessageClient.OnMessageReceivedListener {

    private lateinit var mMessageClient: MessageClient
    private lateinit var modelClassifier: ModelClassifier

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MessageService::class.java)
        }
    }

    // When the service is created
    override fun onCreate() {
        super.onCreate()

        // Get the wearable client and add this service as a listener
        mMessageClient = Wearable.getMessageClient(this)
        mMessageClient.addListener(this)
        modelClassifier = ModelClassifier(applicationContext)

    }

    //When the service is destroyed
    override fun onDestroy() {
        super.onDestroy()

        // Remove the listener
        mMessageClient.removeListener(this)
    }

    // When the smartphone receives the data from the wearable device
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = String(messageEvent.data)
        Log.d(ContentValues.TAG, "msg received. Path: $path, Data: $data")

        // Split and classify
        val features = data.split(",").map { it.toFloat() }
        val label = modelClassifier.classify(features)

        // Print the predicted label
        Log.d(ContentValues.TAG, "Predicted label: $label")

        // Forward emotion label to the main activity
        val intent = Intent()
        intent.setAction(getString(R.string.intent_wearabledata_received))
        intent.putExtra("label", label)
        sendBroadcast(intent)
    }
}
