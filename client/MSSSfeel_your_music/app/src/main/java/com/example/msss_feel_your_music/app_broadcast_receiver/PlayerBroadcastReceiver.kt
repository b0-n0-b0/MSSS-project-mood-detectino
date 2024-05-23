package com.example.msss_feel_your_music.app_broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.msss_feel_your_music.room.database.AppDatabase
import com.example.msss_feel_your_music.room.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


open class PlayerBroadcastReceiver : BroadcastReceiver() {

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

    // TODO Skip check
    internal object TrackStatus {
        var trackStartTime: Long = 0
        var skipLimitMills: Long = 30*1000
    }

    private fun trackIsSkipped(timeSent: Long): Boolean{


        Log.d("PlayerBroadcastReceiver","OLD_trackStartTime ${TrackStatus.trackStartTime}")
        Log.d("PlayerBroadcastReceiver","timeSent ${TrackStatus.trackStartTime}")
        return timeSent < (TrackStatus.trackStartTime + TrackStatus.skipLimitMills)
    }

    //TODO:handle spotify callbacks for blacklist
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
        // Repository instance
        val repository by lazy {
            AppRepository(database.BlacklistDao())
        }
        if (action == BroadcastTypes.METADATA_CHANGED) {
            val trackId = intent.getStringExtra("id") // URI
            val artistName = intent.getStringExtra("artist")
            // val albumName = intent.getStringExtra("album")
            // val trackName = intent.getStringExtra("track")
            // val timeSent = intent.getLongExtra("timeSent", 0)

            // Check if the previous track is skipped in the first 30sec.
            val skipped = trackIsSkipped(timeSentInMs)

            // Update variables with new song info
            TrackStatus.trackStartTime = timeSentInMs

            Log.d("PlayerBroadcastReceiver","skipped $skipped")
            Log.d("PlayerBroadcastReceiver","track id $trackId")
            Log.d("PlayerBroadcastReceiver","artistName $artistName")
            Log.d("PlayerBroadcastReceiver","trackStartTime $TrackStatus.trackStartTime")

            // If the song is skipped, update skipCount
            if(skipped){
                // Coroutine to access database
                GlobalScope.launch(Dispatchers.IO){
                    if (trackId != null) {
                        val blacklist = repository.getTrackByUri(trackId)
                        if(blacklist!=null){
                            repository.updateSkipCount(trackId, blacklist.skipCount + 1)
                            Log.d("PlayerBroadcastReceiver", "Track $trackId updated: skipCount ${blacklist.skipCount+1}")
                        } else if(blacklist==null){
                            repository.insert(trackId)
                            Log.d("PlayerBroadcastReceiver", "New Track $trackId in blacklist")
                        }
                    }
                }
            }

        } else if (action == BroadcastTypes.PLAYBACK_STATE_CHANGED) {
            val playing = intent.getBooleanExtra("playing", false)
            val positionInMs = intent.getIntExtra("playbackPosition", 0)
            // Do something with extracted information
        } else if (action == BroadcastTypes.QUEUE_CHANGED) {
            // Sent only as a notification, your app may want to respond accordingly.
        }
    }
}

