package com.univiptv.orionhub.site.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String? = null,
    val filePath: String? = null,
    val totalChannels: Int = 0,
    val onlineChannels: Int = 0,
    val offlineChannels: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)
