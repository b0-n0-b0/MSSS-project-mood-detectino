package com.example.msss_feel_your_music

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.protocol.types.Track
import android.content.IntentFilter
import com.example.msss_feel_your_music.app_broadcast_receiver.PlayerBroadcastReceiver

// To connect and interact with Spotify
private var spotifyAppRemote: SpotifyAppRemote? = null

// Main activity of the application
class MainActivity : ComponentActivity() {

    // BroadcastReceiver that receives Spotify player changes
    private val receiver = PlayerBroadcastReceiver()

    // When the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO Receiver
        // Intent filter with the actions I am interested in
        val filter = IntentFilter().apply {
            addAction(getString(R.string.intent_metadata_changed))
            addAction(getString(R.string.intent_queue_changed))
            addAction(getString(R.string.intent_playback_state_changed))
        }

        // BroadcastReceiver registration
        // It receives external intents from Spotify
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)

        // TODO Access db
        //   DEBUG Interaction with application database in the activity
        (application as? FeelYourMusicApplication)?.logDatabaseContents()

    }

    // When the activity is visible
    override fun onStart() {
        super.onStart()

        // Initialization of connection parameters
        // obtained registering the app on the Spotify Dashboard
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        // NOTE: tries to connect to the spotify application
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {

            // Connection successful
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                connected()
            }

            // Connection failed
            override fun onFailure(error: Throwable) {
                Log.d("Main Activity","Failed to connect: $error")
                when (error) {
                    // Spotify is not installed on the device
                    is CouldNotFindSpotifyApp -> {
                        if (!SpotifyAppRemote.isSpotifyInstalled(applicationContext)){
                            // Show an alert the notify the user
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
                            Log.d("spotifyLogin","Success: $result")
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

    // When the activity is no more visible
    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    // TODO: Lifecycle broadcast receiver
    // When the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver
        unregisterReceiver(receiver)
    }
}