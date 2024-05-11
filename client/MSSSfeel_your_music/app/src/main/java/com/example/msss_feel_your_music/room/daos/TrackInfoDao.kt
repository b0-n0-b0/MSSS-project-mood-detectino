package com.example.msss_feel_your_music.room.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.msss_feel_your_music.room.entities.TrackInfo
import kotlinx.coroutines.flow.Flow

// Dao for TrackInfo table
@Dao
interface TrackInfoDao {

    // Retrieve all the tracks of the table
    @Query("SELECT * FROM trackInfo")
    fun getAll(): List<TrackInfo>

    // Retrieve the tracks with the given trackIds
    @Query("SELECT * FROM trackInfo WHERE tid IN (:trackIds)")
    fun getAllByIds(trackIds: IntArray): List<TrackInfo>

    // Retrieve the tracks with the valence in a specified range
    @Query("SELECT * FROM trackInfo WHERE valence >= :minValence AND valence < :maxValence")
    fun getTrackByValence(minValence: Double, maxValence: Double): List<TrackInfo>

    // Insert a list of given tracks
    @Insert
    fun insertAll(vararg tracksInfo: TrackInfo)

    // Insert a given track
    @Insert
    suspend fun insert(trackInfo: TrackInfo)

    // Delete a specified track
    @Delete
    fun delete(track: TrackInfo)

    // Delete all the track in the table
    @Query("DELETE FROM trackInfo")
    suspend fun deleteAll()
}