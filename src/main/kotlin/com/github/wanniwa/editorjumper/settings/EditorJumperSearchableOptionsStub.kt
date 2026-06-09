package com.github.wanniwa.editorjumper.settings

import com.github.wanniwa.editorjumper.editors.EditorRegistry
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

/**
 * Static settings panel used only during headless [buildSearchableOptions].
 * Avoids dynamic [javax.swing.ButtonGroup] updates that race with the indexer coroutine.
 */
object EditorJumperSearchableOptionsStub {
    fun createPanel(): JPanel {
        val builder = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><em>${I18nUtils.message("settings.hint.macOS")}</em></html>"))
            .addComponent(JBLabel("<html><em>${I18nUtils.message("settings.hint.windows")}</em></html>"))
            .addComponent(JBLabel("<html><em>${I18nUtils.message("settings.hint.example")}</em></html>"))
            .addSeparator()
            .addComponent(JBLabel("<html><em>${I18nUtils.message("settings.hint.defaultEditor")}</em></html>"))
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.defaultEditor.label")), JBLabel("-"), 1, false)
            .addSeparator()
            .addComponent(JBLabel("<html><em>${I18nUtils.message("settings.hint.shortcutSlots")}</em></html>"))
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot1.label")), JBLabel("-"), 1, false)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot2.label")), JBLabel("-"), 1, false)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot3.label")), JBLabel("-"), 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.projectSettings.workspacePath")), JBLabel("-"), 1, false)
            .addSeparator()
        val editorsPanel = JPanel()
        editorsPanel.add(JBLabel("Editor"))
        editorsPanel.add(JBLabel("Path"))
        editorsPanel.add(JBLabel("Hide"))
        builder.addComponent(editorsPanel)
        EditorRegistry.editorNames.forEach { name ->
            builder.addLabeledComponent(JBLabel(name), JBLabel("-"), 1, false)
        }
        builder.addComponent(JBLabel(I18nUtils.message("settings.addCustomEditor")))
        return builder.panel
    }
}
