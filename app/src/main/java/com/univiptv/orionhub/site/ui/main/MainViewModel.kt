package com.univiptv.orionhub.site.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.univiptv.orionhub.site.data.db.AppDatabase
import com.univiptv.orionhub.site.data.model.CategoryInfo
import com.univiptv.orionhub.site.data.model.Channel
import com.univiptv.orionhub.site.data.model.Playlist
import com.univiptv.orionhub.site.data.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application)

    val playlists: LiveData<List<Playlist>> = repository.getAllPlaylists()

    private val _selectedPlaylistId = MutableLiveData<Long>()

    val categories: LiveData<List<String>> = _selectedPlaylistId.switchMap { id ->
        repository.getCategoriesByPlaylist(id)
    }

    val channels: LiveData<List<Channel>> = _selectedPlaylistId.switchMap { id ->
        repository.getChannelsByPlaylist(id)
    }

    private val _selectedCategory = MutableLiveData<String?>()

    val filteredChannels: LiveData<List<Channel>> = _selectedCategory.switchMap { category ->
        val playlistId = _selectedPlaylistId.value ?: 0L
        if (category != null) {
            repository.getChannelsByCategory(playlistId, category)
        } else {
            repository.getChannelsByPlaylist(playlistId)
        }
    }

    private val _searchQuery = MutableLiveData<String?>()

    val searchResults: LiveData<List<Channel>> = _searchQuery.switchMap { query ->
        val playlistId = _selectedPlaylistId.value ?: 0L
        if (!query.isNullOrBlank()) {
            repository.searchChannels(playlistId, query)
        } else {
            repository.getChannelsByPlaylist(playlistId)
        }
    }

    private val _categoryInfoList = MutableLiveData<List<CategoryInfo>>()
    val categoryInfoList: LiveData<List<CategoryInfo>> = _categoryInfoList

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun selectPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
        _selectedCategory.value = null
        loadCategoryInfo(playlistId)
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun search(query: String?) {
        _searchQuery.value = query
    }

    fun addPlaylistFromUrl(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val id = repository.addPlaylistFromUrl(name, url)
                _selectedPlaylistId.value = id
                checkAllChannels(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPlaylistFromContent(name: String, content: String, filePath: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.addPlaylistFromContent(name, content, filePath)
                _selectedPlaylistId.value = id
                checkAllChannels(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to parse playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPlaylistFromBin(name: String, data: ByteArray, filePath: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.addPlaylistFromBin(name, data, filePath)
                _selectedPlaylistId.value = id
                checkAllChannels(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to parse file"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshPlaylist(playlistId)
                checkAllChannels(playlistId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun checkAllChannels(playlistId: Long) {
        viewModelScope.launch {
            try {
                repository.checkAllChannels(playlistId)
                loadCategoryInfo(playlistId)
            } catch (_: Exception) { }
        }
    }

    private fun loadCategoryInfo(playlistId: Long) {
        viewModelScope.launch {
            try {
                val cats = withContext(Dispatchers.IO) {
                    val allChannels = repository.getChannelsByPlaylistSync(playlistId)
                    val grouped = allChannels.groupBy { it.category }
                    grouped.map { (category, chans) ->
                        CategoryInfo(
                            name = category,
                            totalChannels = chans.size,
                            onlineChannels = chans.count { it.isOnline },
                            offlineChannels = chans.count { !it.isOnline },
                            channels = chans
                        )
                    }.sortedBy { it.name }
                }
                _categoryInfoList.value = cats
            } catch (_: Exception) { }
        }
    }

    fun downloadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val content = repository.downloadPlaylistContent(playlistId)
                if (content != null) {
                    _toastMessage.value = "DOWNLOAD_CONTENT:$content"
                } else {
                    _error.value = "No URL available for download"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Download failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearToast() { _toastMessage.value = null }
}
