package com.github.wanniwa.editorjumper.settings

import com.github.wanniwa.editorjumper.config.LegacyConfigMigration
import com.github.wanniwa.editorjumper.config.ProjectConfigStore
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "com.github.wanniwa.editorjumper.settings.EditorJumperProjectSettings",
    storages = [Storage("editorJumperProjectSettings.xml", deprecated = true)]
)
class EditorJumperProjectSettings(private val project: Project) :
    PersistentStateComponent<EditorJumperProjectSettingsState> {

    private var vsCodeWorkspacePathField: String = ""
    private var projectEditorTypeField: String = ""

    var vsCodeWorkspacePath: String
        get() = vsCodeWorkspacePathField.ifBlank {
            runCatching { ProjectConfigStore.getInstance(project).getVsCodeWorkspacePath() }.getOrDefault("")
        }
        set(value) {
            vsCodeWorkspacePathField = value
            runCatching { ProjectConfigStore.getInstance(project).setVsCodeWorkspacePath(value) }
        }

    var projectEditorType: String
        get() = projectEditorTypeField.ifBlank {
            runCatching { ProjectConfigStore.getInstance(project).getProjectEditorType() }.getOrDefault("")
        }
        set(value) {
            projectEditorTypeField = value
            runCatching { ProjectConfigStore.getInstance(project).setProjectEditorType(value) }
        }

    companion object {
        fun getInstance(project: Project): EditorJumperProjectSettings {
            return project.getService(EditorJumperProjectSettings::class.java)
        }
    }

    override fun getState(): EditorJumperProjectSettingsState = EditorJumperProjectSettingsState()

    override fun loadState(state: EditorJumperProjectSettingsState) {
        vsCodeWorkspacePathField = ""
        projectEditorTypeField = ""
        runCatching {
            LegacyConfigMigration.importLegacyProjectSettings(
                project,
                state.vsCodeWorkspacePath,
                state.projectEditorType,
            )
        }
    }
}

data class EditorJumperProjectSettingsState(
    var vsCodeWorkspacePath: String = "",
    var projectEditorType: String = "",
)
