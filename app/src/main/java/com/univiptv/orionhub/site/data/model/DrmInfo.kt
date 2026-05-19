package com.univiptv.orionhub.site.data.model

data class DrmInfo(
    val mpdUrl: String,
    val imageUrl: String? = null,
    val keyId: String,
    val key: String,
    val title: String = "DRM Stream"
)
