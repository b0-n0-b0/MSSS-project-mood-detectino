package com.example.msss_feel_your_music

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp



class MainActivity : ComponentActivity() {
//    private var spotifyAppRemote: SpotifyAppRemote? = null
    private lateinit var spotifyService: SpotifyService
    private var mBound: Boolean = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SpotifyService, cast the IBinder and get SpotifyService instance.
            val binder = service as SpotifyService.LocalBinder
            spotifyService = binder.getService()
            mBound = true
            connected()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SpotifyService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun connected() {
        val isConnected = spotifyService.connectToSpotify()
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
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }
}