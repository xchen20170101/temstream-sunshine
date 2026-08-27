# Temstream Sunshine

> 基于 Sunshine + Moonlight 的自托管云桌面 / 游戏串流集中管理平台

Temstream Sunshine 把 [Sunshine](https://github.com/LizardByte/Sunshine)（游戏串流服务端）和 [Moonlight](https://moonlight-stream.org)（客户端）整合成一套可集中运维的系统：管理员通过 Web 后台管理用户、设备、授权；终端用户用 Moonlight 客户端一键接入被串流主机。

---

## ✨ 功能特性

- **集中管理**：Web 后台统一管理 Sunshine 设备、用户、授权
- **动态 PIN 配对**：服务端统一签发 PIN，Moonlight 客户端自动完成配对
- **多平台客户端**：Qt 桌面客户端 + Android 客户端
- **设备 / 用户绑定**：支持设备分配 / 回收、用户与设备多对多
- **授权管理**：可选许可证激活与到期检查（测试 / 正式两种类型）
- **TCP 设备心跳**：基于轻量 TCP 长连接做存活探测
- **管理员 Web**：Vue 2 + Element UI，支持用户管理、设备管理、登录页

---

## 📐 架构

```
                            ┌─────────────────────┐
                            │  管理端 Web (front) │
                            │   Vue 2 + Element   │
                            └──────────┬──────────┘
                                       │ HTTPS / REST
                                       ▼
   ┌─────────────────────────────────────────────────┐
   │            backend (Go + Gin + SQLite)          │
   │   用户 /    设备      / TCP 监听 (端口 12345)    │
   └──────┬──────────────────────────────────────────┘
          │ TCP (心跳 / 注册 / PIN 请求)
          │ 携带 DeviceApiKey 鉴权
          ▼
   ┌─────────────────────┐         ┌─────────────────────┐
   │   Sunshine (C++)    │◄───────►│   Moonlight 客户端   │
   │   服务端: 游戏 / 桌面 │  GameStream │   Qt / Android     │
   └─────────────────────┘         └─────────────────────┘
```

> 后端通过 TCP（端口 `12345`）与 Sunshine 通信：设备注册、心跳、PIN 查询。

---

## 🧱 仓库结构

```
.
├── backend/                  # Go + Gin 管理后端 (REST + TCP)
├── front/                    # Vue 2 管理端 Web
├── Sunshine/                 # 基于 LizardByte/Sunshine 二次修改的服务端源码
│   ├── src/                  # C++ 源码（已添加中文注释与后端对接逻辑）
│   ├── config_tool_python/   # Windows 配置工具（编辑配置 / 注册用户）
│   └── ...
├── moonlight-qt/             # 上游 Moonlight Qt 客户端
├── moonlight-android/        # 上游 Moonlight Android 客户端
├── docs/                     # 设计文档与开发笔记
└── ...
```

### 各模块许可

| 模块 | 协议 | 说明 |
|---|---|---|
| `backend/` | **Apache License 2.0** | 本项目的自有代码 |
| `front/` | **Apache License 2.0** | 本项目的自有代码 |
| `Sunshine/` | GPL-3.0 | 上游 LizardByte/Sunshine 协议 |
| `moonlight-qt/` | GPL-3.0 + LGPL | 上游 moonlight-stream 协议 |
| `moonlight-android/` | GPL-3.0 | 上游 moonlight-stream 协议 |

> **本仓库顶层 `LICENSE` 仅约束 `backend/` 与 `front/` 中的自有代码。**Sunshine、Moonlight 等子模块保留各自上游协议，遵循其许可要求。

---

## 🚀 快速开始

### 1. 准备工作

| 组件 | 说明 |
|---|---|
| Go 1.18+ | 编译后端 |
| Node.js 16+ | 编译前端 |
| CMake 3.20+、MSVC / GCC / Clang | 编译 Sunshine（C++ 项目，体积较大，可直接使用预编译的二进制） |
| Qt 5.9+ / Android Studio + NDK | 编译 Moonlight 客户端（可选，使用官方发布版即可） |

### 2. 构建后端

```bash
cd backend
# Windows
go build -o stream-server.exe
# Linux / macOS
go build -o stream-server
```

后端启动时会读取 `settings.yaml`：

- Windows：`backend/settings.yaml`
- Linux：`/opt/stream_server/settings.yaml`

首次启动会自动创建 SQLite 数据库 `cloud_server.db` 与默认管理员账号 `admin/admin`，**生产环境请立即修改默认密码**。

### 3. 构建前端

```bash
cd front
npm install
npm run serve      # 开发模式
npm run build      # 产线构建（产物在 dist/）
```

构建产物可直接由后端静态托管，也可使用 Nginx / Caddy 反向代理到 `:8090`。

### 4. 安装 Sunshine

参考 [Sunshine 官方文档](https://docs.lizardbyte.dev/projects/sunshine)。**注意**：本仓库中的 `Sunshine/` 是带本地修改的源码（支持后端动态 PIN、Tailscale / Headscale 配置、配置文件 `config/sunshine_server.ini`）。如果要使用预编译版本，需自行评估兼容性。

启动 Sunshine 前需修改 `Sunshine/config/sunshine_server.ini`：

```ini
server_ip = <后端 IP>
```

### 5. 使用 Moonlight

从 [Moonlight 官方发布页](https://moonlight-stream.org/) 下载 Qt 或 Android 客户端，输入 Sunshine 主机 IP 即可使用。

---

## ⚙️ 后端配置示例（`settings.yaml`）

```yaml
mysql:                  # 留空则使用 SQLite
  host: 127.0.0.1
  port: 3306
  db: gvb
  user: root
  password: 123456
  log_level: dev
system:
  host: 0.0.0.0
  port: 8090
  env: release
```

---

## 🔌 API 文档

- 管理端接口：参见 [`backend/api.md`](./backend/api.md)
- 设备对接（Android 客户端）：参见 [`backend/api.md`](./backend/api.md)（`/client/login`、`/reset_password` 等）
- Sunshine 自有 API：<https://docs.lizardbyte.dev/projects/sunshine/latest/about/api.html>

### 关键接口（摘录）

| 路径 | 说明 |
|---|---|
| `POST /api/stream/v1/login` | 管理端账号登录（Cookie 鉴权） |
| `POST /api/stream/v1/client/login` | 客户端登录获取设备 IP + PIN |
| `POST /api/stream/v1/users` | 新增用户（管理员） |
| `GET  /api/stream/v1/devices` | 设备列表 |
| `POST /api/stream/v1/distribute` | 分配设备给用户 |
| `GET  /api/stream/v1/licenses` | 获取授权状态 |

---

## 🔐 安全提示

> ⚠️ 在生产部署前，请务必完成以下事项：

1. **修改默认管理员密码**（`admin/admin`）
2. **修改 MySQL 密码**（`settings.yaml` 中默认 `123456`）
3. **修改 `Sunshine/config/sunshine_server.ini` 中的 `server_ip`** 为后端实际地址
4. **防火墙仅暴露必要端口**：Web `:8090`、TCP `:12345`（仅允许 Sunshine 子网访问）
5. **使用 HTTPS**：前端建议通过 Nginx/Caddy 反向代理并启用 TLS

源码中存在两处硬编码密钥，请评估风险后替换：

- `backend/api/utils/encrpt_manager.go` —— AES 密钥
- `backend/core/monitor.go` 与 `Sunshine/src/nvhttp.cpp` —— `DeviceApiKey`，两端必须保持一致

详细披露策略见 [`SECURITY.md`](./SECURITY.md)。

---

## 🛠️ 开发与贡献

欢迎贡献代码、报告 Bug、提出建议。请阅读：

- [`CONTRIBUTING.md`](./CONTRIBUTING.md) —— 贡献指南
- [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md) —— 行为准则

---

## 📜 许可证

本项目自有代码采用 **Apache License 2.0**，详见 [`LICENSE`](./LICENSE)。

各子模块遵循各自上游许可：

- `Sunshine/` —— GPL-3.0（LizardByte）
- `moonlight-qt/` —— GPL-3.0 + LGPL（moonlight-stream）
- `moonlight-android/` —— GPL-3.0（moonlight-stream）

---

## 📬 联系方式

- Email：jia38403@gmail.com
- Issues：<https://github.com/xchen20170101/temstream-sunshine/issues>

---

## 🙏 致谢

- [LizardByte/Sunshine](https://github.com/LizardByte/Sunshine) —— 服务端核心
- [moonlight-stream](https://moonlight-stream.org) —— 客户端核心
- 所有贡献者
