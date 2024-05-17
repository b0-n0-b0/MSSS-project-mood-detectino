package com.example.msss_feel_your_music.utils

// Data class to receive and access recommended tracks
data class Recommendation(
    val tracks: List<Track>,
)

// Track info
data class Track(
    val id: String,
    val name: String,
    val uri: String
)


