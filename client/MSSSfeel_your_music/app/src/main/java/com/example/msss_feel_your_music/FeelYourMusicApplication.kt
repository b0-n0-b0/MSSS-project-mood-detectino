package com.example.msss_feel_your_music

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.msss_feel_your_music.database.AppDatabase
import com.example.msss_feel_your_music.entities.TrackInfo
import com.example.msss_feel_your_music.repository.AppRepository
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
    val repository by lazy { AppRepository(database.trackInfoDao()) }

    override fun onCreate() {
        super.onCreate()

        logDatabaseContents()
    }

    private fun logDatabaseContents(){
        GlobalScope.launch(Dispatchers.IO){

            val tracks = repository.allTracks
            Log.d("FYMApp", "Tracks in db: $tracks")
        }
    }
}