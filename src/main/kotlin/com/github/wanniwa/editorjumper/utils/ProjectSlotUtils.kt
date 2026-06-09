package com.github.wanniwa.editorjumper.utils

import com.github.wanniwa.editorjumper.config.GlobalConfigStore
import com.github.wanniwa.editorjumper.config.ProjectConfigStore
import com.intellij.openapi.project.Project

object ProjectSlotUtils {
    fun getSlotEditor(project: Project?, slot: Int): String {
        val global = GlobalConfigStore.getInstance().getJumperExtras()
        val globalSlots = listOf(global.shortcutSlot1, global.shortcutSlot2, global.shortcutSlot3)
        val globalSlot = globalSlots.getOrElse(slot - 1) { "" }
        if (project == null || project.isDefault) {
            return globalSlot
        }
        val config = ProjectConfigStore.getInstance(project).readProject()
        if (!config.slotsCustomized) {
            return globalSlot
        }
        val projectSlot = when (slot) {
            1 -> config.shortcutSlot1
            2 -> config.shortcutSlot2
            3 -> config.shortcutSlot3
            else -> ""
        }
        return projectSlot.ifBlank { globalSlot }
    }

    fun getEffectiveSlots(project: Project?): Triple<String, String, String> {
        return Triple(
            getSlotEditor(project, 1),
            getSlotEditor(project, 2),
            getSlotEditor(project, 3),
        )
    }

    fun getGlobalSlots(): Triple<String, String, String> {
        val extras = GlobalConfigStore.getInstance().getJumperExtras()
        return Triple(extras.shortcutSlot1, extras.shortcutSlot2, extras.shortcutSlot3)
    }
}
