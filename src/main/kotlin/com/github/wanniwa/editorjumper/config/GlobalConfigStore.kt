package com.github.wanniwa.editorjumper.config

import com.github.wanniwa.editorjumper.editors.EditorRegistry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileEvent
import java.io.File
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.APP)
class GlobalConfigStore : Disposable {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var cachedApps: SharedAppsConfig? = null
    private var cachedMtime: Long = 0L
    private val listeners = CopyOnWriteArrayList<Runnable>()
    private var vfsListener: VirtualFileListener? = null

    fun readApps(forceReload: Boolean = false): SharedAppsConfig {
        val file = CachePaths.getSharedAppsFile()
        if (!forceReload && cachedApps != null && file.exists() && file.lastModified() == cachedMtime) {
            return cachedApps!!
        }
        val data = readJsonSafe(file) ?: migrateFromLegacySettings()
        cachedApps = data
        cachedMtime = if (file.exists()) file.lastModified() else 0L
        return data
    }

    fun writeApps(patch: SharedAppsConfig): SharedAppsConfig {
        val current = readApps(forceReload = true)
        val merged = mergeApps(current, patch)
        merged.revision = (current.revision) + 1
        atomicWrite(CachePaths.getSharedAppsFile(), merged)
        cachedApps = merged
        cachedMtime = CachePaths.getSharedAppsFile().lastModified()
        return merged
    }

    fun getPath(editorName: String): String {
        val apps = readApps()
        return apps.vscodeApps.find { it.name == editorName }?.commandPath?.takeIf { it.isNotBlank() } ?: ""
    }

    fun setPath(editorName: String, path: String) {
        val apps = readApps(forceReload = true)
        val list = apps.vscodeApps.toMutableList()
        val idx = list.indexOfFirst { it.name == editorName }
        val entry = if (idx >= 0) list[idx].copy() else AppEntry(name = editorName)
        entry.commandPath = path.ifBlank { null }
        entry.updatedAt = Instant.now().toString()
        if (idx >= 0) list[idx] = entry else list.add(entry)
        writeApps(SharedAppsConfig(vscodeApps = list))
    }

    fun getHiddenEditors(): Set<String> {
        val apps = readApps()
        return apps.vscodeApps.filter { it.hidden }.map { it.name }.toCollection(LinkedHashSet())
    }

    fun addCustomVscodeApp(name: String, commandPath: String = ""): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val apps = readApps(forceReload = true)
        if (apps.vscodeApps.any { it.name == trimmed }) return false
        val list = apps.vscodeApps.toMutableList()
        list.add(
            AppEntry(
                name = trimmed,
                commandPath = commandPath.ifBlank { null },
                isCustom = true,
                hidden = false,
                updatedAt = Instant.now().toString(),
            ),
        )
        writeApps(SharedAppsConfig(vscodeApps = list))
        return true
    }

    fun removeCustomVscodeApp(name: String): Boolean {
        val apps = readApps(forceReload = true)
        if (apps.vscodeApps.none { it.name == name && it.isCustom }) return false
        val list = apps.vscodeApps.filterNot { it.name == name && it.isCustom }.toMutableList()
        writeApps(SharedAppsConfig(vscodeApps = list))
        return true
    }

    fun setHiddenEditors(hidden: Set<String>) {
        val apps = readApps(forceReload = true)
        val allNames = SharedEditorCatalog.allAppNames()
        val list = apps.vscodeApps.toMutableList()
        val known = list.map { it.name }.toSet()
        val customNames = SharedEditorCatalog.customApps().map { it.name }.toSet()
        allNames.forEach { name ->
            if (!known.contains(name)) {
                list.add(AppEntry(name = name, isCustom = name in customNames))
            }
        }
        val hiddenSet = hidden.toSet()
        list.forEachIndexed { index, entry ->
            list[index] = entry.copy(hidden = hiddenSet.contains(entry.name), updatedAt = Instant.now().toString())
        }
        writeApps(SharedAppsConfig(vscodeApps = list))
    }

    fun getJumperExtras(): JumperExtras = readApps().jumperExtras

    fun saveJumperExtras(
        shortcutSlot1: String,
        shortcutSlot2: String,
        shortcutSlot3: String,
        selectedEditorType: String,
    ) {
        writeApps(
            SharedAppsConfig(
                jumperExtras = JumperExtras(
                    shortcutSlot1 = shortcutSlot1,
                    shortcutSlot2 = shortcutSlot2,
                    shortcutSlot3 = shortcutSlot3,
                    selectedEditorType = selectedEditorType,
                ),
            ),
        )
    }

    fun getJetbrainsApps(): List<AppEntry> = readApps().jetbrainsApps

    fun getVscodeApps(): List<AppEntry> = readApps().vscodeApps

    fun addChangeListener(listener: Runnable) {
        listeners.add(listener)
        ApplicationManager.getApplication().invokeLater { ensureWatcher() }
    }

    fun removeChangeListener(listener: Runnable) {
        listeners.remove(listener)
    }

    private fun notifyListenersOnEdt() {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            listeners.forEach { it.run() }
        } else {
            app.invokeLater { listeners.forEach { it.run() } }
        }
    }

    private fun ensureWatcher() {
        if (vfsListener != null) return
        val file = CachePaths.getSharedAppsFile()
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        if (!file.exists()) readApps(forceReload = true)
        if (LocalFileSystem.getInstance().findFileByIoFile(file) == null) return
        vfsListener = object : VirtualFileListener {
            override fun contentsChanged(event: VirtualFileEvent) {
                if (event.file.path == file.absolutePath) {
                    cachedApps = null
                    notifyListenersOnEdt()
                }
            }
        }
        LocalFileSystem.getInstance().addVirtualFileListener(vfsListener!!)
    }

    private fun readJsonSafe(file: File): SharedAppsConfig? {
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(Charsets.UTF_8), SharedAppsConfig::class.java)
        } catch (_: Exception) {
            val bak = File(file.absolutePath + ".bak")
            runCatching { file.copyTo(bak, overwrite = true) }
            null
        }
    }

    private fun atomicWrite(file: File, data: SharedAppsConfig) {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(gson.toJson(data), Charsets.UTF_8)
        if (file.exists()) file.delete()
        tmp.renameTo(file)
    }

    private fun mergeAppEntry(existing: AppEntry?, incoming: AppEntry): AppEntry {
        if (existing == null) return incoming.copy(updatedAt = incoming.updatedAt ?: Instant.now().toString())
        val merged = existing.copy(
            commandPath = incoming.commandPath ?: existing.commandPath,
            hidden = incoming.hidden,
            isCustom = existing.isCustom || incoming.isCustom,
        )
        if (!existing.commandPath.isNullOrBlank() && !incoming.commandPath.isNullOrBlank() &&
            existing.commandPath != incoming.commandPath
        ) {
            val exTime = existing.updatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0L) } ?: 0L
            val inTime = incoming.updatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0L) } ?: 0L
            merged.commandPath = if (inTime >= exTime) incoming.commandPath else existing.commandPath
        }
        merged.updatedAt = Instant.now().toString()
        return merged
    }

    private fun mergeAppArrays(existing: List<AppEntry>, incoming: List<AppEntry>): MutableList<AppEntry> {
        val map = LinkedHashMap<String, AppEntry>()
        existing.forEach { map[it.name] = it }
        incoming.forEach { map[it.name] = mergeAppEntry(map[it.name], it) }
        return map.values.toMutableList()
    }

    private fun mergeApps(base: SharedAppsConfig, patch: SharedAppsConfig): SharedAppsConfig {
        val extras = if (patch.jumperExtras.shortcutSlot1.isNotBlank() ||
            patch.jumperExtras.selectedEditorType.isNotBlank()
        ) {
            base.jumperExtras.copy(
                shortcutSlot1 = patch.jumperExtras.shortcutSlot1.ifBlank { base.jumperExtras.shortcutSlot1 },
                shortcutSlot2 = patch.jumperExtras.shortcutSlot2.ifBlank { base.jumperExtras.shortcutSlot2 },
                shortcutSlot3 = patch.jumperExtras.shortcutSlot3.ifBlank { base.jumperExtras.shortcutSlot3 },
                selectedEditorType = patch.jumperExtras.selectedEditorType.ifBlank { base.jumperExtras.selectedEditorType },
            )
        } else {
            base.jumperExtras
        }
        return SharedAppsConfig(
            version = 1,
            revision = maxOf(base.revision, patch.revision),
            jetbrainsApps = if (patch.jetbrainsApps.isEmpty()) base.jetbrainsApps else mergeAppArrays(base.jetbrainsApps, patch.jetbrainsApps),
            vscodeApps = if (patch.vscodeApps.isEmpty()) base.vscodeApps else mergeAppArrays(base.vscodeApps, patch.vscodeApps),
            jumperExtras = extras,
        )
    }

    private fun migrateFromLegacySettings(): SharedAppsConfig {
        val defaults = createDefaultsFromRegistry()
        atomicWrite(CachePaths.getSharedAppsFile(), defaults)
        return defaults
    }

    private fun createDefaultsFromRegistry(): SharedAppsConfig {
        val vscodeApps = EditorRegistry.editors.map {
            AppEntry(name = it.name, commandPath = null, isCustom = false, hidden = false)
        }.toMutableList()
        return SharedAppsConfig(
            jetbrainsApps = mutableListOf(),
            vscodeApps = vscodeApps,
            jumperExtras = JumperExtras(),
        )
    }

    companion object {
        fun getInstance(): GlobalConfigStore =
            ApplicationManager.getApplication().getService(GlobalConfigStore::class.java)
    }

    override fun dispose() {
        vfsListener?.let { LocalFileSystem.getInstance().removeVirtualFileListener(it) }
        vfsListener = null
        listeners.clear()
    }
}
