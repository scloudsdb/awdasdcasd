package com.univiptv.orionhub.site.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.univiptv.orionhub.site.data.model.Playlist

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): LiveData<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Query("UPDATE playlists SET totalChannels = :total, onlineChannels = :online, offlineChannels = :offline WHERE id = :id")
    suspend fun updateCounts(id: Long, total: Int, online: Int, offline: Int)

    @Query("UPDATE playlists SET isLoading = :loading WHERE id = :id")
    suspend fun setLoading(id: Long, loading: Boolean)
}
