package com.univiptv.orionhub.site.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object StreamChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun isStreamOnline(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).head().build()
            val response = client.newCall(request).execute()
            response.close()
            response.isSuccessful || response.code in 200..399
        } catch (e: Exception) {
            false
        }
    }
}
