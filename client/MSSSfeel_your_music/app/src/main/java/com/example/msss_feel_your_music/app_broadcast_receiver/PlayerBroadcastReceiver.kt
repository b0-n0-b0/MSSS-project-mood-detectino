package com.example.msss_feel_your_music.app_broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

public open class PlayerBroadcastReceiver : BroadcastReceiver() {

    // Intents this BroadcastReceiver can receive
    internal object BroadcastTypes {
        const val SPOTIFY_PACKAGE = "com.spotify.music"

        // This intent is sent when a new track starts playing
        const val METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged"

        // This intent is sent when the play queue is changed
        const val QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged"

        // This intent is sent whenever the user presses play/pause, or when seeking the track position
        const val PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged"
    }

    // When an intent is received
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PlayerBroadcastReceiver","Intent: $intent")
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        val timeSentInMs = intent.getLongExtra("timeSent", 0L)
        val action = intent.action
        if (action == BroadcastTypes.METADATA_CHANGED) {
            val trackId = intent.getStringExtra("id")
            val artistName = intent.getStringExtra("artist")
            val albumName = intent.getStringExtra("album")
            val trackName = intent.getStringExtra("track")
            val trackLengthInSec = intent.getIntExtra("length", 0)

            // Do something with extracted information...

            Log.d("PlayerBroadcastReceiver","track id $trackId")
            Log.d("PlayerBroadcastReceiver","artistName $artistName")

        } else if (action == BroadcastTypes.PLAYBACK_STATE_CHANGED) {
            val playing = intent.getBooleanExtra("playing", false)
            val positionInMs = intent.getIntExtra("playbackPosition", 0)
            // Do something with extracted information
        } else if (action == BroadcastTypes.QUEUE_CHANGED) {
            // Sent only as a notification, your app may want to respond accordingly.
        }
    }
}
