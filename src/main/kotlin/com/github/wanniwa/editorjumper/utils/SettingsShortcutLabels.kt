package com.github.wanniwa.editorjumper.utils

import com.intellij.openapi.util.SystemInfo

object SettingsShortcutLabels {
    fun displayLabel(): String =
        if (SystemInfo.isMac) {
            I18nUtils.message("shortcut.openSettings.mac")
        } else {
            I18nUtils.message("shortcut.openSettings.win")
        }
}
