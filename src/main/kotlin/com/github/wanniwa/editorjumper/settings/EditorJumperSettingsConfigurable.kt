package com.github.wanniwa.editorjumper.settings

import com.github.wanniwa.editorjumper.config.GlobalConfigStore
import com.github.wanniwa.editorjumper.config.ProjectConfigStore
import com.github.wanniwa.editorjumper.config.SharedEditorCatalog
import com.github.wanniwa.editorjumper.editors.EditorRegistry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.WithEpDependencies
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.github.wanniwa.editorjumper.statusbar.EditorJumperStatusBarWidget
import com.github.wanniwa.editorjumper.utils.I18nUtils
import com.github.wanniwa.editorjumper.utils.PluginIndexingEnvironment
import com.github.wanniwa.editorjumper.utils.ProjectSlotUtils
import javax.swing.JComponent
import java.util.ArrayList

class EditorJumperSettingsConfigurable : Configurable, WithEpDependencies {
    private var mySettingsComponent: EditorJumperSettingsComponent? = null
    private var searchableStubPanel: JComponent? = null
    private var configChangeListener: Runnable? = null

    override fun getDisplayName(): String = I18nUtils.message("settings.displayName")

    override fun getPreferredFocusedComponent(): JComponent =
        mySettingsComponent?.getPreferredFocusedComponent() ?: searchableStubPanel!!

    override fun createComponent(): JComponent {
        if (PluginIndexingEnvironment.isSearchableOptionsTraverse()) {
            searchableStubPanel = EditorJumperSearchableOptionsStub.createPanel()
            return searchableStubPanel!!
        }
        mySettingsComponent = EditorJumperSettingsComponent()
        val listener = Runnable {
            ApplicationManager.getApplication().invokeLater {
                if (mySettingsComponent != null) {
                    reset()
                }
            }
        }
        configChangeListener = listener
        GlobalConfigStore.getInstance().addChangeListener(listener)
        return mySettingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val component = mySettingsComponent ?: return false
        val settings = EditorJumperSettings.getInstance()
        if (component.getDefaultEditorType() != settings.selectedEditorType) {
            return true
        }
        val currentEditorType = getCurrentEditorType(settings, getCurrentProject())
        if (component.getSelectedEditorType() != currentEditorType) {
            return true
        }

        val (effSlot1, effSlot2, effSlot3) = ProjectSlotUtils.getEffectiveSlots(getCurrentProject())
        if (component.getShortcutSlot1() != effSlot1) {
            return true
        }
        if (component.getShortcutSlot2() != effSlot2) {
            return true
        }
        if (component.getShortcutSlot3() != effSlot3) {
            return true
        }

        val currentProject = getCurrentProject()
        if (currentProject != null) {
            val workspacePath = EditorJumperProjectSettings.getInstance(currentProject).vsCodeWorkspacePath
            if (component.getWorkspacePath() != workspacePath) {
                return true
            }
        }

        if (component.getAllEditorNames().any { name ->
                component.getPath(name) != settings.getPath(name)
            }) {
            return true
        }

        val hiddenSet = GlobalConfigStore.getInstance().getHiddenEditors()
        if (component.getAllEditorNames().any { name ->
                val modelVisible = !hiddenSet.contains(name)
                val uiVisible = component.isEditorVisible(name)
                modelVisible != uiVisible
            }) {
            return true
        }

        return false
    }

    override fun apply() {
        val component = mySettingsComponent ?: return
        val settings = EditorJumperSettings.getInstance()

        component.getAllEditorNames().forEach { name ->
            settings.setPath(name, component.getPath(name))
        }

        val allNames = component.getAllEditorNames()
        val selectedVisible = allNames.filter { component.isEditorVisible(it) }.toMutableSet()
        val hidden = allNames.filterNot { selectedVisible.contains(it) }.toMutableSet()
        settings.hiddenEditors = LinkedHashSet(hidden)
        settings.selectedEditorType = component.getDefaultEditorType()
        val slot1 = component.getShortcutSlot1()
        val slot2 = component.getShortcutSlot2()
        val slot3 = component.getShortcutSlot3()
        GlobalConfigStore.getInstance().setHiddenEditors(hidden)
        val newEditorType = component.getSelectedEditorType()

        val currentProject = getCurrentProject()
        if (currentProject != null) {
            ProjectConfigStore.getInstance(currentProject).setShortcutSlots(slot1, slot2, slot3, customized = true)
            val projectSettings = EditorJumperProjectSettings.getInstance(currentProject)
            projectSettings.projectEditorType = newEditorType
            projectSettings.vsCodeWorkspacePath = component.getWorkspacePath()
            val global = GlobalConfigStore.getInstance().getJumperExtras()
            GlobalConfigStore.getInstance().saveJumperExtras(
                global.shortcutSlot1,
                global.shortcutSlot2,
                global.shortcutSlot3,
                component.getDefaultEditorType(),
            )
        } else {
            settings.shortcutSlot1 = slot1
            settings.shortcutSlot2 = slot2
            settings.shortcutSlot3 = slot3
            GlobalConfigStore.getInstance().saveJumperExtras(
                slot1,
                slot2,
                slot3,
                component.getDefaultEditorType(),
            )
        }
        refreshStatusBarWidgets()
    }

    override fun reset() {
        val component = mySettingsComponent ?: return
        val settings = EditorJumperSettings.getInstance()
        component.rebuildCustomEditors(SharedEditorCatalog.customApps())
        component.getAllEditorNames().forEach { name ->
            component.setPath(name, settings.getPath(name))
        }

        val hiddenSet = GlobalConfigStore.getInstance().getHiddenEditors()
        component.getAllEditorNames().forEach { name ->
            component.setEditorVisible(name, !hiddenSet.contains(name))
        }

        val currentEditorType = getCurrentEditorType(settings, getCurrentProject())
        component.setDefaultEditorType(settings.selectedEditorType)
        component.setSelectedEditorType(currentEditorType)
        val (effSlot1, effSlot2, effSlot3) = ProjectSlotUtils.getEffectiveSlots(getCurrentProject())
        component.setShortcutSlot1(effSlot1)
        component.setShortcutSlot2(effSlot2)
        component.setShortcutSlot3(effSlot3)
        val currentProject = getCurrentProject()
        if (currentProject != null) {
            component.setWorkspacePath(EditorJumperProjectSettings.getInstance(currentProject).vsCodeWorkspacePath)
            component.setWorkspacePathEnabled(true)
        } else {
            component.setWorkspacePath("")
            component.setWorkspacePathEnabled(false)
        }
        refreshStatusBarWidgets()
    }

    private fun refreshStatusBarWidgets() {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (!project.isDefault) {
                WindowManager.getInstance().getStatusBar(project)?.updateWidget(EditorJumperStatusBarWidget.ID)
            }
        }
    }

    private fun getCurrentProject(): Project? =
        IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
            ?: ProjectManager.getInstance().openProjects.firstOrNull { !it.isDefault }

    private fun getCurrentEditorType(settings: EditorJumperSettings, project: Project?): String {
        if (project == null) return settings.selectedEditorType
        val projectSettings = EditorJumperProjectSettings.getInstance(project)
        return if (projectSettings.projectEditorType.isBlank()) {
            settings.selectedEditorType
        } else {
            projectSettings.projectEditorType
        }
    }

    override fun disposeUIResources() {
        configChangeListener?.let { GlobalConfigStore.getInstance().removeChangeListener(it) }
        configChangeListener = null
        mySettingsComponent = null
        searchableStubPanel = null
    }

    override fun getDependencies(): Collection<BaseExtensionPointName<*>> = ArrayList()
}
