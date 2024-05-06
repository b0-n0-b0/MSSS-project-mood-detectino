package com.example.msss_feel_your_music

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.spotify.android.appremote.api.SpotifyAppRemote


private var spotifyAppRemote: SpotifyAppRemote? = null

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button_click)
        button.setOnClickListener {
            val intent = Intent(this, SpotifyLoginActivity::class.java)
            startActivity(intent)
        }
    }

//    override fun onStart() {
//        super.onStart()
//        val connectionParams = ConnectionParams.Builder(clientId)
//            .setRedirectUri(redirectUri)
//            .showAuthView(true)
//            .build()
//
//        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
//            override fun onConnected(appRemote: SpotifyAppRemote) {
//                spotifyAppRemote = appRemote
//                Log.d("ConnectionToSpotify", "Connected! Yay!")
//                connected()
//            }
//
//            override fun onFailure(throwable: Throwable) {
//                Log.e("ConnectionToSpotify", throwable.message, throwable)
//                setContent{
//                    Text(""+throwable)
//                }
//            }
//        })
//    }
//
//    private fun connected() {
//        spotifyAppRemote?.let { it ->
//            // Play a playlist
//            val playlistURI = "spotify:playlist:37i9dQZF1DX2sUQwD7tbmL"
//            it.playerApi.play(playlistURI)
//            // Subscribe to PlayerState
//            it.playerApi.subscribeToPlayerState().setEventCallback {
//                val track: Track = it.track
//                Log.d("MainActivity", track.name + " by " + track.artist.name)
//            }
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        spotifyAppRemote?.let {
//            SpotifyAppRemote.disconnect(it)
//        }
//    }
}