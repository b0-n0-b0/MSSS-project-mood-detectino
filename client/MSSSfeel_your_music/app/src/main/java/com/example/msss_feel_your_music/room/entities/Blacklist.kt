package com.example.msss_feel_your_music.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity that represents the table int the database
// A tuple contains id, skipCount and timestamp of a track
@Entity
data class Blacklist(
    @PrimaryKey val tid: String,
    @ColumnInfo(name = "skipCount") val skipCount: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
