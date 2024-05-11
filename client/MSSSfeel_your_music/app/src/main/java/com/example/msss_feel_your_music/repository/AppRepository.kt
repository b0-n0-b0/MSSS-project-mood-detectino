package com.example.msss_feel_your_music.repository

import androidx.annotation.WorkerThread
import com.example.msss_feel_your_music.daos.TrackInfoDao
import com.example.msss_feel_your_music.entities.TrackInfo
import kotlinx.coroutines.flow.Flow

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class AppRepository(private val trackInfoDao: TrackInfoDao) {
    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allTracks: List<TrackInfo> = trackInfoDao.getAll()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(trackInfo: TrackInfo) {
        trackInfoDao.insert(trackInfo)
    }

}