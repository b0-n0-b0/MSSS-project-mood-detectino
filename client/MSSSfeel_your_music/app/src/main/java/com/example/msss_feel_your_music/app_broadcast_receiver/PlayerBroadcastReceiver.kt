package com.example.msss_feel_your_music.app_broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.example.msss_feel_your_music.R
import com.example.msss_feel_your_music.room.database.AppDatabase
import com.example.msss_feel_your_music.room.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


// BroadcastReceiver to receive Spotify intents
open class PlayerBroadcastReceiver : BroadcastReceiver() {

    // To check the track status
    internal object TrackStatus {
        var trackStartTime: Long = 0
        var skipLimitMills: Long = 30*1000 // 30 seconds is the limit within a track is considered skipped
    }

    // Method that check if a track is skipped
    private fun trackIsSkipped(timeSent: Long): Boolean{
        return timeSent < (TrackStatus.trackStartTime + TrackStatus.skipLimitMills)
    }

    // When an intent is received
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PlayerBroadcastReceiver","Intent: $intent")
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        val timeSentInMs = intent.getLongExtra("timeSent", 0L)
        val action = intent.action

        // Database instance
        val database by lazy {
            AppDatabase.getDatabase(
                context,
                CoroutineScope(SupervisorJob())
            )
        }

        // Repository instance to access database
        val repository by lazy {
            AppRepository(database.BlacklistDao())
        }

        // Check the action of the intent
        if (action == getString(context, R.string.intent_metadata_changed)) {
            val trackId = intent.getStringExtra("id") // URI
            val artistName = intent.getStringExtra("artist")

            // Check if the previous track is skipped in the first 30sec.
            val skipped = trackIsSkipped(timeSentInMs)

            // Update variables with new song info
            TrackStatus.trackStartTime = timeSentInMs

            Log.d("PlayerBroadcastReceiver","track id $trackId") // N.B. IT'S THE URI
            Log.d("PlayerBroadcastReceiver","skipped $skipped")

            // If the song is skipped, update skipCount
            if (skipped) {
                // Coroutine to access database
                GlobalScope.launch(Dispatchers.IO){

                    if (trackId != null) {
                        // Check if the track is already in blacklist
                        val blacklist = repository.getTrackByUri(trackId)
                        // If it is, increase the skip counter
                        if (blacklist != null) {
                            repository.updateSkipCount(trackId, blacklist.skipCount + 1)
                            Log.d("PlayerBroadcastReceiver", "Track $trackId updated: skipCount ${blacklist.skipCount + 1}")
                        }
                        // If it is not, insert it with skipCount = 1
                        else if (blacklist == null) {
                            repository.insert(trackId)
                            Log.d("PlayerBroadcastReceiver", "New Track $trackId in blacklist")
                        }
                    }

                }
            }

        }
    }
}

