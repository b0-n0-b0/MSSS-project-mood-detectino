package com.example.msss_feel_your_music.room.repository

import androidx.annotation.WorkerThread
import com.example.msss_feel_your_music.room.daos.BlacklistDao
import com.example.msss_feel_your_music.room.entities.Blacklist

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class AppRepository(private val blacklistDao: BlacklistDao) {

    // Room executes all queries on a separate thread.
    val allTracksInBlacklist: List<Blacklist> = blacklistDao.getAll()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(blacklist: Blacklist) {
        blacklistDao.insert(blacklist)
    }

}