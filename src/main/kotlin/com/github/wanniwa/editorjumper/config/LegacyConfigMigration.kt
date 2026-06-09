package com.github.wanniwa.editorjumper.config

import com.github.wanniwa.editorjumper.settings.EditorJumperSettings
import com.intellij.openapi.project.Project
import java.time.Instant

object LegacyConfigMigration {
    fun hasLegacyGlobalPayload(settings: EditorJumperSettings): Boolean {
        settings.applyLegacyFieldMigration()
        return settings.editorPaths.isNotEmpty() ||
            settings.hiddenEditors.isNotEmpty() ||
            settings.hasLegacyPathFields() ||
            settings.shortcutSlot1 != "Cursor" ||
            settings.shortcutSlot2 != "Visual Studio Code" ||
            settings.shortcutSlot3 != "Windsurf" ||
            settings.selectedEditorType != "Cursor"
    }

    fun importLegacyGlobalSettings(settings: EditorJumperSettings): Boolean {
        settings.applyLegacyFieldMigration()
        if (!hasLegacyGlobalPayload(settings)) {
            return false
        }
        val store = GlobalConfigStore.getInstance()
        store.readApps(forceReload = true)
        val vscodeApps = settings.editorPaths.map { (name, path) ->
            AppEntry(
                name = name,
                commandPath = path.ifBlank { null },
                isCustom = false,
                hidden = settings.hiddenEditors.contains(name),
                updatedAt = Instant.now().toString(),
            )
        }
        val patch = SharedAppsConfig(
            vscodeApps = vscodeApps.toMutableList(),
            jumperExtras = JumperExtras(
                shortcutSlot1 = settings.shortcutSlot1,
                shortcutSlot2 = settings.shortcutSlot2,
                shortcutSlot3 = settings.shortcutSlot3,
                selectedEditorType = settings.selectedEditorType,
            ),
        )
        if (settings.hiddenEditors.isNotEmpty()) {
            store.writeApps(patch)
            store.setHiddenEditors(settings.hiddenEditors)
        } else {
            store.writeApps(patch)
        }
        return true
    }

    fun importLegacyProjectSettings(project: Project, workspacePath: String, editorType: String): Boolean {
        if (workspacePath.isBlank() && editorType.isBlank()) {
            return false
        }
        val store = ProjectConfigStore.getInstance(project)
        val anchor = store.resolveAnchorPath(workspacePath) ?: return false
        store.writeProject(
            JumperProjectConfig(
                anchorPath = PathKeyUtil.normalizeAnchorPath(anchor),
                vsCodeWorkspacePath = workspacePath,
                projectEditorType = editorType,
            ),
        )
        return true
    }

    fun migrateProjectOnStartup(project: Project) {
        val store = ProjectConfigStore.getInstance(project)
        val projectConfig = store.readProject()
        val globalExtras = GlobalConfigStore.getInstance().getJumperExtras()
        if (projectConfig.projectEditorType.isBlank() && globalExtras.selectedEditorType.isNotBlank()) {
            store.setProjectEditorType(globalExtras.selectedEditorType)
        }
    }
}
