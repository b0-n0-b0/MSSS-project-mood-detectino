package com.example.msss_feel_your_music

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.msss_feel_your_music.room.database.AppDatabase
import com.example.msss_feel_your_music.room.repository.AppRepository
import com.example.msss_feel_your_music.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FeelYourMusicApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { AppRepository(database.BlacklistDao()) }

    // When the application is created
    override fun onCreate() {
        super.onCreate()

        // logDatabaseContents()
    }

    // DEBUG to show the track stored in the database
    fun logDatabaseContents(){
        GlobalScope.launch(Dispatchers.IO){

            val blacklistItems = repository.allTracksInBlacklist

            for (blacklist in blacklistItems) {
                Log.d("FYMApp", "tid: ${blacklist.uri}")
                Log.d("FYMApp", "skipCount: ${blacklist.skipCount}")
            }
            Log.d("FYMApp", "Tracks in db: $blacklistItems.")
        }
    }
}