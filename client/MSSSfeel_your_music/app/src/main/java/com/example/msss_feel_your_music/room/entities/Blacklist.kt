package com.example.msss_feel_your_music.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity that represents the Blacklist table in the database
// A tuple contains uri and skipCount of a track
@Entity
data class Blacklist(
    @PrimaryKey val uri: String,
    @ColumnInfo(name = "skipCount") val skipCount: Int
)
