# Moonlight-Qt 与 Sunshine 配对流程分析

## 目录
- [概述](#概述)
- [配对流程整体架构](#配对流程整体架构)
- [详细配对步骤](#详细配对步骤)
- [核心组件分析](#核心组件分析)
- [安全机制](#安全机制)
- [关键代码位置](#关键代码位置)
- [总结](#总结)

## 概述

Moonlight-Qt 与 Sunshine 的配对是基于 NVIDIA GameStream 协议的 PIN 码认证机制。配对过程涉及多个加密操作、证书交换和双向认证，确保只有授权客户端可以连接到流媒体服务器。

## 配对流程整体架构

### 系统组件
- **Moonlight-Qt (客户端)**：负责发起配对请求，执行 PIN 码验证
- **Sunshine (服务端)**：提供配对服务，验证客户端的 PIN 码和证书
- **后端管理系统**：在集中管理模式下负责生成和分发 PIN 码

### 通信协议
- **HTTP/HTTPS**：用于配对流程的网络通信
- **端口**：HTTP (47989)，HTTPS (47984)
- **协议**：基于 NVIDIA GameStream 协议 v7.1.431.-1

## 详细配对步骤

### 1. 设备发现 (Device Discovery)

**Moonlight-Qt 端** (computermanager.cpp:362-373)
- 使用 mDNS (Bonjour) 协议广播 HTTP 服务 `_nvstream._tcp.local.`
- 发现 Sunshine 服务器并获取其 IP 地址和端口
- 发送 `serverinfo` 请求获取服务器基本信息

**Sunshine 端** (nvhttp.cpp:941-1053)
- 在 HTTP/HTTPS 端口监听 `serverinfo` 请求
- 返回服务器信息，包括：
  - 主机名、版本号、唯一 ID
  - HTTPS 端口、配对状态
  - 支持的编码格式 (H.264/HEVC/AV1)
  - 支持的显示模式

### 2. PIN 码获取

在标准流程中，用户在 Sunshine 服务器上查看 PIN 码（4位数字）并输入到 Moonlight 客户端。

**当前项目特殊实现** (nvhttp.cpp:848-879)
- Sunshine 服务器通过 TCP 连接（端口 12345）向后端管理系统请求 PIN 码
- 管理系统返回预先生成的 PIN 码
- 实现了 `sendPin()` 函数自动触发 PIN 码验证流程

### 3. 配对初始化

**Moonlight-Qt 端** (nvpairingmanager.cpp:225-246)
1. 生成 16 字节随机盐 (salt)
2. 将 PIN 码与盐组合：`salted_pin = salt + PIN`
3. 使用 SHA-256 哈希算法生成 AES 密钥 (前 16 字节)
4. 发送 `getservercert` 请求：
   ```
   GET /windows_pair?phrase=getservercert&salt=<salt_hex>&clientcert=<client_cert_hex>
   ```

**Sunshine 端** (nvhttp.cpp:314-333)
1. 接收客户端证书和盐值
2. 验证盐值长度 (至少 32 字符)
3. 使用 `crypto::gen_aes_key()` 生成 AES 密钥：
   - 组合：`salt + PIN`
   - SHA-256 哈希
   - 取前 16 字节作为 AES-128 密钥
4. 返回服务器证书（plaincert）

### 4. 客户端挑战 (Client Challenge)

**Moonlight-Qt 端** (nvpairingmanager.cpp:264-277)
1. 生成 16 字节随机挑战值 (randomChallenge)
2. 使用 AES-128-ECB 加密挑战值
3. 发送 `clientchallenge` 请求：
   ```
   GET /windows_pair?clientchallenge=<encrypted_challenge_hex>
   ```

**Sunshine 端** (nvhttp.cpp:355-389)
1. 解密客户端挑战值
2. 生成 16 字节服务器密钥 (serversecret)
3. 构建响应数据：
   ```
   challengeResponse = hash + server_signature + server_secret
   ```
4. 计算该数据的 SHA-256 哈希
5. 生成 16 字节服务器挑战值 (serverchallenge)
6. 使用 AES-128-ECB 加密响应并返回

### 5. 服务器挑战响应 (Server Challenge Response)

**Moonlight-Qt 端** (nvpairingmanager.cpp:279-312)
1. 解密服务器挑战响应
2. 提取服务器响应哈希 (前 32/20 字节，取决于协议版本)
3. 构建挑战响应数据：
   ```
   challengeResponse = server_challenge + client_cert_signature + client_secret
   ```
4. 计算 SHA-256/SHA-1 哈希
5. 使用 AES-128-ECB 加密并发送

**Sunshine 端** (nvhttp.cpp:334-353)
1. 解密客户端响应
2. 提取客户端哈希值
3. 签名服务器密钥
4. 返回 `pairingsecret` (serversecret + signature)

### 6. 验证和完成配对

**Moonlight-Qt 端** (nvpairingmanager.cpp:314-368)
1. 接收服务器密钥和签名
2. 验证服务器签名（防止中间人攻击）
3. 验证服务器响应哈希是否正确
4. 如果不正确，返回 `PIN_WRONG` 状态
5. 签名客户端密钥并发送
6. 发送最终 `pairchallenge` HTTPS 请求
7. 返回 `PAIRED` 状态

**Sunshine 端** (nvhttp.cpp:391-430)
1. 验证客户端密钥签名
2. 验证客户端哈希值
3. 将客户端证书添加到信任列表
4. 保存配对状态到文件
5. 清除会话数据

## 核心组件分析

### Moonlight-Qt 端

#### NvPairingManager 类 (nvpairingmanager.h/cpp)
负责整个配对流程的执行。

**主要方法**:
- `pair()`: 主配对函数，执行完整的 5 阶段配对流程
- `generateRandomBytes()`: 生成随机数
- `encrypt()/decrypt()`: AES-128-ECB 加密/解密
- `verifySignature()`: 验证 RSA 签名
- `signMessage()`: 签名消息
- `saltPin()`: 组合盐和 PIN 码

#### ComputerManager 类 (computermanager.cpp)
负责服务器管理和配对任务调度。

**关键逻辑**:
- `PendingPairingTask`: 在后台线程执行配对
- `pairHost()`: 启动配对任务
- `generatePinString()`: 获取目标 PIN 码（从配置文件）

### Sunshine 端

#### nvhttp 命名空间 (nvhttp.cpp/h)
提供 HTTP/HTTPS 服务器和配对功能。

**核心函数**:
- `pair()`: 处理配对请求的主函数
- `pin()`: 验证 PIN 码并响应
- `getservercert()`: 处理获取服务器证书请求
- `clientchallenge()`: 处理客户端挑战
- `serverchallengeresp()`: 处理服务器挑战响应
- `clientpairingsecret()`: 处理客户端密钥

#### crypto 命名空间 (crypto.cpp/h)
提供加密操作支持。

**关键功能**:
- `gen_aes_key()`: 从盐和 PIN 生成 AES 密钥
- `hash()`: SHA-256 哈希
- `sign256()/verify256()`: RSA 签名和验证
- `cipher::ecb_t`: AES-128-ECB 加密/解密

## 安全机制

### 1. PIN 码保护
- 4 位数字 PIN 码（理论上 10,000 种组合）
- PIN 码与随机盐组合后哈希，防止彩虹表攻击
- 每个配对会话使用不同的盐值

### 2. 证书交换
- 双向 X.509 证书交换
- 客户端证书用于后续连接的 SSL 验证
- 证书存储在服务端的信任列表中

### 3. 挑战-响应机制
- 双向随机挑战值，防止重放攻击
- 挑战值使用 AES 加密传输
- SHA-256/SHA-1 哈希验证响应

### 4. 数字签名
- RSA-2048/4096 签名验证
- 防止中间人攻击
- 验证通信双方的身份

### 5. 传输加密
- AES-128-ECB 加密敏感数据
- HTTPS 连接保护后续通信
- 证书固定（Certificate Pinning）

## 关键代码位置

### Moonlight-Qt
1. **配对管理器**: `moonlight-qt/app/backend/nvpairingmanager.cpp:204-369`
2. **计算机管理**: `moonlight-qt/app/backend/computermanager.cpp:564-629`
3. **PIN 码获取**: `moonlight-qt/app/backend/computermanager.cpp:997-1006`

### Sunshine
1. **配对处理**: `Sunshine-0.21.0/src/nvhttp.cpp:591-678`
2. **PIN 验证**: `Sunshine-0.21.0/src/nvhttp.cpp:893-929`
3. **获取服务器证书**: `Sunshine-0.21.0/src/nvhttp.cpp:315-333`
4. **客户端挑战**: `Sunshine-0.21.0/src/nvhttp.cpp:355-389`
5. **服务器挑战响应**: `Sunshine-0.21.0/src/nvhttp.cpp:334-353`
6. **客户端密钥验证**: `Sunshine-0.21.0/src/nvhttp.cpp:391-430`
7. **AES 密钥生成**: `Sunshine-0.21.0/src/crypto.cpp:310-325`

### 配置文件
1. **Moonlight 配置**: `conf.ini` (serverIp 字段)
2. **Sunshine 配置**: `conf_sunshine.ini` (服务器 IP)

## 总结

Moonlight-Qt 与 Sunshine 的配对流程是一个安全而复杂的双向认证过程，主要特点包括：

### 流程特点
1. **5 阶段认证**: 包含证书交换、双向挑战-响应、PIN 验证等多个步骤
2. **强加密保护**: 使用 AES-128、SHA-256、RSA 等现代加密算法
3. **双向认证**: 客户端和服务器互相验证对方的身份
4. **防攻击机制**: 包含防重放、防中间人、防暴力破解等多种安全机制

### 项目特殊实现
在当前项目中，配对流程有以下特殊实现：

1. **自动化 PIN 码获取**: 通过 TCP 连接从管理系统自动获取 PIN 码，而非手动输入
2. **集中管理**: 后端管理系统负责生成和维护设备 PIN 码
3. **简化配置**: 使用配置文件存储服务器 IP 地址，便于部署

### 应用场景
该配对机制适用于：
- 远程游戏串流服务
- 企业级桌面虚拟化
- 需要高安全性的远程访问场景

整个配对过程在确保安全性的同时，通过自动化和集中管理提高了部署和使用的便利性。