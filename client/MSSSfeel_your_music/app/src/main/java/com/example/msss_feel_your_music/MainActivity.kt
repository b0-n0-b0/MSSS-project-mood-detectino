package com.example.msss_feel_your_music

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == "com.example.msss_feel_your_music.spotifyConnectionAction"){
                val isConnected = intent.getBooleanExtra("result",false)
                if(!isConnected){
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setMessage("This app needs Spotify installed on the device!")
                        .setCancelable(false)
                        .setPositiveButton("Got it!") { _, _ ->
                            finish()
                        }
                    val alert = builder.create()
                    alert.show()
                }
                spotifyService.test()
            }
        }
    }

    private lateinit var spotifyService: SpotifyService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SpotifyService, cast the IBinder and get SpotifyService instance.
            val binder = service as SpotifyService.LocalBinder
            spotifyService = binder.getService()
            mBound = true
            spotifyService.connectToSpotify()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter()
        filter.addAction("com.example.msss_feel_your_music.spotifyConnectionAction")
        ContextCompat.registerReceiver(this, receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED)
        setContentView(R.layout.main_activity)
    }

    //TODO(capire se devo chiamare unbind e simili sulla onStop o sulla onDestroy etc)
    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SpotifyService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }


    override fun onStop() {
        super.onStop()
//        unbindService(connection)
//        unregisterReceiver(receiver)
        mBound = false
    }

}