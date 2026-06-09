package com.github.wanniwa.editorjumper.config

import java.io.File

object CachePaths {
    fun getCacheRootDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = when {
            os.contains("mac") -> File(home, "Library/Caches/EzEditorJumper")
            os.contains("win") -> {
                val local = System.getenv("LOCALAPPDATA") ?: File(home, "AppData/Local").absolutePath
                File(local, "EzEditorJumper/cache")
            }
            else -> {
                val xdg = System.getenv("XDG_CACHE_HOME")
                val base = if (!xdg.isNullOrBlank()) File(xdg) else File(home, ".cache")
                File(base, "EzEditorJumper")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getSharedAppsFile(): File = File(getCacheRootDir(), "shared-apps.json")
}
