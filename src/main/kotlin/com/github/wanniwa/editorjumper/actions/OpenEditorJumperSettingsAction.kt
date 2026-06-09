package com.github.wanniwa.editorjumper.actions

import com.github.wanniwa.editorjumper.settings.EditorJumperSettingsConfigurable
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.github.wanniwa.editorjumper.utils.SettingsShortcutLabels
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

class OpenEditorJumperSettingsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project,
            EditorJumperSettingsConfigurable::class.java,
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
        e.presentation.text = I18nUtils.message("action.openSettings.text", SettingsShortcutLabels.displayLabel())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
