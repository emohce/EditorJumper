package com.github.wanniwa.editorjumper.utils

object PluginIndexingEnvironment {
    private const val TRAVERSE_PROPERTY = "ezeditorjumper.searchable.options.traverse"

    fun isSearchableOptionsTraverse(): Boolean =
        System.getProperty(TRAVERSE_PROPERTY) == "true"
}
