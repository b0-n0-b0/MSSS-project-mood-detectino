package com.example.msss_feel_your_music

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.types.Track

class SpotifyService : Service() {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): SpotifyService = this@SpotifyService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("SpotifyService","bind executed")
        return binder
    }

    fun connectToSpotify(): Boolean {
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.disconnect(spotifyAppRemote);
        var connected = true
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService","connected")
            }
            override fun onFailure(error: Throwable) {
                Log.d("SpotifyService","not connected")
                when (error) {
                    is CouldNotFindSpotifyApp -> {
                        connected = false
                    }
                }
            }
        })
        return connected
    }
    fun test(){
        spotifyAppRemote?.let {
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

    fun insertInQueue(songId: String){
        spotifyAppRemote?.playerApi?.queue(songId)
    }
}