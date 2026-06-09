package com.github.wanniwa.editorjumper.settings

import com.github.wanniwa.editorjumper.config.GlobalConfigStore
import com.github.wanniwa.editorjumper.config.LegacyConfigMigration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.wanniwa.editorjumper.settings.EditorJumperSettings",
    storages = [Storage("EditorJumperSettings.xml")]
)
class EditorJumperSettings : PersistentStateComponent<EditorJumperSettings> {

    /** Map of editor name -> custom executable path. */
    var editorPaths: MutableMap<String, String> = HashMap()

    /**
     * Editors that are hidden from the status bar menu.
     * If empty, all known editors are shown (backwards compatible default).
     */
    var hiddenEditors: MutableSet<String> = LinkedHashSet()

    var selectedEditorType: String = "Cursor"
    var hasShownStatusBarGuide: Boolean = false

    // Shortcut editor slots for global shortcuts
    var shortcutSlot1: String = "Cursor"  // Alt+Shift+O
    var shortcutSlot2: String = "Visual Studio Code"  // Alt+Shift+I
    var shortcutSlot3: String = "Windsurf"  // Alt+Shift+U

    // ---------------------------------------------------------------------------
    // Legacy fields kept solely for one-time migration of existing user settings.
    // ---------------------------------------------------------------------------
    var vscodePath: String = ""
    var cursorPath: String = ""
    var traePath: String = ""
    var windsurfPath: String = ""
    var voidPath: String = ""
    var kiroPath: String = ""
    var qoderPath: String = ""
    var catPawAIPath: String = ""
    var antigravityPath: String = ""
    var traeCN: Boolean = false

    fun getPath(editorName: String): String {
        return runCatching { globalStore().getPath(editorName) }
            .getOrElse { editorPaths[editorName] ?: "" }
    }

    fun setPath(editorName: String, path: String) {
        editorPaths[editorName] = path
        runCatching { globalStore().setPath(editorName, path) }
    }

    fun syncPathsToGlobalStore() {
        editorPaths.forEach { (name, path) ->
            runCatching { globalStore().setPath(name, path) }
        }
        if (hiddenEditors.isNotEmpty()) {
            runCatching { globalStore().setHiddenEditors(hiddenEditors) }
        }
        runCatching {
            globalStore().saveJumperExtras(shortcutSlot1, shortcutSlot2, shortcutSlot3, selectedEditorType)
        }
    }

    fun applyLegacyFieldMigration() {
        migrateLegacyFields()
    }

    fun hasLegacyPathFields(): Boolean =
        vscodePath.isNotEmpty() ||
            cursorPath.isNotEmpty() ||
            traePath.isNotEmpty() ||
            windsurfPath.isNotEmpty() ||
            voidPath.isNotEmpty() ||
            kiroPath.isNotEmpty() ||
            qoderPath.isNotEmpty() ||
            catPawAIPath.isNotEmpty() ||
            antigravityPath.isNotEmpty()

    private fun hydrateFromGlobalStore() {
        runCatching {
            val extras = globalStore().getJumperExtras()
            selectedEditorType = extras.selectedEditorType
            shortcutSlot1 = extras.shortcutSlot1
            shortcutSlot2 = extras.shortcutSlot2
            shortcutSlot3 = extras.shortcutSlot3
        }
    }

    private fun globalStore(): GlobalConfigStore = GlobalConfigStore.getInstance()

    companion object {
        fun getInstance(): EditorJumperSettings =
            ApplicationManager.getApplication().getService(EditorJumperSettings::class.java)
    }

    override fun getState(): EditorJumperSettings =
        EditorJumperSettings().apply { hasShownStatusBarGuide = this@EditorJumperSettings.hasShownStatusBarGuide }

    override fun loadState(state: EditorJumperSettings) {
        hasShownStatusBarGuide = state.hasShownStatusBarGuide
        val incoming = EditorJumperSettings()
        XmlSerializerUtil.copyBean(state, incoming)
        runCatching {
            if (LegacyConfigMigration.hasLegacyGlobalPayload(incoming)) {
                LegacyConfigMigration.importLegacyGlobalSettings(incoming)
            }
            globalStore().readApps(forceReload = true)
        }
        hydrateFromGlobalStore()
    }

    private fun migrateLegacyFields() {
        if (editorPaths.isEmpty()) {
            mapOf(
                "Visual Studio Code" to vscodePath,
                "Cursor" to cursorPath,
                (if (traeCN) "Trae CN" else "Trae") to traePath,
                "Windsurf" to windsurfPath,
                "Void" to voidPath,
                "Kiro" to kiroPath,
                "Qoder" to qoderPath,
                "CatPawAI" to catPawAIPath,
                "Antigravity" to antigravityPath,
            ).filter { it.value.isNotEmpty() }
                .forEach { (k, v) -> editorPaths[k] = v }
        }

        // 兼容历史数据：之前用 Trae + traeCN，现在显式区分为两种编辑器
        if (traeCN && selectedEditorType == "Trae") {
            selectedEditorType = "Trae CN"
        }

    }
}
