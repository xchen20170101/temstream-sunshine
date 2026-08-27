# 安全策略

## 支持的版本

下表列出本项目当前受到安全更新支持的版本：

| 版本 | 支持状态 |
|---|---|
| `main` 分支最新提交 | ✅ 持续支持 |
| 旧版 Release | ❌ 不再支持，请升级到 `main` |

---

## 报告安全漏洞

我们非常重视安全问题。如果你发现了潜在的安全漏洞，**请勿在公开 Issue 中披露**。

请通过以下方式私下报告：

- **Email**：jia38403@gmail.com
- 主题建议格式：`[Security] <简要描述>`

请在报告中尽量包含以下信息：

1. 漏洞类型与影响范围
2. 复现步骤 / PoC（若有）
3. 受影响的版本 / commit
4. 可能的影响（如数据泄露、权限提升、远程代码执行等）
5. 你的联系方式（方便我们跟进）

我们承诺在收到报告后：

- **72 小时内**确认收到
- 评估并尽快修复高危问题
- 修复发布后在 [Releases](../../releases) 与 Release Notes 中致谢（除非你希望匿名）

---

## 已知的安全注意事项

> 以下事项在生产部署前**必须**完成，与本项目无关，但属于默认配置带来的安全风险：

1. **修改默认管理员账号**：后端首次启动会创建 `admin / admin`，请立即修改。
2. **修改数据库密码**：`backend/settings.yaml` 中默认 MySQL 密码为 `123456`，请修改或留空使用 SQLite。
3. **替换硬编码密钥**：
 - `backend/api/utils/encrpt_manager.go` 中的 AES 密钥（用于授权码加解密）
 - `backend/core/monitor.go` 与 `Sunshine/src/nvhttp.cpp` 中的 `DeviceApiKey`（后端 ↔ Sunshine TCP 鉴权密钥，两端必须一致）
4. **使用 HTTPS**：前端建议通过 Nginx / Caddy 反代并启用 TLS。
5. **限制端口暴露**：
 - 管理端 Web：`8090`
 - TCP 设备通道：`12345`（仅允许 Sunshine 主机访问）
 - Sunshine 自有端口（HTTP/HTTPS/RTSP 等）：仅允许局域网或 VPN

---

## 安全更新流程

1. 维护者在私有仓库中准备补丁与 Release Notes。
2. 合并到 `main` 并打 tag。
3. 在 GitHub [Releases](../../releases) 发布，并描述影响范围、严重程度、缓解措施。
4. 对于严重漏洞，会在仓库根目录的 `SECURITY.md` 中追加"已修复公告"条目。

---

## 参考资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [GitHub Security Advisories](https://docs.github.com/en/code-security/security-advisories)

---

如有其他安全相关问题，欢迎联系：jia38403@gmail.com
