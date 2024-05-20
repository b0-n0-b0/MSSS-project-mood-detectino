package com.example.msss_feel_your_music.room.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.msss_feel_your_music.room.entities.Blacklist

// Dao for Blacklist table
@Dao
interface BlacklistDao {

    // Retrieve all the tracks of the table
    @Query("SELECT * FROM blacklist")
    fun getAll(): List<Blacklist>

    // Retrieve the tracks with the given trackIds
    @Query("SELECT * FROM blacklist WHERE tid IN (:trackIds)")
    fun getAllByIds(trackIds: IntArray): List<Blacklist>

    // Retrieve the tracks with the valence in a specified range
    // @Query("SELECT * FROM blacklist WHERE valence >= :minValence AND valence < :maxValence")
    // fun getTrackByValence(minValence: Double, maxValence: Double): List<Blacklist>

    // Insert a list of given tracks
    @Insert
    fun insertAll(vararg blacklists: Blacklist)

    // Insert a given track
    @Insert
    suspend fun insert(blacklist: Blacklist)

    // Delete a specified track
    @Delete
    fun delete(blacklist: Blacklist)

    // Delete all the track in the table
    @Query("DELETE FROM blacklist")
    suspend fun deleteAll()
}