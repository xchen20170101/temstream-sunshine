# 更新日志

本项目的所有重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

---

## [Unreleased]

### 新增
- 顶层 `README.md`、`LICENSE`（Apache 2.0）
- `CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`、`CHANGELOG.md`
- `.editorconfig`、`.gitattributes`、`.github/` 工作流与 Issue / PR 模板
- `docs/` 目录收纳开发与设计文档

### 变更
- 后端协议由 Mulan PSL v2 切换为 Apache License 2.0
- 整理 `Readme.txt`：移除对 `deploy/`、`license_tool/` 等不存在目录的引用
- 删除过时的 `sunshine_installer.iss`

### 修复
- 在根 `.gitignore` 中排除 `moonlight-android/local.properties`，避免泄露本机 NDK 路径

---

## 历史版本

更早的版本变更（按提交迭代）未单独整理，可通过 `git log` 查看。
