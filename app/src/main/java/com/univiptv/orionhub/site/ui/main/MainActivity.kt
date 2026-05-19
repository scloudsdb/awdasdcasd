package com.univiptv.orionhub.site.ui.main

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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.univiptv.orionhub.site.R
import com.univiptv.orionhub.site.data.model.CategoryInfo
import com.univiptv.orionhub.site.data.model.Channel
import com.univiptv.orionhub.site.data.model.Playlist
import com.univiptv.orionhub.site.databinding.ActivityMainBinding
import com.univiptv.orionhub.site.ui.player.PlayerActivity
import com.univiptv.orionhub.site.ui.settings.SettingsActivity
import com.univiptv.orionhub.site.util.LocaleHelper
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private val playlistAdapter = PlaylistAdapter(
        onPlaylistClick = { onPlaylistSelected(it) },
        onMenuClick = { playlist, view -> showPlaylistMenu(playlist, view) }
    )
    private val categoryAdapter = CategoryAdapter { onCategorySelected(it) }
    private val channelAdapter = ChannelAdapter { openPlayer(it) }

    private enum class ViewMode { PLAYLISTS, CATEGORIES, CHANNELS }
    private var currentMode = ViewMode.PLAYLISTS
    private var currentPlaylistId: Long = -1

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } ?: result.data?.data?.let { uris.add(it) }
            if (uris.isNotEmpty()) importFiles(uris)
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
        setupDrmInput()
        setupSwipeRefresh()
        observeViewModel()
        showPlaylistsView()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_search -> {
                    (item.actionView as? SearchView)?.setOnQueryTextListener(
                        object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?) = false
                            override fun onQueryTextChange(newText: String?): Boolean {
                                if (currentMode == ViewMode.CHANNELS) viewModel.search(newText)
                                return true
                            }
                        }
                    )
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
        binding.btnImportFile.setOnClickListener { openFilePicker() }
    }

    private fun setupDrmInput() {
        binding.btnPlayDrm.setOnClickListener {
            val mpdUrl = binding.etMpdUrl.text?.toString()?.trim()
            val imageUrl = binding.etImageUrl.text?.toString()?.trim()
            val kidKey = binding.etKidKey.text?.toString()?.trim()

            if (mpdUrl.isNullOrEmpty()) {
                Toast.makeText(this, R.string.please_enter_mpd, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (kidKey.isNullOrEmpty() || !kidKey.contains(":")) {
                Toast.makeText(this, R.string.please_enter_kid_key, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parts = kidKey.split(":", limit = 2)
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, "DRM Stream")
                putExtra(PlayerActivity.EXTRA_CHANNEL_URL, mpdUrl)
                putExtra(PlayerActivity.EXTRA_DRM_KEY_ID, parts[0].trim())
                putExtra(PlayerActivity.EXTRA_DRM_KEY, parts[1].trim())
                putExtra(PlayerActivity.EXTRA_IMAGE_URL, imageUrl)
                putExtra(PlayerActivity.EXTRA_IS_DRM, true)
            }
            startActivity(intent)
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
        viewModel.categoryInfoList.observe(this) { cats ->
            if (currentMode == ViewMode.CATEGORIES) {
                categoryAdapter.submitList(cats)
                binding.emptyState.visibility = if (cats.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (cats.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        viewModel.filteredChannels.observe(this) { channels ->
            if (currentMode == ViewMode.CHANNELS) channelAdapter.submitList(channels)
        }
        viewModel.searchResults.observe(this) { channels ->
            if (currentMode == ViewMode.CHANNELS) channelAdapter.submitList(channels)
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { err ->
            err?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        viewModel.toastMessage.observe(this) { msg ->
            msg?.let {
                if (it.startsWith("DOWNLOAD_CONTENT:")) {
                    savePlaylistToFile(it.removePrefix("DOWNLOAD_CONTENT:"))
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
        binding.inputSection.visibility = View.VISIBLE
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.navigationIcon = null
        viewModel.playlists.value?.let { pl ->
            playlistAdapter.submitList(pl)
            binding.emptyState.visibility = if (pl.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (pl.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun onPlaylistSelected(playlist: Playlist) {
        currentPlaylistId = playlist.id
        currentMode = ViewMode.CATEGORIES
        viewModel.selectPlaylist(playlist.id)
        binding.recyclerView.adapter = categoryAdapter
        binding.inputSection.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.toolbar.title = playlist.name
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupTabs()
    }

    private fun onCategorySelected(info: CategoryInfo) {
        currentMode = ViewMode.CHANNELS
        viewModel.selectCategory(info.name)
        binding.recyclerView.adapter = channelAdapter
        binding.toolbar.title = info.name
        binding.tabLayout.visibility = View.GONE
    }

    private fun setupTabs() {
        binding.tabLayout.visibility = View.VISIBLE
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.all_channels))
        viewModel.categoryInfoList.observe(this) { cats ->
            if (currentMode == ViewMode.CATEGORIES) {
                if (binding.tabLayout.tabCount <= 1) {
                    cats.forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it.name)) }
                }
                categoryAdapter.submitList(cats)
            }
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    currentMode = ViewMode.CATEGORIES
                    binding.recyclerView.adapter = categoryAdapter
                    viewModel.selectCategory(null)
                } else {
                    val name = tab?.text?.toString()
                    val info = viewModel.categoryInfoList.value?.find { it.name == name }
                    if (info != null) onCategorySelected(info)
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
                1 -> { viewModel.refreshPlaylist(playlist.id); true }
                2 -> { viewModel.checkAllChannels(playlist.id); true }
                3 -> { viewModel.downloadPlaylist(playlist.id); true }
                4 -> { showDeleteConfirmation(playlist); true }
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
                if (!name.isNullOrEmpty()) onConfirm(name)
                else Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun importFiles(uris: List<Uri>) {
        var imported = 0
        for (uri in uris) {
            try {
                val fileName = getFileName(uri)
                val data = contentResolver.openInputStream(uri)?.readBytes() ?: continue
                val name = fileName.substringBeforeLast('.')
                if (fileName.endsWith(".m3u", true) || fileName.endsWith(".m3u8", true)) {
                    viewModel.addPlaylistFromContent(name, data.toString(Charsets.UTF_8), fileName)
                } else {
                    viewModel.addPlaylistFromBin(name, data, fileName)
                }
                imported++
            } catch (e: Exception) {
                Toast.makeText(this, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        if (imported > 0) Toast.makeText(this, getString(R.string.files_imported, imported), Toast.LENGTH_SHORT).show()
    }

    private fun getFileName(uri: Uri): String {
        var name = "playlist"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, channel.name)
            putExtra(PlayerActivity.EXTRA_CHANNEL_URL, channel.url)
            putExtra(PlayerActivity.EXTRA_IMAGE_URL, channel.imageUrl ?: channel.logoUrl)
            if (!channel.drmKeyId.isNullOrEmpty() && !channel.drmKey.isNullOrEmpty()) {
                putExtra(PlayerActivity.EXTRA_IS_DRM, true)
                putExtra(PlayerActivity.EXTRA_DRM_KEY_ID, channel.drmKeyId)
                putExtra(PlayerActivity.EXTRA_DRM_KEY, channel.drmKey)
            }
        }
        startActivity(intent)
    }

    private fun savePlaylistToFile(content: String) {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "playlist_${System.currentTimeMillis()}.m3u")
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
                binding.toolbar.title = viewModel.playlists.value?.find { it.id == currentPlaylistId }?.name ?: getString(R.string.app_name)
                viewModel.selectCategory(null)
            }
            ViewMode.CATEGORIES -> showPlaylistsView()
            ViewMode.PLAYLISTS -> super.onBackPressed()
        }
    }
}
