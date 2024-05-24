package com.example.msss_feel_your_music

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.msss_feel_your_music.app_broadcast_receiver.PlayerBroadcastReceiver
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.spotify.android.appremote.api.SpotifyAppRemote
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

    private var spotifyReceiver = PlayerBroadcastReceiver()
    private var currentLabel = 0
    private var accessToken = ""
    private var internalReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == getString(R.string.intent_wearabledata_received)){
                currentLabel = intent.getIntExtra("label",0)
                Log.d("MainActivity","internalReceiver")

                // Get boundaries and recommended genres
                val boundaries: Boundaries = getRangeFromLabel(currentLabel)
                val recommendedGenres: String = getGenresFromLabel(currentLabel)

                // Set mood TextView
                val moodTextView: TextView = findViewById(R.id.moodTextView)
                setMoodTextView(moodTextView, currentLabel)

                // Fetch the recommended tracks
                fetchRecommendations(accessToken, recommendedGenres, boundaries)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isInstalled = SpotifyAppRemote.isSpotifyInstalled(this)
        if(!isInstalled){
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("This app needs Spotify installed on the device!")
                .setCancelable(false)
                .setPositiveButton("Got it!") { _, _ ->
                    finish()
                }
            val alert = builder.create()
            alert.show()
        }
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage("This app needs Spotify  Device Broadcast Status on!")
            .setCancelable(false)
            .setPositiveButton("Already done") { _, _ ->
            }
            .setNegativeButton("Open settings"){ _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS));
            }
        val alert = builder.create()
        alert.show()
        //internalReceiver setup
        val filter = IntentFilter()
        filter.addAction(getString(R.string.intent_spotify_connection_error))
        filter.addAction(getString(R.string.intent_wearabledata_received))
        ContextCompat.registerReceiver(this, internalReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED)

        //spotifyReceiver setup
        val spotifyFilter = IntentFilter().apply {
            addAction(getString(R.string.intent_metadata_changed))
            addAction(getString(R.string.intent_queue_changed))
            addAction(getString(R.string.intent_playback_state_changed))}
        registerReceiver(spotifyReceiver, spotifyFilter, RECEIVER_EXPORTED)
        setContentView(R.layout.main_activity)
        val startButton = findViewById<Button>(R.id.startButton)
//        val stopButton = findViewById<Button>(R.id.stopButton)
        startButton.setOnClickListener {
            sendMessage("/start","start")
            val serviceIntent = MessageService.createIntent(this@MainActivity)
            startService(serviceIntent)
        }

//        stopButton.setOnClickListener {
//            sendMessage("/stop","stop")
//            val serviceIntent = MessageService.createIntent(this@MainActivity)
//            stopService(serviceIntent)
//        }
    }

    private fun webApiLogin(){
        Log.d("MainActivity","webApiLogin")
        val clientId = getString(R.string.CLIENT_ID)
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
        Log.d("MainActivity","onActivityResult")

        // Check if result comes from the correct activity
        if (requestCode == R.integer.request_code) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    Log.d("Access Token", response.accessToken)
                    accessToken = response.accessToken
                }
                AuthorizationResponse.Type.ERROR -> {
                    // Handle errors
                }
                else -> {}
            }
        }
    }

    override fun onStart() {
        super.onStart()
        webApiLogin()
    }


    // When the activity is no more visible
    override fun onStop() {
        super.onStop()
    }

    // Get Request to the recommendation endpoint of Spotify Web API
    private fun fetchRecommendations(accessToken: String, seedGenres: String, boundaries: Boundaries) {
        val url = "https://api.spotify.com/v1/recommendations" +
                "?limit=2" +
                //"&seed_artists=$seedArtists" +
                "&seed_genres=$seedGenres" +
                //"&seed_tracks=$seedTracks" +
                "&min_energy=${boundaries.eLow}" +
                "&max_energy=${boundaries.eHigh}" +
                "&min_valence=${boundaries.vLow}" +
                "&max_valence=${boundaries.vHigh}"

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

                    // Extract id, name and uri of each recommended track
                    val recommendations = gson.fromJson(responseBody, Recommendation::class.java)
                    val recs : ArrayList<String> = ArrayList<String>()
                    recommendations.tracks.forEach { track ->
                        recs.add(track.uri)
                    }
                    val intent = Intent(this@MainActivity, SpotifyService::class.java)
                    intent.putExtra("recommendations", recs)
                    intent.putExtra("label", currentLabel)
                    startService(intent)
                }
            }
        })
    }

    // Utility function emotion_label -> bounds (valence and energy)
    data class Boundaries(val vLow: Double, val vHigh: Double, val eLow: Double, val eHigh: Double)
    private fun getRangeFromLabel(label: Int): Boundaries{
    // Emotion labels: Neutral  Calm    Tired   Tension Excited
    //                 0        1       2       3       4
    //      Valence    0.4-0.6  0.6-1   0-0.4   0-0.4   0.6-1
    //      Energy     0.4-0.6  0-0.4   0-0.4   0.6-1   0.6-1
        val lowerBoundLeft = getString(R.string.lower_bound_left).toDouble()
        val lowerBoundRight = getString(R.string.lower_bound_right).toDouble()
        val middleBoundLeft = getString(R.string.middle_bound_left).toDouble()
        val middleBoundRight = getString(R.string.middle_bound_right).toDouble()
        val highBoundLeft = getString(R.string.high_bound_left).toDouble()
        val highBoundRight = getString(R.string.high_bound_right).toDouble()

        return when (label) {
            0 -> Boundaries(vLow = middleBoundLeft, vHigh = middleBoundRight, eLow = middleBoundLeft, eHigh = middleBoundRight)
            1 -> Boundaries(vLow = highBoundLeft, vHigh = highBoundRight, eLow = lowerBoundLeft, eHigh = lowerBoundRight)
            2 -> Boundaries(vLow = lowerBoundLeft, vHigh = lowerBoundRight, eLow = lowerBoundLeft, eHigh = lowerBoundRight)
            3 -> Boundaries(vLow = lowerBoundLeft, vHigh = lowerBoundRight, eLow = highBoundLeft, eHigh = highBoundRight)
            4 -> Boundaries(vLow = highBoundLeft, vHigh = highBoundRight, eLow = highBoundLeft, eHigh = highBoundRight)
            else -> throw IllegalArgumentException("Input must be between 0 and 4")
        }
    }

    // Utility function to map label to genres
    private fun getGenresFromLabel(label: Int): String {
        // Mapping label -> genres
        val recommendedGenres = when (label) {
            0 -> getString(R.string.neutral_genres)
            1 -> getString(R.string.calm_genres)
            2 -> getString(R.string.tired_genres)
            3 -> getString(R.string.tension_genres)
            4 -> getString(R.string.excited_genres)
            else -> getString(R.string.neutral_genres)
        }
        Log.d("MainActivity","Recommended genres: $recommendedGenres")

        return recommendedGenres
    }

    // Utility function to update the mood TextView
    private fun setMoodTextView(moodTextView: TextView, label: Int) {

        val mood = when (label) {
            0 -> getString(R.string.neutral)
            1 -> getString(R.string.calm)
            2 -> getString(R.string.tired)
            3 -> getString(R.string.tension)
            4 -> getString(R.string.excited)
            else -> getString(R.string.no_mood)
        }
        moodTextView.text = mood
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(internalReceiver)
    }


    fun sendMessage(messagePath: String, data: String) {
        val nodeIdsTask: Task<List<Node>> = Wearable.getNodeClient(this).connectedNodes
        val byteArray = data.toByteArray(Charsets.UTF_8)
        nodeIdsTask.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d(ContentValues.TAG, "nodo: $node.id")
                val sendMessageTask = Wearable.getMessageClient(this).sendMessage(node.id, messagePath, byteArray)
                sendMessageTask.addOnSuccessListener {
                    Log.d(ContentValues.TAG, "Message sent successfully")
                }.addOnFailureListener { exception ->
                    Log.e(ContentValues.TAG, "Failed to send message", exception)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e(ContentValues.TAG, "Failed to get node IDs", exception)
        }
    }

}