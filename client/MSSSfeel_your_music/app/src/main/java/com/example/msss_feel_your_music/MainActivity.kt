package com.example.msss_feel_your_music

import android.annotation.SuppressLint
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
import com.example.msss_feel_your_music.utils.Recommendation
import com.google.gson.Gson
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


// Main activity of the application
class MainActivity : ComponentActivity() {
    private var internalReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == getString(R.string.intent_spotify_connected)){
                val isConnected = intent.getBooleanExtra("result",false)
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
                spotifyService.test()
            }
        }
    }
    private var spotifyReceiver = PlayerBroadcastReceiver()
    private lateinit var spotifyService: SpotifyService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SpotifyService, cast the IBinder and get SpotifyService instance.
            val binder = service as SpotifyService.LocalBinder
            spotifyService = binder.getService()
            mBound = true
            spotifyService.connectToSpotify()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //internalReceiver setup
        val filter = IntentFilter()
        filter.addAction(getString(R.string.intent_spotify_connected))
        ContextCompat.registerReceiver(this, internalReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED)

        //spotifyReceiver setup
        val spotifyFilter = IntentFilter().apply {
            addAction(getString(R.string.intent_metadata_changed))
            addAction(getString(R.string.intent_queue_changed))
            addAction(getString(R.string.intent_playback_state_changed))}
        registerReceiver(spotifyReceiver, spotifyFilter, RECEIVER_EXPORTED)
//      DEBUG db
//      (application as? FeelYourMusicApplication)?.logDatabaseContents()
        setContentView(R.layout.main_activity)

        // TODO Prova access token for Spotify Web API
        val clientId = getString(R.string.CLIENT_ID)
        val clientSecret = R.string.CLIENT_SECRET
        val requestCode = R.integer.request_code
        val redirectUri = getString(R.string.REDIRECT_URI)

        val builder =
            AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)

        builder.setScopes(arrayOf("streaming"))
        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, requestCode, request)

    }

    // Method called when Spotify login activity returns
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct activity
        if (requestCode == R.integer.request_code) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {

                    // TODO DEBUG Access token and tracks recommendation
                    Log.d("Access Token", response.accessToken)
                    fetchRecommendations(response.accessToken, "4NHQUGzhtTLFvgF5SZesLK",
                        "classical", "0c6xIDDpzE81m2q797ordA")
                }
                AuthorizationResponse.Type.ERROR -> {
                    // Handle errors
                }
                else -> {}
            }
        }
    }

    //TODO(capire se devo chiamare unbind e simili sulla onStop o sulla onDestroy etc)
    //TODO add popup to say "enable broadcast stuff in spotify"
    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SpotifyService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }


    // When the activity is no more visible
    override fun onStop() {
        super.onStop()
//        unbindService(connection)
//        unregisterReceiver(receiver)
        mBound = false
    }

    // Get Request to the recommendation endpoint of Spotify Web API
    private fun fetchRecommendations(accessToken: String, seedArtists: String, seedGenres: String, seedTracks: String) {
        val url = "https://api.spotify.com/v1/recommendations" +
                "?limit=3" +
                "&seed_artists=$seedArtists" +
                "&seed_genres=$seedGenres" +
                "&seed_tracks=$seedTracks"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        println("Failed to get recommendations")
                        println("Response Code: ${response.code}")
                        val errorBody = response.body?.string()
                        println("Response Body: $errorBody")
                        return
                    }

                    val responseBody = response.body?.string()
                    val gson = Gson()
                    if (responseBody != null) {
                        Log.d("ResponseBody", responseBody)
                    }

                    // TODO extract tracks uri and other info from json

                    // Extract id, name and uri of each recommended track
                    val recommendations = gson.fromJson(responseBody, Recommendation::class.java)

                    recommendations.tracks.forEach { track ->
                        println("Track id: ${track.id} ")
                        println("Track name: ${track.name} ")
                        println("Track uri: ${track.uri} ")
                    }

                }
            }
        })
    }

}