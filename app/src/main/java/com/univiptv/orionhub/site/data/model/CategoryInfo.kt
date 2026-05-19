package com.univiptv.orionhub.site.data.model

data class CategoryInfo(
    val name: String,
    val totalChannels: Int,
    val onlineChannels: Int,
    val offlineChannels: Int,
    val channels: List<Channel> = emptyList()
)
