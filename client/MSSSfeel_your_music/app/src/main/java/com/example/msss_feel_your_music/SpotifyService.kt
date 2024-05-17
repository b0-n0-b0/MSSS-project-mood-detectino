package com.example.msss_feel_your_music

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.protocol.types.Track
import com.spotify.sdk.android.auth.AccountsQueryParameters.CLIENT_ID
import com.spotify.sdk.android.auth.AccountsQueryParameters.REDIRECT_URI
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException


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

    fun connectToSpotify() {
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.disconnect(spotifyAppRemote);
        var connected = true
        val intent = Intent()
        intent.setAction("com.example.msss_feel_your_music.spotifyConnectionAction")
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService","connected")
                intent.putExtra("result", connected)
                sendBroadcast(intent)
            }
            override fun onFailure(error: Throwable) {
                Log.d("SpotifyService","not connected")
                when (error) {
                    is CouldNotFindSpotifyApp -> {
                        connected = false
                    }
                }
                intent.putExtra("result", connected)
                sendBroadcast(intent)
            }
        })
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