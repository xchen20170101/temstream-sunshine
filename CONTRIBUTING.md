# 贡献指南

欢迎贡献！无论是报告 Bug、提出功能建议、改进文档，还是直接提交 Pull Request，我们都非常感谢。

---

## 📋 行为准则

参与本项目即表示你同意遵守 [行为准则](./CODE_OF_CONDUCT.md)。请在所有互动中保持友善与专业。

---

## 🐛 报告 Bug

1. 前往 [Issues](../../issues) 页面，确认该 Bug 尚未被报告。
2. 使用 **Bug Report** 模板提交。
3. 提供尽量详细的信息：复现步骤、期望行为、实际行为、截图 / 日志、环境信息（操作系统、Go / Node 版本等）。

---

## 💡 提出功能建议

1. 前往 [Issues](../../issues) 页面，使用 **Feature Request** 模板。
2. 描述清楚：
 - 想要解决的问题
 - 期望的解决方案
 - 是否有备选方案
 - 附加背景与截图（如有）

---

## 🔧 提交 Pull Request

### 流程

1. **Fork** 本仓库，并从 `main` 分支创建你的特性分支：
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **进行修改**。请遵循已有的代码风格：
   - Go：`gofmt` / `goimports`
   - Vue / JS / CSS：遵循 `front/.editorconfig` 与 Vue 2 / Element UI 规范
   - C++：`Sunshine/.clang-format`
   - PowerShell / Shell：保持可读性，避免引入新依赖

3. **提交修改**。Commit 信息建议使用清晰的中文 / 英文描述，例如：
   ```
   feat(backend): 新增设备心跳间隔可配置项
   fix(front): 修复用户列表分页错误
   docs: 补充 API 鉴权说明
   ```

4. **推送并创建 Pull Request**：
   ```bash
   git push origin feature/your-feature-name
   ```
   请在 PR 描述中说明：
   - 解决了什么问题
   - 关键实现思路
   - 是否有破坏性变更
   - 测试情况

5. 等待 **CI** 与 **Code Review** 通过后合并。

### PR 检查清单

提交 PR 前请确认：

- [ ] 代码能成功构建（`go build ./...`、`npm run build`、Sunshine 的 CMake 构建等）
- [ ] 已添加 / 更新必要的单元测试（如适用）
- [ ] 已更新相关文档（README、API 文档、CHANGELOG）
- [ ] Commit 信息清晰可读
- [ ] 没有引入无关的格式化 / 重构噪音
- [ ] 不包含敏感信息（密钥、令牌、绝对路径、用户名密码等）

---

## 🧪 开发环境

| 模块 | 依赖 |
|---|---|
| `backend/` | Go 1.18+ |
| `front/` | Node.js 16+、npm 8+ |
| `Sunshine/` | CMake 3.20+、MSVC 2022 / GCC / Clang、Boost、OpenSSL 等（详见 [Sunshine 官方文档](https://docs.lizardbyte.dev/projects/sunshine)） |
| `moonlight-qt/` | Qt 5.9+ 或 Qt 6+ |
| `moonlight-android/` | Android Studio + Android NDK |

详细的本地启动说明参见 [README.md](./README.md)。

---

## 📁 项目结构

请在修改前熟悉仓库目录结构（见 [README.md](./README.md) 中"仓库结构"一节）。

修改时请遵守以下边界：

- **`backend/`、`front/`** 属于本项目自有代码，欢迎自由改进。
- **`Sunshine/`、`moonlight-qt/`、`moonlight-android/`** 是上游子项目：
 - **常规 bug 修复 / 小幅适配**可以直接提交到这里。
 - **大幅功能改动**请先在上游提 Issue / PR，避免与上游脱节。
 - 修改时请保持子项目自身的协议不变。

---

## 🔐 敏感信息

提交前请检查：

- 不要提交任何 **私钥、令牌、密码、机器码、个人邮箱（除公开联系邮箱外）**
- 不要提交 **构建产物**（`bin/`、`dist/`、`*.exe`、`*.dll` 等，已在 `.gitignore` 中）
- 不要提交 **IDE 本地配置**（`.idea/`、`.vscode/`、`*.local.properties` 等，已在 `.gitignore` 中）

---

## 📜 许可证

提交代码即表示你同意你的贡献按本项目根目录的 [LICENSE](./LICENSE)（Apache License 2.0）授权。各子模块遵循各自上游协议。

---

## 📬 联系方式

如有任何问题，欢迎：

- 在 [Issues](../../issues) 提问
- 发送邮件至：jia38403@gmail.com

感谢你的贡献！
