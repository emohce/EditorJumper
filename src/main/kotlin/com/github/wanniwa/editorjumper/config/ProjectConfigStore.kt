package com.github.wanniwa.editorjumper.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
class ProjectConfigStore(private val project: Project) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val pluginSuffix = "jumper"

    fun resolveAnchorPath(legacyWorkspacePath: String = ""): String? {
        val ws = legacyWorkspacePath.trim()
        if (ws.isNotEmpty() && File(ws).exists()) {
            return ws
        }
        return project.basePath
    }

    fun readProject(): JumperProjectConfig {
        val anchor = resolveAnchorPath() ?: return migrateFromLegacy()
        val file = getCacheFile(anchor) ?: return migrateFromLegacy()
        val data = readJsonSafe(file)
        if (data != null) return data
        val migrated = migrateFromLegacy()
        migrated.anchorPath = PathKeyUtil.normalizeAnchorPath(anchor)
        writeProject(migrated)
        return migrated
    }

    fun writeProject(config: JumperProjectConfig) {
        val anchor = config.anchorPath.ifBlank { PathKeyUtil.normalizeAnchorPath(resolveAnchorPath(config.vsCodeWorkspacePath) ?: return) }
        val file = getCacheFile(anchor) ?: return
        val next = config.copy(anchorPath = PathKeyUtil.normalizeAnchorPath(anchor))
        atomicWrite(file, next)
    }

    fun getVsCodeWorkspacePath(): String = readProject().vsCodeWorkspacePath

    fun setVsCodeWorkspacePath(path: String) {
        val current = readProject()
        current.vsCodeWorkspacePath = path
        writeProject(current)
    }

    fun getProjectEditorType(): String = readProject().projectEditorType

    fun setProjectEditorType(editorType: String) {
        val current = readProject()
        current.projectEditorType = editorType
        writeProject(current)
    }

    fun setShortcutSlots(slot1: String, slot2: String, slot3: String, customized: Boolean) {
        val current = readProject()
        current.shortcutSlot1 = slot1
        current.shortcutSlot2 = slot2
        current.shortcutSlot3 = slot3
        current.slotsCustomized = customized
        writeProject(current)
    }

    private fun getCacheFile(anchorPath: String): File? {
        val normalized = PathKeyUtil.normalizeAnchorPath(anchorPath)
        val key = PathKeyUtil.computeConfigKey(normalized)
        if (key.isEmpty()) return null
        val name = PathKeyUtil.buildProjectCacheFileName(key, normalized, pluginSuffix)
        return File(CachePaths.getCacheRootDir(), name)
    }

    private fun migrateFromLegacy(): JumperProjectConfig {
        val anchor = resolveAnchorPath()
        return JumperProjectConfig(
            anchorPath = anchor?.let { PathKeyUtil.normalizeAnchorPath(it) } ?: "",
        )
    }

    private fun readJsonSafe(file: File): JumperProjectConfig? {
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(Charsets.UTF_8), JumperProjectConfig::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun atomicWrite(file: File, data: JumperProjectConfig) {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(gson.toJson(data), Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }

    companion object {
        fun getInstance(project: Project): ProjectConfigStore = project.getService(ProjectConfigStore::class.java)
    }
}
