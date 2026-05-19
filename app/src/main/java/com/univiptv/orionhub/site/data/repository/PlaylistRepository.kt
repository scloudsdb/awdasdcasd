package com.univiptv.orionhub.site.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.univiptv.orionhub.site.data.db.AppDatabase
import com.univiptv.orionhub.site.data.model.Channel
import com.univiptv.orionhub.site.data.model.Playlist
import com.univiptv.orionhub.site.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PlaylistRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getAllPlaylists(): LiveData<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getChannelsByPlaylist(playlistId: Long): LiveData<List<Channel>> =
        channelDao.getChannelsByPlaylist(playlistId)

    fun getChannelsByCategory(playlistId: Long, category: String): LiveData<List<Channel>> =
        channelDao.getChannelsByCategory(playlistId, category)

    fun getCategoriesByPlaylist(playlistId: Long): LiveData<List<String>> =
        channelDao.getCategoriesByPlaylist(playlistId)

    fun searchChannels(playlistId: Long, query: String): LiveData<List<Channel>> =
        channelDao.searchChannels(playlistId, query)

    suspend fun addPlaylistFromUrl(name: String, url: String): Long {
        val playlistId = playlistDao.insert(Playlist(name = name, url = url, isLoading = true))
        try {
            val content = downloadContent(url)
            val channels = M3UParser.parse(content, playlistId)
            channelDao.insertAll(channels)
            updatePlaylistCounts(playlistId)
        } catch (e: Exception) {
            playlistDao.setLoading(playlistId, false)
            throw e
        }
        playlistDao.setLoading(playlistId, false)
        return playlistId
    }

    suspend fun addPlaylistFromContent(name: String, content: String, filePath: String? = null): Long {
        val playlistId = playlistDao.insert(Playlist(name = name, filePath = filePath, isLoading = true))
        try {
            val channels = M3UParser.parse(content, playlistId)
            channelDao.insertAll(channels)
            updatePlaylistCounts(playlistId)
        } catch (e: Exception) {
            playlistDao.setLoading(playlistId, false)
            throw e
        }
        playlistDao.setLoading(playlistId, false)
        return playlistId
    }

    suspend fun addPlaylistFromBin(name: String, data: ByteArray, filePath: String? = null): Long {
        val playlistId = playlistDao.insert(Playlist(name = name, filePath = filePath, isLoading = true))
        try {
            val channels = M3UParser.parseBinFile(data, playlistId)
            channelDao.insertAll(channels)
            updatePlaylistCounts(playlistId)
        } catch (e: Exception) {
            playlistDao.setLoading(playlistId, false)
            throw e
        }
        playlistDao.setLoading(playlistId, false)
        return playlistId
    }

    suspend fun refreshPlaylist(playlistId: Long) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val url = playlist.url ?: return
        playlistDao.setLoading(playlistId, true)
        try {
            val content = downloadContent(url)
            channelDao.deleteByPlaylist(playlistId)
            val channels = M3UParser.parse(content, playlistId)
            channelDao.insertAll(channels)
            updatePlaylistCounts(playlistId)
        } finally {
            playlistDao.setLoading(playlistId, false)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

    suspend fun checkChannelStatus(channelId: Long, url: String) {
        val isOnline = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).head().build()
                val response = httpClient.newCall(request).execute()
                response.close()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
        channelDao.updateStatus(channelId, isOnline, System.currentTimeMillis())
    }

    suspend fun checkAllChannels(playlistId: Long) {
        playlistDao.setLoading(playlistId, true)
        try {
            val channels = channelDao.getChannelsByPlaylistSync(playlistId)
            for (channel in channels) {
                checkChannelStatus(channel.id, channel.url)
            }
            updatePlaylistCounts(playlistId)
        } finally {
            playlistDao.setLoading(playlistId, false)
        }
    }

    suspend fun getChannelsByPlaylistSync(playlistId: Long): List<Channel> =
        channelDao.getChannelsByPlaylistSync(playlistId)

    private suspend fun updatePlaylistCounts(playlistId: Long) {
        val channels = channelDao.getChannelsByPlaylistSync(playlistId)
        val total = channels.size
        val online = channels.count { it.isOnline }
        playlistDao.updateCounts(playlistId, total, online, total - online)
    }

    private suspend fun downloadContent(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        response.body?.string() ?: throw Exception("Empty response")
    }

    suspend fun deleteChannel(channel: Channel) {
        channelDao.delete(channel)
    }

    suspend fun deleteCategory(playlistId: Long, category: String) {
        channelDao.deleteByCategory(playlistId, category)
    }

    suspend fun downloadPlaylistContent(playlistId: Long): String? {
        val channels = channelDao.getChannelsByPlaylistSync(playlistId)
        if (channels.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            M3UParser.exportOnlineChannels(channels)
        }
    }
}
