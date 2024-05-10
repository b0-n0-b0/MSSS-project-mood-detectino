package com.example.msss_feel_your_music

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.types.Track
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter
import com.example.msss_feel_your_music.app_broadcast_receiver.MyBroadcastReceiver
import com.example.msss_feel_your_music.FeelYourMusicApplication

private var spotifyAppRemote: SpotifyAppRemote? = null

class MainActivity : ComponentActivity() {
    private val receiver = MyBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // TODO Receiver
        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.queuechanged")
            addAction("com.spotify.music.playbackstatechanged")
        }
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)

        // TODO Access db
        (application as? FeelYourMusicApplication)?.logDatabaseContents()


    }

    override fun onStart() {
        super.onStart()

        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        //NOTE: tries to connect to the spotify application
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                connected()
            }
            override fun onFailure(error: Throwable) {
                Log.d("Main Activity","Failed to connect: $error")
                when (error) {
                    is CouldNotFindSpotifyApp -> {
                        if (!SpotifyAppRemote.isSpotifyInstalled(applicationContext)){
                            val builder = AlertDialog.Builder(this@MainActivity)
                            builder.setMessage("This app needs Spotify installed on the device!")
                                .setCancelable(false)
                                .setPositiveButton("Got it!") { dialog, _ ->
                                    finish()
                                }
                            val alert = builder.create()
                            alert.show()
                        }
                    }
                }
            }
        })

    }

    private val spotifyLoginLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
            result?.let { activityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = activityResult.data
                    data?.let {
                        val extras = it.extras
                        extras?.let { bundle ->
                            val result = bundle.getBoolean("success")
                            Log.d("something","Success: $result")
                            if(result){
                                connected()
                            }
                        }
                    }
                } else {
                    //TODO: handle error
                }
            }
    }

    private fun connected() {
        spotifyAppRemote?.let { it ->
            // Play a playlist
            val playlistURI = "spotify:playlist:37i9dQZF1DX2sUQwD7tbmL"
            it.playerApi.play(playlistURI)
            // Subscribe to PlayerState
            it.playerApi.subscribeToPlayerState().setEventCallback {
                val track: Track = it.track
                Log.d("MainActivity", track.name + " by " + track.artist.name)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    // TODO: Lifecycle broadcast receiver
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}