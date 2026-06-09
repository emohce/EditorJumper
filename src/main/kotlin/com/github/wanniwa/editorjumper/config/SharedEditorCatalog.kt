package com.github.wanniwa.editorjumper.config

import com.github.wanniwa.editorjumper.editors.EditorRegistry

object SharedEditorCatalog {
    fun builtinNames(): List<String> = EditorRegistry.editorNames

    fun customApps(): List<AppEntry> {
        val builtin = builtinNames().toSet()
        return GlobalConfigStore.getInstance().readApps().vscodeApps.filter {
            it.isCustom && it.name !in builtin
        }
    }

    fun allAppNames(): List<String> = builtinNames() + customApps().map { it.name }
}
