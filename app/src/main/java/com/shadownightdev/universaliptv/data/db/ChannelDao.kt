package com.shadownightdev.universaliptv.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shadownightdev.universaliptv.data.model.Channel

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: Channel): Long

    @Update
    suspend fun update(channel: Channel)

    @Delete
    suspend fun delete(channel: Channel)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY category, name")
    fun getChannelsByPlaylist(playlistId: Long): LiveData<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND category = :category ORDER BY name")
    fun getChannelsByCategory(playlistId: Long, category: String): LiveData<List<Channel>>

    @Query("SELECT DISTINCT category FROM channels WHERE playlistId = :playlistId ORDER BY category")
    fun getCategoriesByPlaylist(playlistId: Long): LiveData<List<String>>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND category = :category")
    suspend fun getChannelCountByCategory(playlistId: Long, category: String): Int

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND category = :category AND isOnline = 1")
    suspend fun getOnlineCountByCategory(playlistId: Long, category: String): Int

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND category = :category AND isOnline = 0")
    suspend fun getOfflineCountByCategory(playlistId: Long, category: String): Int

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name")
    fun searchChannels(playlistId: Long, query: String): LiveData<List<Channel>>

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelsByPlaylistSync(playlistId: Long): List<Channel>

    @Query("UPDATE channels SET isOnline = :isOnline, lastChecked = :timestamp WHERE id = :channelId")
    suspend fun updateStatus(channelId: Long, isOnline: Boolean, timestamp: Long)
}
