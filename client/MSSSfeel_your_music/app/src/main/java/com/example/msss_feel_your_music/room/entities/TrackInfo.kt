package com.example.msss_feel_your_music.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity that represents the table int the database
// A tuple contains id and valence of a track
@Entity
data class TrackInfo(
    @PrimaryKey val tid: String,
    @ColumnInfo(name = "valence") val valence: Double
)
