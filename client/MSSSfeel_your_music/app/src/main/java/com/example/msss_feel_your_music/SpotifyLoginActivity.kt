package com.example.msss_feel_your_music

import android.content.Intent
import android.os.Bundle
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
        val REDIRECT_URI = getString(R.string.REDIRECT_URI)
        val CLIENT_ID = getString(R.string.CLIENT_ID)
        val builder =
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)

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
                    setContent{
                        Text("Success! Token:"+resultCode)
                    }
                }
                AuthorizationResponse.Type.ERROR -> {
                    setContent{
                        Text("Error! Token:"+resultCode)
                    }
                }
                else -> {
                    setContent{
                        Text("Something else! Token:"+resultCode)
                    }
                }
            }
        }
    }

}