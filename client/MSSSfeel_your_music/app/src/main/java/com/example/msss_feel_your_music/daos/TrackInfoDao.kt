package com.example.msss_feel_your_music.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.msss_feel_your_music.entities.TrackInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackInfoDao {
    @Query("SELECT * FROM trackInfo")
    fun getAll(): List<TrackInfo>

    @Query("SELECT * FROM trackInfo WHERE tid IN (:trackIds)")
    fun getAllByIds(trackIds: IntArray): List<TrackInfo>

    @Query("SELECT * FROM trackInfo WHERE valence >= :minValence AND valence < :maxValence")
    fun getTrackByValence(minValence: Double, maxValence: Double): List<TrackInfo>

    @Insert
    fun insertAll(vararg tracksInfo: TrackInfo)

    @Insert
    suspend fun insert(trackInfo: TrackInfo)

    @Delete
    fun delete(track: TrackInfo)

    @Query("DELETE FROM trackInfo")
    suspend fun deleteAll()
}