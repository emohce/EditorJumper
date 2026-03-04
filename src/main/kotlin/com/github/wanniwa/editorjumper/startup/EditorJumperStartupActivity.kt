package com.github.wanniwa.editorjumper.startup

import com.github.wanniwa.editorjumper.settings.EditorJumperProjectSettings
import com.github.wanniwa.editorjumper.settings.EditorJumperSettings
import com.github.wanniwa.editorjumper.actions.OpenInExternalEditorAction
import com.github.wanniwa.editorjumper.actions.FastOpenInExternalEditorAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class EditorJumperStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // 获取全局设置和项目设置
        val globalSettings = EditorJumperSettings.getInstance()
        val projectSettings = EditorJumperProjectSettings.getInstance(project)

        // 如果项目设置为空，使用全局设置
        if (projectSettings.projectEditorType.isBlank()) {
            projectSettings.projectEditorType = globalSettings.selectedEditorType
        }

        // 动态注册按编辑器划分的动作，便于在 Keymap 中为不同编辑器绑定快捷键
        val actionManager = ActionManager.getInstance()
        val editorTypes = listOf(
            "Cursor" to "Cursor",
            "Visual Studio Code" to "Vscode",
            "Trae" to "Trae",
            "Windsurf" to "Windsurf",
            "Void" to "Void",
            "Kiro" to "Kiro",
            "Qoder" to "Qoder",
            "CatPawAI" to "CatPawAI",
            "Antigravity" to "Antigravity"
        )

        for ((editorType, suffix) in editorTypes) {
            val openId = "EditorJumper.OpenIn" + suffix
            if (actionManager.getAction(openId) == null) {
                val action = object : OpenInExternalEditorAction() {
                    override fun getTargetEditor(project: Project): String {
                        return editorType
                    }
                }
                actionManager.registerAction(openId, action)
            }

            val fastId = "EditorJumper.FastOpenIn" + suffix
            if (actionManager.getAction(fastId) == null) {
                val fastAction = object : FastOpenInExternalEditorAction() {
                    override fun getTargetEditor(project: Project): String {
                        return editorType
                    }
                }
                actionManager.registerAction(fastId, fastAction)
            }
        }
    }
} 