package com.shadownightdev.universaliptv.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.shadownightdev.universaliptv.R
import com.shadownightdev.universaliptv.databinding.ActivitySettingsBinding
import com.shadownightdev.universaliptv.ui.main.MainActivity
import com.shadownightdev.universaliptv.ui.main.MainViewModel
import com.shadownightdev.universaliptv.util.LocaleHelper
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: MainViewModel

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
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupToolbar()
        setupLanguage()
        setupPlaylistManagement()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupLanguage() {
        val currentLang = LocaleHelper.getLanguage(this)
        when (currentLang) {
            "en" -> binding.radioEnglish.isChecked = true
            "id" -> binding.radioIndonesian.isChecked = true
        }

        binding.languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLang = when (checkedId) {
                R.id.radioEnglish -> "en"
                R.id.radioIndonesian -> "id"
                else -> "en"
            }
            if (newLang != currentLang) {
                LocaleHelper.setLocale(this, newLang)
                Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show()
                restartApp()
            }
        }
    }

    private fun setupPlaylistManagement() {
        binding.btnSettingsLoadUrl.setOnClickListener {
            val url = binding.settingsUrlInput.text?.toString()?.trim()
            if (url.isNullOrEmpty()) {
                Toast.makeText(this, R.string.please_enter_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addPlaylistFromUrl("Playlist", url)
            binding.settingsUrlInput.text?.clear()
            Toast.makeText(this, R.string.playlist_added, Toast.LENGTH_SHORT).show()
        }

        binding.btnSettingsImport.setOnClickListener {
            openFilePicker()
        }

        binding.btnDownloadPlaylist.setOnClickListener {
            val playlists = viewModel.playlists.value
            if (playlists.isNullOrEmpty()) {
                Toast.makeText(this, R.string.no_playlists, Toast.LENGTH_SHORT).show()
            } else {
                viewModel.downloadPlaylist(playlists.first().id)
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

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
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

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
