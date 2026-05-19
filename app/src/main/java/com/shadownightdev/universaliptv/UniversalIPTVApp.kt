package com.shadownightdev.universaliptv

import android.app.Application
import android.content.Context
import com.shadownightdev.universaliptv.util.LocaleHelper

class UniversalIPTVApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: UniversalIPTVApp
            private set
    }
}
