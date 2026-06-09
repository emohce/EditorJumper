package com.github.wanniwa.editorjumper.statusbar

import com.github.wanniwa.editorjumper.settings.EditorJumperSettingsConfigurable
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.github.wanniwa.editorjumper.utils.ProjectSlotUtils
import com.github.wanniwa.editorjumper.utils.SettingsShortcutLabels
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget

class EditorJumperStatusBarWidget(private val project: Project) : StatusBarWidget,
    StatusBarWidget.MultipleTextValuesPresentation,
    Disposable {
    companion object {
        const val ID = "EditorJumperWidget"
    }

    private sealed interface StatusBarPopupItem
    private data class SlotPopupItem(val slot: Int, val shortcutLabel: String) : StatusBarPopupItem
    private object SettingsPopupItem : StatusBarPopupItem

    private var statusBar: StatusBar? = null

    override fun ID(): String = ID

    override fun getTooltipText(): String = I18nUtils.message("statusbar.tooltip")

    override fun getSelectedValue(): String {
        val editorType = ProjectSlotUtils.getSlotEditor(project, 1)
        return I18nUtils.message("statusbar.jumpTo", editorType)
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun getPopup(): ListPopup {
        val items = listOf(
            SlotPopupItem(1, "Alt+Shift+O"),
            SlotPopupItem(2, "Alt+Shift+I"),
            SlotPopupItem(3, "Alt+Shift+U"),
            SettingsPopupItem,
        )
        val step = object : BaseListPopupStep<StatusBarPopupItem>(
            I18nUtils.message("statusbar.popup.title"),
            items,
        ) {
            override fun getTextFor(value: StatusBarPopupItem): String = when (value) {
                is SlotPopupItem -> {
                    val editor = ProjectSlotUtils.getSlotEditor(project, value.slot)
                    I18nUtils.message("statusbar.slotItem", value.slot, value.shortcutLabel, editor)
                }
                SettingsPopupItem -> I18nUtils.message("statusbar.settingsItem", SettingsShortcutLabels.displayLabel())
            }

            override fun onChosen(value: StatusBarPopupItem, finalChoice: Boolean): PopupStep<*>? {
                if (!finalChoice) {
                    return this
                }
                ApplicationManager.getApplication().invokeLater {
                    when (value) {
                        is SlotPopupItem -> triggerSlot(value.slot)
                        SettingsPopupItem -> openSettings()
                    }
                }
                return PopupStep.FINAL_CHOICE
            }
        }
        return JBPopupFactory.getInstance().createListPopup(step)
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            EditorJumperSettingsConfigurable::class.java,
        )
    }

    private fun triggerSlot(slot: Int) {
        val actionId = when (slot) {
            1 -> "EditorJumper.ShortcutSlot1Action"
            2 -> "EditorJumper.ShortcutSlot2Action"
            3 -> "EditorJumper.ShortcutSlot3Action"
            else -> return
        }
        val action = ActionManager.getInstance().getAction(actionId) ?: return
        val frame = WindowManager.getInstance().getIdeFrame(project) ?: return
        ActionManager.getInstance().tryToExecute(
            action,
            null,
            frame.component,
            ActionPlaces.STATUS_BAR_PLACE,
            true,
        )
    }

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }
}
