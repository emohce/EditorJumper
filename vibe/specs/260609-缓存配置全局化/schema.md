# 缓存配置全局化 — Schema 契约

**Baseline**: 2026-06-09 | **Status**: 实施中

## 缓存根目录

| 平台 | 路径 |
|------|------|
| macOS | `~/Library/Caches/EzEditorJumper/` |
| Windows | `%LOCALAPPDATA%\EzEditorJumper\cache\` |
| Linux | `$XDG_CACHE_HOME/EzEditorJumper/` 或 `~/.cache/EzEditorJumper/` |

## shared-apps.json

```json
{
  "version": 1,
  "revision": 0,
  "jetbrainsApps": [
    { "name": "IDEA", "commandPath": null, "isCustom": false, "hidden": false, "updatedAt": null }
  ],
  "vscodeApps": [
    { "name": "Cursor", "commandPath": null, "isCustom": false, "hidden": false, "updatedAt": null }
  ],
  "jumperExtras": {
    "shortcutSlot1": "Cursor",
    "shortcutSlot2": "Visual Studio Code",
    "shortcutSlot3": "Windsurf",
    "selectedEditorType": "Cursor"
  }
}
```

## 项目配置 `{configKey}_{basename}_{jumper|jumper-v}.json`

### Jumper

```json
{
  "version": 1,
  "anchorPath": "/abs/path",
  "vsCodeWorkspacePath": "",
  "projectEditorType": "Cursor"
}
```

### JumperV

```json
{
  "version": 1,
  "anchorPath": "/abs/path",
  "jetBrainsRootProjectPath": "",
  "slotTargets": [
    { "slot": 1, "type": "jetbrains", "target": "IDEA" },
    { "slot": 2, "type": "vscode-app", "target": "Cursor" },
    { "slot": 3, "type": "vscode-app", "target": "Windsurf" }
  ],
  "jumpBackSource": ""
}
```

## PathKeyUtil

1. 解析 anchor 路径（精确路径）
2. `path.resolve` + 规范化：统一 `/`，Windows 盘符小写
3. `configKey = sha256(anchorPath).hex.substring(0, 16)`

### Golden vectors

| anchorPath | configKey (first 16 hex) |
|------------|--------------------------|
| `/Users/test/myproject` | `8f14e45f...` (platform-normalized) |
| `C:/Projects/demo` | 与 `c:/projects/demo` 相同 |

## 名称对齐表（canonical name）

JetBrains: IDEA, WebStorm, PyCharm, GoLand, CLion, PhpStorm, RubyMine, Rider, Android Studio, Xcode

VSCode-family: Visual Studio Code, Cursor, Trae, Windsurf, Void, Kiro, Qoder, CatPawAI, Antigravity, Trae CN, CodeBuddy, CodeBuddy CN

## Non-goals

- 不做 Windsurf/Devin 启动路径扫描
- 不改 shortcut 键位
- 不合并 jumper/jumper-v 项目 JSON 为单文件
