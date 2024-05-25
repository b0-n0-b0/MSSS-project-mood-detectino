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


// Service that connects and communicates with Spotify SDK
class SpotifyService : Service() {

    // To connect to Spotify SDK
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Start Service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectToSpotify(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    // Method that request a connection with Spotify SDK
    private fun connectToSpotify(intent: Intent?) {

        // Connection parameters
        val clientId = getString(R.string.CLIENT_ID)
        val redirectUri = getString(R.string.REDIRECT_URI)
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        // Try to disconnect and then connect to Spotify SDK (default approach)
        SpotifyAppRemote.disconnect(spotifyAppRemote);
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {

            // Connection successfull
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService","connected")
                var label = intent?.getIntExtra("label",0)
                var recommendations = intent?.getStringArrayListExtra("recommendations")
                handleRecommendations(label, recommendations)
            }
            // Connection failed
            override fun onFailure(error: Throwable) {
                Log.d("SpotifyService","not connected")
            }
        })
    }

    // Method that handles the recommendation (check blacklist and add in queue)
    private fun handleRecommendations(label: Int?, recommendations: ArrayList<String>?){
        spotifyAppRemote?.let {
            it.playerApi.playerState
                .setResultCallback { playerState ->

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
                    Log.d("SpotifyService", "Error during recommendations")
                }
        }
    }
}