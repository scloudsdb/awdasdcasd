package com.univiptv.orionhub.site.ui.settings

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
import com.univiptv.orionhub.site.R
import com.univiptv.orionhub.site.databinding.ActivitySettingsBinding
import com.univiptv.orionhub.site.ui.main.MainActivity
import com.univiptv.orionhub.site.ui.main.MainViewModel
import com.univiptv.orionhub.site.util.LocaleHelper
import com.univiptv.orionhub.site.util.ThemeHelper
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: MainViewModel

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
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupLanguage()
        setupTheme()
        setupPlaylistManagement()
    }

    private fun setupLanguage() {
        val currentLang = LocaleHelper.getLanguage(this)
        when (currentLang) {
            "en" -> binding.radioEnglish.isChecked = true
            "id" -> binding.radioIndonesian.isChecked = true
        }
        binding.languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLang = if (checkedId == R.id.radioEnglish) "en" else "id"
            if (newLang != currentLang) {
                LocaleHelper.setLocale(this, newLang)
                Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show()
                restartApp()
            }
        }
    }

    private fun setupTheme() {
        val currentTheme = ThemeHelper.getTheme(this)
        when (currentTheme) {
            ThemeHelper.THEME_LIGHT -> binding.radioThemeLight.isChecked = true
            ThemeHelper.THEME_DARK -> binding.radioThemeDark.isChecked = true
            ThemeHelper.THEME_SYSTEM -> binding.radioThemeSystem.isChecked = true
        }
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.radioThemeLight -> ThemeHelper.THEME_LIGHT
                R.id.radioThemeDark -> ThemeHelper.THEME_DARK
                R.id.radioThemeSystem -> ThemeHelper.THEME_SYSTEM
                else -> ThemeHelper.THEME_LIGHT
            }
            if (newTheme != currentTheme) {
                ThemeHelper.setTheme(this, newTheme)
                Toast.makeText(this, R.string.theme_changed, Toast.LENGTH_SHORT).show()
                restartApp()
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(intent)
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
                    savePlaylistToFile(it.removePrefix("DOWNLOAD_CONTENT:"))
                } else {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearToast()
            }
        }
        viewModel.error.observe(this) { err ->
            err?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
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
}
