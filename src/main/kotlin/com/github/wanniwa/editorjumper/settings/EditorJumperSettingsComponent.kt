package com.github.wanniwa.editorjumper.settings

import com.github.wanniwa.editorjumper.config.AppEntry
import com.github.wanniwa.editorjumper.config.GlobalConfigStore
import com.github.wanniwa.editorjumper.config.SharedEditorCatalog
import com.github.wanniwa.editorjumper.editors.EditorRegistry
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.FormBuilder
import com.github.wanniwa.editorjumper.utils.I18nUtils
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.DefaultComboBoxModel
import javax.swing.ButtonGroup
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.FlowLayout

private data class CustomEditorUi(
    val radio: JBRadioButton,
    val pathField: TextFieldWithBrowseButton,
    val hiddenCheckBox: JBCheckBox,
    val label: JBLabel,
)

class EditorJumperSettingsComponent {
    private val myMainPanel: JPanel
    private val editorTypeComboBox = ComboBox<String>()
    private val editorNameLabels: MutableMap<String, JBLabel> = mutableMapOf()

    // Shortcut slot combo boxes
    private val shortcutSlot1ComboBox = ComboBox<String>()
    private val shortcutSlot2ComboBox = ComboBox<String>()
    private val shortcutSlot3ComboBox = ComboBox<String>()
    private val workspacePathField = TextFieldWithBrowseButton()

    private val pathFields: Map<String, TextFieldWithBrowseButton> =
        EditorRegistry.editors.associate { editor ->
            val field = TextFieldWithBrowseButton()
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            descriptor.title = I18nUtils.getFileChooserTitle(editor.name)
            field.addBrowseFolderListener(TextBrowseFolderListener(descriptor))
            editor.name to field
        }

    private val hiddenCheckBoxes: Map<String, JBCheckBox> =
        EditorRegistry.editors.associate { editor ->
            editor.name to JBCheckBox()
        }

    private val selectedButtons: Map<String, JBRadioButton> =
        EditorRegistry.editors.associate { editor ->
            editor.name to JBRadioButton()
        }

    private val buttonGroup = ButtonGroup()
    private val customEditorsPanel = JPanel(GridBagLayout())
    private val customRows = LinkedHashMap<String, CustomEditorUi>()
    private val addCustomEditorButton = JButton(I18nUtils.message("settings.addCustomEditor"))

    init {
        editorTypeComboBox.model = DefaultComboBoxModel(EditorRegistry.editorNames.toTypedArray())
        shortcutSlot1ComboBox.model = DefaultComboBoxModel(EditorRegistry.editorNames.toTypedArray())
        shortcutSlot2ComboBox.model = DefaultComboBoxModel(EditorRegistry.editorNames.toTypedArray())
        shortcutSlot3ComboBox.model = DefaultComboBoxModel(EditorRegistry.editorNames.toTypedArray())

        val workspaceDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
        workspaceDescriptor.title = "Select Workspace File"
        workspaceDescriptor.withFileFilter { file -> file.name.endsWith(".code-workspace") }
        workspacePathField.addBrowseFolderListener(TextBrowseFolderListener(workspaceDescriptor))

        val macHintLabel = JBLabel("<html><em>${I18nUtils.message("settings.hint.macOS")}</em></html>")
        val windowsHintLabel = JBLabel("<html><em>${I18nUtils.message("settings.hint.windows")}</em></html>")
        val exampleLabel = JBLabel("<html><em>${I18nUtils.message("settings.hint.example")}</em></html>")
        val defaultEditorHintLabel = JBLabel("<html><em>${I18nUtils.message("settings.hint.defaultEditor")}</em></html>")
        val shortcutSlotsHintLabel = JBLabel("<html><em>${I18nUtils.message("settings.hint.shortcutSlots")}</em></html>")

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(macHintLabel)
            .addComponent(windowsHintLabel)
            .addComponent(exampleLabel)
            .addSeparator()
            .addComponent(defaultEditorHintLabel)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.defaultEditor.label")), editorTypeComboBox, 1, false)
            .addSeparator()
            .addComponent(shortcutSlotsHintLabel)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot1.label")), shortcutSlot1ComboBox, 1, false)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot2.label")), shortcutSlot2ComboBox, 1, false)
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.shortcutSlot3.label")), shortcutSlot3ComboBox, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel(I18nUtils.message("settings.projectSettings.workspacePath")), workspacePathField, 1, false)
            .addSeparator()

        // Editors table: Editor | Path | Hidden
        val editorsPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(2, 2, 2, 2)
        }

        // Header row
        gbc.gridy = 0
        gbc.weighty = 0.0

        gbc.gridx = 0
        gbc.weightx = 0.0
        editorsPanel.add(JBLabel("Editor"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        editorsPanel.add(JBLabel("Path"), gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        editorsPanel.add(JBLabel("Hide"), gbc)

        // Data rows
        var row = 1
        EditorRegistry.editors.forEach { editor ->
            val radio = selectedButtons[editor.name]!!
            radio.text = ""
            buttonGroup.add(radio)

            val pathField = pathFields[editor.name]!!
            val hiddenCheckBox = hiddenCheckBoxes[editor.name]!!
            hiddenCheckBox.text = ""
            hiddenCheckBox.addActionListener {
                updateEditorNameStyle(editor.name)
            }

            gbc.gridy = row

            val editorLabel = JBLabel(editor.name)
            editorNameLabels[editor.name] = editorLabel
            val editorCell = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                isOpaque = false
                add(radio)
                add(editorLabel)
            }
            gbc.gridx = 0
            gbc.weightx = 0.0
            editorsPanel.add(editorCell, gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            editorsPanel.add(pathField, gbc)

            gbc.gridx = 2
            gbc.weightx = 0.0
            editorsPanel.add(hiddenCheckBox, gbc)

            row++
        }

        formBuilder.addComponent(editorsPanel)
            .addComponent(customEditorsPanel)
            .addComponent(addCustomEditorButton)

        addCustomEditorButton.addActionListener { showAddCustomEditorDialog() }

        myMainPanel = formBuilder.panel
    }

    fun getPanel(): JPanel = myMainPanel

    fun getPreferredFocusedComponent(): JComponent = editorTypeComboBox

    fun getPath(editorName: String): String =
        pathFields[editorName]?.text ?: customRows[editorName]?.pathField?.text ?: ""

    fun setPath(editorName: String, path: String) {
        pathFields[editorName]?.text = path
        customRows[editorName]?.pathField?.text = path
    }

    fun isEditorVisible(editorName: String): Boolean {
        val hidden = hiddenCheckBoxes[editorName]?.isSelected
            ?: customRows[editorName]?.hiddenCheckBox?.isSelected
            ?: false
        return !hidden
    }

    fun setEditorVisible(editorName: String, visible: Boolean) {
        hiddenCheckBoxes[editorName]?.isSelected = !visible
        customRows[editorName]?.hiddenCheckBox?.isSelected = !visible
        updateEditorNameStyle(editorName)
    }

    fun getAllEditorNames(): List<String> = SharedEditorCatalog.allAppNames()

    fun rebuildCustomEditors(apps: List<AppEntry>) {
        customRows.values.toList().forEach { buttonGroup.remove(it.radio) }
        customRows.clear()
        customEditorsPanel.removeAll()
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(2, 2, 2, 2)
        }
        apps.forEachIndexed { index, app ->
            val radio = JBRadioButton()
            radio.text = ""
            buttonGroup.add(radio)
            val pathField = TextFieldWithBrowseButton()
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            descriptor.title = I18nUtils.getFileChooserTitle(app.name)
            pathField.addBrowseFolderListener(TextBrowseFolderListener(descriptor))
            pathField.text = app.commandPath ?: ""
            val hiddenCheckBox = JBCheckBox()
            hiddenCheckBox.text = ""
            hiddenCheckBox.isSelected = app.hidden
            hiddenCheckBox.addActionListener { updateEditorNameStyle(app.name) }
            val label = JBLabel(app.name + I18nUtils.message("settings.customEditor.suffix"))
            editorNameLabels[app.name] = label
            val removeButton = JButton(I18nUtils.message("settings.removeCustomEditor"))
            removeButton.addActionListener { removeCustomEditor(app.name) }
            val editorCell = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                isOpaque = false
                add(radio)
                add(label)
                add(removeButton)
            }
            gbc.gridy = index
            gbc.gridx = 0
            gbc.weightx = 0.0
            customEditorsPanel.add(editorCell, gbc)
            gbc.gridx = 1
            gbc.weightx = 1.0
            customEditorsPanel.add(pathField, gbc)
            gbc.gridx = 2
            gbc.weightx = 0.0
            customEditorsPanel.add(hiddenCheckBox, gbc)
            customRows[app.name] = CustomEditorUi(radio, pathField, hiddenCheckBox, label)
        }
        customEditorsPanel.revalidate()
        customEditorsPanel.repaint()
        refreshComboBoxModels()
    }

    fun setHideToggleEnabled(editorName: String, enabled: Boolean) {
        hiddenCheckBoxes[editorName]?.isEnabled = enabled
    }

    fun getDefaultEditorType(): String = editorTypeComboBox.selectedItem as String

    fun setDefaultEditorType(editorType: String) {
        editorTypeComboBox.selectedItem = editorType
    }

    fun getSelectedEditorType(): String {
        val builtin = selectedButtons.entries.firstOrNull { it.value.isSelected }?.key
        if (builtin != null) return builtin
        return customRows.entries.firstOrNull { it.value.radio.isSelected }?.key
            ?: EditorRegistry.editorNames.first()
    }

    fun setSelectedEditorType(editorType: String) {
        val hasBuiltin = selectedButtons.containsKey(editorType)
        val hasCustom = customRows.containsKey(editorType)
        val target = when {
            hasBuiltin -> editorType
            hasCustom -> editorType
            else -> EditorRegistry.editorNames.first()
        }
        selectedButtons.toList().forEach { (name, button) ->
            button.isEnabled = true
            button.isSelected = name == target
        }
        customRows.toList().forEach { (name, ui) ->
            ui.radio.isEnabled = true
            ui.radio.isSelected = name == target
        }
    }

    fun getShortcutSlot1(): String = shortcutSlot1ComboBox.selectedItem as String
    fun setShortcutSlot1(editorType: String) {
        shortcutSlot1ComboBox.selectedItem = editorType
    }

    fun getShortcutSlot2(): String = shortcutSlot2ComboBox.selectedItem as String
    fun setShortcutSlot2(editorType: String) {
        shortcutSlot2ComboBox.selectedItem = editorType
    }

    fun getShortcutSlot3(): String = shortcutSlot3ComboBox.selectedItem as String
    fun setShortcutSlot3(editorType: String) {
        shortcutSlot3ComboBox.selectedItem = editorType
    }

    fun getWorkspacePath(): String = workspacePathField.text

    fun setWorkspacePath(path: String) {
        workspacePathField.text = path
    }

    fun setWorkspacePathEnabled(enabled: Boolean) {
        workspacePathField.isEnabled = enabled
    }

    private fun refreshComboBoxModels() {
        val names = getAllEditorNames().toTypedArray()
        editorTypeComboBox.model = DefaultComboBoxModel(names)
        shortcutSlot1ComboBox.model = DefaultComboBoxModel(names)
        shortcutSlot2ComboBox.model = DefaultComboBoxModel(names)
        shortcutSlot3ComboBox.model = DefaultComboBoxModel(names)
    }

    private fun showAddCustomEditorDialog() {
        val name = Messages.showInputDialog(
            I18nUtils.message("settings.customEditorName.prompt"),
            I18nUtils.message("settings.addCustomEditor"),
            Messages.getQuestionIcon(),
        )?.trim().orEmpty()
        if (name.isEmpty()) return
        if (getAllEditorNames().contains(name)) {
            Messages.showErrorDialog(
                myMainPanel,
                I18nUtils.message("settings.customEditorName.duplicate", name),
                I18nUtils.message("settings.addCustomEditor"),
            )
            return
        }
        if (!GlobalConfigStore.getInstance().addCustomVscodeApp(name)) {
            Messages.showErrorDialog(
                myMainPanel,
                I18nUtils.message("settings.customEditorName.duplicate", name),
                I18nUtils.message("settings.addCustomEditor"),
            )
            return
        }
        rebuildCustomEditors(SharedEditorCatalog.customApps())
        setSelectedEditorType(name)
    }

    private fun removeCustomEditor(name: String) {
        GlobalConfigStore.getInstance().removeCustomVscodeApp(name)
        editorNameLabels.remove(name)
        rebuildCustomEditors(SharedEditorCatalog.customApps())
    }

    private fun updateEditorNameStyle(editorName: String) {
        val label = editorNameLabels[editorName] ?: return
        val hidden = hiddenCheckBoxes[editorName]?.isSelected
            ?: customRows[editorName]?.hiddenCheckBox?.isSelected
            ?: false
        val suffix = if (customRows.containsKey(editorName)) I18nUtils.message("settings.customEditor.suffix") else ""
        val displayName = editorName + suffix
        label.text = if (hidden) "<html><s>$displayName</s></html>" else displayName
    }
}
