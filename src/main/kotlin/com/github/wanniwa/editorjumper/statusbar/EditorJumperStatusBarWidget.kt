package com.github.wanniwa.editorjumper.statusbar

import com.github.wanniwa.editorjumper.settings.EditorJumperSettingsConfigurable
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.github.wanniwa.editorjumper.utils.ProjectSlotUtils
import com.github.wanniwa.editorjumper.utils.SettingsShortcutLabels
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
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
        var pendingChoice: StatusBarPopupItem? = null
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
                pendingChoice = value
                return PopupStep.FINAL_CHOICE
            }

            override fun getFinalRunnable(): Runnable? {
                val choice = pendingChoice ?: return null
                pendingChoice = null
                return Runnable {
                    when (choice) {
                        is SlotPopupItem -> triggerSlot(choice.slot)
                        SettingsPopupItem -> openSettings()
                    }
                }
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
        val action = when (slot) {
            1 -> ActionManager.getInstance().getAction("EditorJumper.ShortcutSlot1Action")
            2 -> ActionManager.getInstance().getAction("EditorJumper.ShortcutSlot2Action")
            3 -> ActionManager.getInstance().getAction("EditorJumper.ShortcutSlot3Action")
            else -> null
        } as? AnAction ?: return
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
        val anActionEvent = AnActionEvent(
            null,
            dataContext,
            ActionPlaces.STATUS_BAR_PLACE,
            Presentation(),
            ActionManager.getInstance(),
            0,
        )
        action.actionPerformed(anActionEvent)
    }

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }
}
