# Project Rules

Tool: codex

## Project Profile

- Name: `EzEditorJumper`
- Current inferred stack: Editor integration utility project
- Migration date: 2026-06-06

## Detected Manifests

- `build.gradle.kts`

## Local Rule Policy

- Keep project-specific constraints here; move reusable cross-project rules to CodeNote.
- Do not overwrite existing user work or unrelated business files.
- Before implementation, inspect the relevant source paths and existing docs for the current task.
- For UI work, follow project style first, then CodeNote UI rules.
- For security, data, release, or permission work, apply CodeNote high-risk gates.

## High-Risk Areas

- Treat configuration, credentials, release scripts, generated artifacts, data mutations, and external-service writes as high risk until project-specific rules say otherwise.
- Add concrete high-risk paths here as they are discovered.
## Migrated Project-Specific Constraints

- 修改 editor/jumper 行为前必须确认触发入口、焦点上下文和目标文件定位规则。
- 修改快捷键或 action 前必须确认冲突和回归路径。
- 构建产物和 IDE 生成文件不手改。
