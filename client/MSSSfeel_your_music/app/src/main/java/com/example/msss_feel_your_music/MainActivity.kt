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
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.msss_feel_your_music.app_broadcast_receiver.PlayerBroadcastReceiver


// Main activity of the application
class MainActivity : ComponentActivity() {
    private var internalReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == getString(R.string.intent_spotify_connected)){
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
    private var spotifyReceiver = PlayerBroadcastReceiver()
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
        //internalReceiver setup
        val filter = IntentFilter()
        filter.addAction(getString(R.string.intent_spotify_connected))
        ContextCompat.registerReceiver(this, internalReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED)

        //spotifyReceiver setup
        val spotifyFilter = IntentFilter().apply {
            addAction(getString(R.string.intent_metadata_changed))
            addAction(getString(R.string.intent_queue_changed))
            addAction(getString(R.string.intent_playback_state_changed))}
        registerReceiver(spotifyReceiver, spotifyFilter, RECEIVER_EXPORTED)
//      DEBUG db
//      (application as? FeelYourMusicApplication)?.logDatabaseContents()
        setContentView(R.layout.main_activity)
    }

    //TODO(capire se devo chiamare unbind e simili sulla onStop o sulla onDestroy etc)
    //TODO add popup to say "enable broadcast stuff in spotify"
    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SpotifyService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }


    // When the activity is no more visible
    override fun onStop() {
        super.onStop()
//        unbindService(connection)
//        unregisterReceiver(receiver)
        mBound = false
    }

}