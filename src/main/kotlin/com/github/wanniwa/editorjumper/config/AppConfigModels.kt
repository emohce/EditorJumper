package com.github.wanniwa.editorjumper.config

data class AppEntry(
    var name: String = "",
    var commandPath: String? = null,
    var isCustom: Boolean = false,
    var hidden: Boolean = false,
    var updatedAt: String? = null,
)

data class JumperExtras(
    var shortcutSlot1: String = "Cursor",
    var shortcutSlot2: String = "Visual Studio Code",
    var shortcutSlot3: String = "Windsurf",
    var selectedEditorType: String = "Cursor",
)

data class SharedAppsConfig(
    var version: Int = 1,
    var revision: Int = 0,
    var jetbrainsApps: MutableList<AppEntry> = mutableListOf(),
    var vscodeApps: MutableList<AppEntry> = mutableListOf(),
    var jumperExtras: JumperExtras = JumperExtras(),
)

data class JumperProjectConfig(
    var version: Int = 1,
    var anchorPath: String = "",
    var vsCodeWorkspacePath: String = "",
    var projectEditorType: String = "",
    var shortcutSlot1: String = "",
    var shortcutSlot2: String = "",
    var shortcutSlot3: String = "",
    var slotsCustomized: Boolean = false,
)
