package com.example.msss_feel_your_music.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TrackInfo(
    @PrimaryKey val tid: String,
    @ColumnInfo(name = "valence") val valence: Double
)
