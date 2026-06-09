package com.github.wanniwa.editorjumper.editors

import com.github.wanniwa.editorjumper.settings.EditorJumperSettings
import com.intellij.openapi.project.Project

class EditorHandlerFactory {
    companion object {
        fun getHandler(editorType: String, customPath: String, project: Project?): EditorHandler {
            val registration = EditorRegistry.find(editorType)
            if (registration != null) {
                return registration.create(customPath, project)
            }
            val path = customPath.ifBlank { EditorJumperSettings.getInstance().getPath(editorType) }
            val cfg = EditorConfig(name = editorType, supportsWorkspace = true, quotePaths = true)
            return ConfigBasedEditorHandler(cfg, path.takeIf { it.isNotBlank() }, project)
        }

        fun getHandler(editorType: String, project: Project?): EditorHandler {
            val settings = EditorJumperSettings.getInstance()
            return getHandler(editorType, settings.getPath(editorType), project)
        }
    }
}
