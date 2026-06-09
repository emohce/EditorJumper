package com.github.wanniwa.editorjumper.utils

object PluginIndexingEnvironment {
    fun isSearchableOptionsTraverse(): Boolean {
        val requiredPluginId = System.getProperty("idea.required.plugins.id") ?: return false
        return requiredPluginId.contains("EzEditorJumper") || requiredPluginId.contains("EditorJumper")
    }
}
