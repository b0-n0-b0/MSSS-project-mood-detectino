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
import com.example.msss_feel_your_music.app_broadcast_receiver.PlayerBroadcastReceiver
import com.spotify.android.appremote.api.SpotifyAppRemote


// Main activity of the application
class MainActivity : ComponentActivity() {
    private var spotifyReceiver = PlayerBroadcastReceiver()

    private var internalReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == getString(R.string.intent_spotify_connection_error)){
                //TODO
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //internalReceiver setup
        val isInstalled = SpotifyAppRemote.isSpotifyInstalled(this)
        if(!isInstalled){
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("This app needs Spotify installed on the device!")
                .setCancelable(false)
                .setPositiveButton("Got it!") { _, _ ->
                    finish()
                }
            val alert = builder.create()
            alert.show()
        }
        //TODO
        val filter = IntentFilter()
        filter.addAction(getString(R.string.intent_spotify_connection_error))
        ContextCompat.registerReceiver(this, internalReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED)

        //spotifyReceiver setup
        val spotifyFilter = IntentFilter().apply {
            addAction(getString(R.string.intent_metadata_changed))
            addAction(getString(R.string.intent_queue_changed))
            addAction(getString(R.string.intent_playback_state_changed))}
        registerReceiver(spotifyReceiver, spotifyFilter, RECEIVER_EXPORTED)
        setContentView(R.layout.main_activity)
    }

    //TODO(capire se devo chiamare unbind e simili sulla onStop o sulla onDestroy etc)
    //TODO add popup to say "enable broadcast stuff in spotify"
    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SpotifyService::class.java)
        intent.setAction("queueAdd")
        startService(intent);
    }


    // When the activity is no more visible
    override fun onStop() {
        super.onStop()
//        unbindService(connection)
//        unregisterReceiver(receiver)
    }

}