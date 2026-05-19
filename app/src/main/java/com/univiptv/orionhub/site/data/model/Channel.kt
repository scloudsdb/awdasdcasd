package com.univiptv.orionhub.site.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("category")]
)
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String = "Uncategorized",
    val language: String? = null,
    val drmLicenseUrl: String? = null,
    val drmKeyId: String? = null,
    val drmKey: String? = null,
    val drmType: String? = null,
    val imageUrl: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    val isOnline: Boolean = true,
    val lastChecked: Long = 0
)
