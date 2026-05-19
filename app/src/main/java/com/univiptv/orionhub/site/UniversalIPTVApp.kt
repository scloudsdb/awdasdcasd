package com.univiptv.orionhub.site

import android.app.Application
import android.content.Context
import com.univiptv.orionhub.site.util.LocaleHelper
import com.univiptv.orionhub.site.util.ThemeHelper

class UniversalIPTVApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeHelper.applyTheme(this)
    }

    companion object {
        lateinit var instance: UniversalIPTVApp
            private set
    }
}
