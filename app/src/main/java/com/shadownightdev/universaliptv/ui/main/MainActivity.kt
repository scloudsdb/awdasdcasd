package com.shadownightdev.universaliptv.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.shadownightdev.universaliptv.R
import com.shadownightdev.universaliptv.data.model.CategoryInfo
import com.shadownightdev.universaliptv.data.model.Channel
import com.shadownightdev.universaliptv.data.model.Playlist
import com.shadownightdev.universaliptv.databinding.ActivityMainBinding
import com.shadownightdev.universaliptv.ui.player.PlayerActivity
import com.shadownightdev.universaliptv.ui.settings.SettingsActivity
import com.shadownightdev.universaliptv.util.LocaleHelper
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private val playlistAdapter = PlaylistAdapter(
        onPlaylistClick = { playlist -> onPlaylistSelected(playlist) },
        onMenuClick = { playlist, view -> showPlaylistMenu(playlist, view) }
    )

    private val categoryAdapter = CategoryAdapter { categoryInfo ->
        onCategorySelected(categoryInfo)
    }

    private val channelAdapter = ChannelAdapter { channel ->
        openPlayer(channel)
    }

    private enum class ViewMode { PLAYLISTS, CATEGORIES, CHANNELS }
    private var currentMode = ViewMode.PLAYLISTS
    private var currentPlaylistId: Long = -1

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                uris.add(uri)
            }
            if (uris.isNotEmpty()) {
                importFiles(uris)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupUrlInput()
        setupSwipeRefresh()
        observeViewModel()

        showPlaylistsView()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_search -> {
                    val searchView = menuItem.actionView as? SearchView
                    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false
                        override fun onQueryTextChange(newText: String?): Boolean {
                            if (currentMode == ViewMode.CHANNELS) {
                                viewModel.search(newText)
                            }
                            return true
                        }
                    })
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupUrlInput() {
        binding.btnLoadUrl.setOnClickListener {
            val url = binding.urlInput.text?.toString()?.trim()
            if (url.isNullOrEmpty()) {
                Toast.makeText(this, R.string.please_enter_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showNameDialog { name ->
                viewModel.addPlaylistFromUrl(name, url)
                binding.urlInput.text?.clear()
            }
        }

        binding.btnImportFile.setOnClickListener {
            openFilePicker()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            if (currentMode != ViewMode.PLAYLISTS && currentPlaylistId > 0) {
                viewModel.refreshPlaylist(currentPlaylistId)
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewModel.playlists.observe(this) { playlists ->
            if (currentMode == ViewMode.PLAYLISTS) {
                playlistAdapter.submitList(playlists)
                binding.emptyState.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.categoryInfoList.observe(this) { categories ->
            if (currentMode == ViewMode.CATEGORIES) {
                categoryAdapter.submitList(categories)
                binding.emptyState.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.filteredChannels.observe(this) { channels ->
            if (currentMode == ViewMode.CHANNELS) {
                channelAdapter.submitList(channels)
            }
        }

        viewModel.searchResults.observe(this) { channels ->
            if (currentMode == ViewMode.CHANNELS) {
                channelAdapter.submitList(channels)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.toastMessage.observe(this) { msg ->
            msg?.let {
                if (it.startsWith("DOWNLOAD_CONTENT:")) {
                    val content = it.removePrefix("DOWNLOAD_CONTENT:")
                    savePlaylistToFile(content)
                } else {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearToast()
            }
        }
    }

    private fun showPlaylistsView() {
        currentMode = ViewMode.PLAYLISTS
        binding.recyclerView.adapter = playlistAdapter
        binding.tabLayout.visibility = View.GONE
        binding.urlInputCard.visibility = View.VISIBLE
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.navigationIcon = null

        viewModel.playlists.value?.let { playlists ->
            playlistAdapter.submitList(playlists)
            binding.emptyState.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (playlists.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun onPlaylistSelected(playlist: Playlist) {
        currentPlaylistId = playlist.id
        currentMode = ViewMode.CATEGORIES
        viewModel.selectPlaylist(playlist.id)

        binding.recyclerView.adapter = categoryAdapter
        binding.urlInputCard.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.toolbar.title = playlist.name
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupTabs()
    }

    private fun onCategorySelected(categoryInfo: CategoryInfo) {
        currentMode = ViewMode.CHANNELS
        viewModel.selectCategory(categoryInfo.name)

        binding.recyclerView.adapter = channelAdapter
        binding.toolbar.title = categoryInfo.name
        binding.tabLayout.visibility = View.GONE

        viewModel.filteredChannels.observe(this) { channels ->
            if (currentMode == ViewMode.CHANNELS) {
                channelAdapter.submitList(channels)
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.visibility = View.VISIBLE
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.all_channels))

        viewModel.categoryInfoList.observe(this) { categories ->
            if (currentMode == ViewMode.CATEGORIES) {
                val tabs = binding.tabLayout.tabCount
                if (tabs <= 1) {
                    categories.forEach { cat ->
                        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(cat.name))
                    }
                }
                categoryAdapter.submitList(categories)
            }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    currentMode = ViewMode.CATEGORIES
                    binding.recyclerView.adapter = categoryAdapter
                    viewModel.selectCategory(null)
                } else {
                    val categoryName = tab?.text?.toString()
                    if (categoryName != null) {
                        val info = viewModel.categoryInfoList.value?.find { it.name == categoryName }
                        if (info != null) {
                            onCategorySelected(info)
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showPlaylistMenu(playlist: Playlist, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.refresh)
        popup.menu.add(0, 2, 1, R.string.check_status)
        popup.menu.add(0, 3, 2, R.string.download_playlist)
        popup.menu.add(0, 4, 3, R.string.delete)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    viewModel.refreshPlaylist(playlist.id)
                    true
                }
                2 -> {
                    viewModel.checkAllChannels(playlist.id)
                    true
                }
                3 -> {
                    viewModel.downloadPlaylist(playlist.id)
                    true
                }
                4 -> {
                    showDeleteConfirmation(playlist)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(playlist: Playlist) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deletePlaylist(playlist)
                Toast.makeText(this, R.string.playlist_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showNameDialog(onConfirm: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_name, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.etPlaylistName)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.playlist_name)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = editText.text?.toString()?.trim()
                if (!name.isNullOrEmpty()) {
                    onConfirm(name)
                } else {
                    Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "audio/x-mpegurl",
                "audio/mpegurl",
                "application/x-mpegurl",
                "application/vnd.apple.mpegurl",
                "application/octet-stream",
                "*/*"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun importFiles(uris: List<Uri>) {
        var imported = 0
        for (uri in uris) {
            try {
                val fileName = getFileName(uri)
                val inputStream = contentResolver.openInputStream(uri) ?: continue
                val data = inputStream.readBytes()
                inputStream.close()

                val name = fileName.substringBeforeLast('.')
                if (fileName.endsWith(".m3u", true) || fileName.endsWith(".m3u8", true)) {
                    val content = data.toString(Charsets.UTF_8)
                    viewModel.addPlaylistFromContent(name, content, fileName)
                } else {
                    viewModel.addPlaylistFromBin(name, data, fileName)
                }
                imported++
            } catch (e: Exception) {
                Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        if (imported > 0) {
            Toast.makeText(this, getString(R.string.files_imported, imported), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "playlist"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, channel.name)
            putExtra(PlayerActivity.EXTRA_CHANNEL_URL, channel.url)
        }
        startActivity(intent)
    }

    private fun savePlaylistToFile(content: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "playlist_${System.currentTimeMillis()}.m3u")
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            Toast.makeText(this, getString(R.string.saved_to, file.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val file = File(filesDir, "playlist_${System.currentTimeMillis()}.m3u")
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                Toast.makeText(this, getString(R.string.saved_to, file.absolutePath), Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(this, "${getString(R.string.error)}: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        when (currentMode) {
            ViewMode.CHANNELS -> {
                currentMode = ViewMode.CATEGORIES
                binding.recyclerView.adapter = categoryAdapter
                binding.tabLayout.visibility = View.VISIBLE
                val playlist = viewModel.playlists.value?.find { it.id == currentPlaylistId }
                binding.toolbar.title = playlist?.name ?: getString(R.string.app_name)
                viewModel.selectCategory(null)
            }
            ViewMode.CATEGORIES -> {
                showPlaylistsView()
            }
            ViewMode.PLAYLISTS -> {
                super.onBackPressed()
            }
        }
    }
}
