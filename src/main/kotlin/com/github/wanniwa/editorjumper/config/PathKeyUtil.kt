package com.github.wanniwa.editorjumper.config

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object PathKeyUtil {
    fun normalizeAnchorPath(anchorPath: String?): String {
        if (anchorPath.isNullOrBlank()) return ""
        var resolved = File(anchorPath.trim()).absoluteFile.path.replace('\\', '/')
        if (resolved.length >= 2 && resolved[1] == ':') {
            resolved = resolved[0].lowercaseChar() + resolved.substring(1)
        }
        return resolved
    }

    fun computeConfigKey(anchorPath: String?): String {
        val normalized = normalizeAnchorPath(anchorPath)
        if (normalized.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(normalized.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }

    fun buildProjectCacheFileName(configKey: String, anchorPath: String, pluginSuffix: String): String {
        val base = File(anchorPath).name.ifBlank { "project" }
        val safeBase = base.replace(Regex("[^a-zA-Z0-9._-]+"), "_")
        return "${configKey}_${safeBase}_$pluginSuffix.json"
    }
}
