package com.example.msss_feel_your_music

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track


private var spotifyAppRemote: SpotifyAppRemote? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)

        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.disconnect(spotifyAppRemote);
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                connected()
            }
            override fun onFailure(error: Throwable) {
                Log.d("Main Activity","Failed to connect")
                showSpotifyLogin()
            }
        })
    }

    private val spotifyLoginLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        //TODO: add result handling
    }

    private fun showSpotifyLogin(){
        setContentView(R.layout.authorize_app)
        val button = findViewById<Button>(R.id.connectSpotifyButton)
        button.setOnClickListener {
            val intent = Intent(this, SpotifyLoginActivity::class.java)
            spotifyLoginLauncher.launch(intent)
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
}