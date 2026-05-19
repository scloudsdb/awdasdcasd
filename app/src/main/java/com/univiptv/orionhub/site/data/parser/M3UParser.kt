package com.univiptv.orionhub.site.data.parser

import com.univiptv.orionhub.site.data.model.Channel

object M3UParser {

    fun parse(content: String, playlistId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var i = 0

        var pendingUserAgent: String? = null
        var pendingReferrer: String? = null
        var pendingDrmType: String? = null
        var pendingDrmKeyId: String? = null
        var pendingDrmKey: String? = null
        var pendingDrmLicenseUrl: String? = null

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.isEmpty() || line.startsWith("#EXTM3U") || line.startsWith("<") || line.startsWith("=")) {
                i++
                continue
            }

            if (line.startsWith("#EXTVLCOPT:http-user-agent=")) {
                pendingUserAgent = line.substringAfter("#EXTVLCOPT:http-user-agent=").trim()
                    .removeSurrounding("\"")
                i++
                continue
            }

            if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                pendingReferrer = line.substringAfter("#EXTVLCOPT:http-referrer=").trim()
                    .removeSurrounding("\"")
                i++
                continue
            }

            if (line.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                pendingDrmType = line.substringAfter("#KODIPROP:inputstream.adaptive.license_type=").trim()
                i++
                continue
            }

            if (line.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                val keyValue = line.substringAfter("#KODIPROP:inputstream.adaptive.license_key=").trim()
                if (keyValue.startsWith("http")) {
                    pendingDrmLicenseUrl = keyValue
                } else if (keyValue.contains(":")) {
                    val parts = keyValue.split(":", limit = 2)
                    pendingDrmKeyId = parts[0].trim()
                    pendingDrmKey = parts[1].trim()
                }
                i++
                continue
            }

            if (line.startsWith("#EXTVLCOPT:") || line.startsWith("#KODIPROP:")) {
                i++
                continue
            }

            if (line.startsWith("#EXTINF:")) {
                val info = parseExtInf(line)
                val url = findNextUrl(lines, i + 1)
                if (url != null) {
                    // Check if lines between EXTINF and URL have more KODIPROP/EXTVLCOPT
                    val extraMeta = collectMetaBetween(lines, i + 1, url)

                    val finalUserAgent = extraMeta.userAgent ?: pendingUserAgent
                    val finalReferrer = extraMeta.referrer ?: pendingReferrer
                    val finalDrmType = extraMeta.drmType ?: pendingDrmType
                    val finalDrmKeyId = extraMeta.drmKeyId ?: pendingDrmKeyId
                    val finalDrmKey = extraMeta.drmKey ?: pendingDrmKey
                    val finalDrmLicenseUrl = extraMeta.drmLicenseUrl ?: pendingDrmLicenseUrl

                    channels.add(
                        Channel(
                            playlistId = playlistId,
                            name = info.name,
                            url = url,
                            logoUrl = info.logoUrl,
                            category = info.category,
                            language = info.language,
                            drmKeyId = finalDrmKeyId,
                            drmKey = finalDrmKey,
                            drmType = finalDrmType,
                            drmLicenseUrl = finalDrmLicenseUrl,
                            userAgent = finalUserAgent,
                            referrer = finalReferrer
                        )
                    )
                }
                pendingUserAgent = null
                pendingReferrer = null
                pendingDrmType = null
                pendingDrmKeyId = null
                pendingDrmKey = null
                pendingDrmLicenseUrl = null
            }
            i++
        }
        return channels
    }

    private data class ExtraMeta(
        val userAgent: String? = null,
        val referrer: String? = null,
        val drmType: String? = null,
        val drmKeyId: String? = null,
        val drmKey: String? = null,
        val drmLicenseUrl: String? = null
    )

    private fun collectMetaBetween(lines: List<String>, startIndex: Int, targetUrl: String): ExtraMeta {
        var userAgent: String? = null
        var referrer: String? = null
        var drmType: String? = null
        var drmKeyId: String? = null
        var drmKey: String? = null
        var drmLicenseUrl: String? = null

        var i = startIndex
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line == targetUrl) break
            if (line.startsWith("#EXTVLCOPT:http-user-agent=")) {
                userAgent = line.substringAfter("#EXTVLCOPT:http-user-agent=").trim().removeSurrounding("\"")
            } else if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                referrer = line.substringAfter("#EXTVLCOPT:http-referrer=").trim().removeSurrounding("\"")
            } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                drmType = line.substringAfter("#KODIPROP:inputstream.adaptive.license_type=").trim()
            } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                val keyValue = line.substringAfter("#KODIPROP:inputstream.adaptive.license_key=").trim()
                if (keyValue.startsWith("http")) {
                    drmLicenseUrl = keyValue
                } else if (keyValue.contains(":")) {
                    val parts = keyValue.split(":", limit = 2)
                    drmKeyId = parts[0].trim()
                    drmKey = parts[1].trim()
                }
            }
            i++
        }
        return ExtraMeta(userAgent, referrer, drmType, drmKeyId, drmKey, drmLicenseUrl)
    }

    private fun findNextUrl(lines: List<String>, startIndex: Int): String? {
        var i = startIndex
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("<")
                && !line.startsWith("=") && (line.startsWith("http") || line.startsWith("rtmp") || line.startsWith("rtsp"))) {
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
            // Return what we have
        }
        return channels
    }

    fun exportOnlineChannels(channels: List<Channel>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine()
        for (ch in channels.filter { it.isOnline }) {
            if (!ch.userAgent.isNullOrEmpty()) {
                sb.appendLine("#EXTVLCOPT:http-user-agent=${ch.userAgent}")
            }
            if (!ch.referrer.isNullOrEmpty()) {
                sb.appendLine("#EXTVLCOPT:http-referrer=${ch.referrer}")
            }
            if (!ch.drmType.isNullOrEmpty()) {
                sb.appendLine("#KODIPROP:inputstream.adaptive.license_type=${ch.drmType}")
            }
            if (!ch.drmKeyId.isNullOrEmpty() && !ch.drmKey.isNullOrEmpty()) {
                sb.appendLine("#KODIPROP:inputstream.adaptive.license_key=${ch.drmKeyId}:${ch.drmKey}")
            } else if (!ch.drmLicenseUrl.isNullOrEmpty()) {
                sb.appendLine("#KODIPROP:inputstream.adaptive.license_key=${ch.drmLicenseUrl}")
            }
            val logo = ch.logoUrl ?: ""
            sb.appendLine("#EXTINF:-1 tvg-logo=\"$logo\" group-title=\"${ch.category}\",${ch.name}")
            sb.appendLine(ch.url)
            sb.appendLine()
        }
        return sb.toString()
    }
}
