package com.github.wanniwa.editorjumper.actions

import com.github.wanniwa.editorjumper.editors.EditorHandler
import com.github.wanniwa.editorjumper.editors.EditorHandlerFactory
import com.github.wanniwa.editorjumper.settings.EditorJumperSettings
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Shortcut Slot 2 Action (Alt+Shift+I)
 * Opens project in the editor configured for shortcut slot 2
 */
class ShortcutSlot2Action : BaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get editor handler for shortcut slot 2
        val settings = EditorJumperSettings.getInstance()
        val editorType = settings.shortcutSlot2
        val handler = EditorHandlerFactory.getHandler(editorType, project)

        // Check editor path exists
        if (!checkEditorPathForType(project, editorType)) {
            return
        }

        val selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        // Get cursor position
        var lineNumber = editor?.caretModel?.currentCaret?.logicalPosition?.line?.plus(1)
        var columnNumber = editor?.caretModel?.currentCaret?.logicalPosition?.column?.plus(1)

        // Open in external editor
        openInExternalEditor(project, handler, selectedFile, lineNumber, columnNumber)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        // Update menu item text to show the configured editor
        val settings = EditorJumperSettings.getInstance()
        val editorType = settings.shortcutSlot2
        e.presentation.text = I18nUtils.message("action.shortcutSlot2.text", editorType)
    }

    override fun getEditorHandler(project: Project): EditorHandler {
        val settings = EditorJumperSettings.getInstance()
        val editorType = settings.shortcutSlot2
        return EditorHandlerFactory.getHandler(editorType, project)
    }

    private fun checkEditorPathForType(project: Project, editorType: String): Boolean {
        val settings = EditorJumperSettings.getInstance()
        val customPath = settings.getPath(editorType)

        // macOS: No path check needed
        if (com.intellij.openapi.util.SystemInfo.isMac) {
            return true
        }

        // Windows: Check for Cursor/Qoder/Antigravity
        if (com.intellij.openapi.util.SystemInfo.isWindows && listOf("Cursor", "Qoder", "Antigravity").contains(editorType)) {
            return true
        }

        // Check custom path
        if (customPath.isBlank()) {
            val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
                project,
                I18nUtils.message("dialog.editorPathNotConfigured.message", editorType),
                I18nUtils.message("dialog.editorPathNotConfigured.title"),
                I18nUtils.message("dialog.editorPathNotConfigured.openSettings"),
                I18nUtils.message("dialog.editorPathNotConfigured.cancel"),
                com.intellij.openapi.ui.Messages.getWarningIcon()
            )

            if (result == com.intellij.openapi.ui.Messages.YES) {
                com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    com.github.wanniwa.editorjumper.settings.EditorJumperSettingsConfigurable::class.java
                )
            }
            return false
        }
        return true
    }
}
