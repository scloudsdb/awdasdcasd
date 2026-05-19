package com.shadownightdev.universaliptv.data.parser

import com.shadownightdev.universaliptv.data.model.Channel

object M3UParser {

    fun parse(content: String, playlistId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val info = parseExtInf(line)
                val url = findNextUrl(lines, i + 1)
                if (url != null) {
                    channels.add(
                        Channel(
                            playlistId = playlistId,
                            name = info.name,
                            url = url,
                            logoUrl = info.logoUrl,
                            category = info.category,
                            language = info.language
                        )
                    )
                }
            }
            i++
        }
        return channels
    }

    private fun findNextUrl(lines: List<String>, startIndex: Int): String? {
        var i = startIndex
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                return line
            }
            if (line.startsWith("#EXTINF:")) return null
            i++
        }
        return null
    }

    private fun parseExtInf(line: String): ExtInfInfo {
        val name = extractDisplayName(line)
        val logoUrl = extractAttribute(line, "tvg-logo")
        val category = extractAttribute(line, "group-title") ?: "Uncategorized"
        val language = extractAttribute(line, "tvg-language")
        return ExtInfInfo(name, logoUrl, category, language)
    }

    private fun extractDisplayName(line: String): String {
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex >= 0 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }
    }

    private fun extractAttribute(line: String, attr: String): String? {
        val pattern = Regex("""$attr="([^"]*?)"""")
        return pattern.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    private data class ExtInfInfo(
        val name: String,
        val logoUrl: String?,
        val category: String,
        val language: String?
    )

    fun parseBinFile(data: ByteArray, playlistId: Long): List<Channel> {
        return try {
            val content = data.toString(Charsets.UTF_8)
            if (content.contains("#EXTINF") || content.contains("#EXTM3U")) {
                parse(content, playlistId)
            } else {
                parseBinaryFormat(data, playlistId)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBinaryFormat(data: ByteArray, playlistId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val content = data.toString(Charsets.UTF_8)
            val lines = content.lines().filter { it.isNotBlank() }
            for (line in lines) {
                val parts = line.split("|", ",", "\t")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val url = parts.lastOrNull { it.trim().startsWith("http") }?.trim()
                    if (url != null) {
                        val category = if (parts.size >= 3) parts[1].trim() else "Uncategorized"
                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                name = name,
                                url = url,
                                category = category
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Return whatever we've parsed so far
        }
        return channels
    }
}
