package com.example.msss_feel_your_music

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyLoginActivity : ComponentActivity() {
    private val REQUEST_CODE = 1337
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val redirect_uri = getString(R.string.REDIRECT_URI)
        val client_id = getString(R.string.CLIENT_ID)
        val builder =
            AuthorizationRequest.Builder(client_id, AuthorizationResponse.Type.TOKEN, redirect_uri)

        builder.setScopes(arrayOf("streaming"))
        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    Log.println(Log.INFO,"Spotify Login", "Login successful")
                }
                // TODO: handle errors in a logical way
                AuthorizationResponse.Type.ERROR -> {
                    Log.println(Log.INFO,"Spotify Login", "Login Error")
                }
                else -> {
                    Log.println(Log.INFO,"Spotify Login", "Something went wrong")
                }
            }
        }
    }

}