package com.example.msss_feel_your_music

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.msss_feel_your_music.room.database.AppDatabase
import com.example.msss_feel_your_music.room.repository.AppRepository
import com.example.msss_feel_your_music.utils.convertLongToTime
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class SpotifyService : Service() {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var oldLabel: Int? = -1
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectToSpotify(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    private fun connectToSpotify(intent: Intent?) {
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.disconnect(spotifyAppRemote);
//        var connected = true
//        val intent = Intent()
//        intent.setAction(getString(R.string.intent_spotify_connection_error))
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService","connected")
                var label = intent?.getIntExtra("label",0)
                var recommendations = intent?.getStringArrayListExtra("recommendations")
                handleRecommendations(label, recommendations)
            }
            override fun onFailure(error: Throwable) {
                Log.d("SpotifyService","not connected")
                //TODO:handle errors
            }
        })
    }

    private fun test(){
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

    private fun handleRecommendations(label: Int?, recommendations: ArrayList<String>?){
        Log.d("SpotifyService", "label $label")
        Log.d("SpotifyService", "reccomentations: $recommendations")
        spotifyAppRemote?.let {
            it.playerApi.playerState
                .setResultCallback { playerState ->

                    // TODO TOGLIERE IL CONTROLLO DEL PLAYER isPaused
                    if (!playerState.isPaused) {
                        // do stuff
                        // it.playerApi.play(recommendations?.get(0))

                        //TODO:if oldLabel != label -> empty queue
                        if (oldLabel != label) {

                        }

                        //TODO:check queue length < N -> add only if true
                    }

                    // For each recommended track
                    recommendations?.forEachIndexed { i, rec ->
                        if (i < recommendations.size){

                            // Database instance
                            val database by lazy {
                                AppDatabase.getDatabase(
                                    this,
                                    CoroutineScope(SupervisorJob())
                                )
                            }

                            // Repository instance
                            val repository by lazy {
                                AppRepository(database.BlacklistDao())
                            }

                            // Coroutine to access database
                            GlobalScope.launch(Dispatchers.IO){

                                // DEBUG CLEAN TABLE
                                // repository.deleteAll()

                                // Check if the track is in the blacklist
                                val blacklist = repository.getTrackByUri(rec)

                                // If it's not OR if skipCount < 3, add to the player queue
                                if (blacklist == null || (blacklist != null && blacklist.skipCount < 3)) {
                                    it.playerApi.queue(rec)
                                    Log.d("SpotifyService", "inserted in queue: $rec")
                                }
                                // Else, do not add it to the player queue
                                else {
                                    Log.d("SpotifyService", "track in blacklist: ${blacklist.uri}")
                                }
                            }
                        }
                    }
                }
                .setErrorCallback { throwable ->
                    //TODO: handle errors
                };
            //save label
            oldLabel = label
        }
    }

    private fun insertInQueue(songUri: String){
        spotifyAppRemote?.playerApi?.queue(songUri)
    }
}