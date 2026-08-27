# 客户端对接 API 文档

## 1. 概述

- **Base URL 前缀**：`/api/stream/v1`
- 所有接口返回统一结构：

```json
{
  "code": 0,
  "data": ...,
  "msg": "查询成功"
}
```

- 字段说明：
  - `code`:  
    - `0` 表示成功  
    - `2` 表示失败
  - `data`: 业务数据，类型由不同接口决定
  - `msg`: 提示信息或错误码字符串（例如 `"User.PasswordIsWrong"`、`"User.NoBindDevice"` 等）

> 客户端应主要根据 `code` 判断成功/失败，根据 `msg` 做错误码映射/提示。

---

## 2. 认证与会话说明

- Gin 中间件放行以下接口，不需要 Cookie/token：
  - `POST /api/stream/v1/login`
  - `POST /api/stream/v1/reset_password`
  - `POST /api/stream/v1/client/login`
- **其他接口默认需要 Cookie 鉴权**：
  - 请求头中需携带服务器之前设置的 `Cookie`：包含 `accessToken` 和 `userId`
  - 认证失败时会返回 HTTP 401（中间件 `cookieCheckMid` 截断）

对“纯客户端对接”的场景，一般使用：

- `POST /client/login` 获取云桌面 IP + PIN
- `POST /reset_password` 由 Android 客户端重置密码  
（是否使用授权接口可按业务需要选择）

---

## 3. 客户端登录获取云桌面 IP + PIN

### 3.1 客户端登录（获取云桌面访问信息）

- **URL**：`/api/stream/v1/client/login`
- **Method**：`POST`
- **是否需要认证**：否（免 Cookie）
- **Content-Type**：`application/json`

#### 3.1.1 请求体

```json
{
  "username": "testuser",
  "password": "123456"
}
```

- 字段说明（来自 `ClientLoginRequest`）：
  - `username`：用户名（字符串）
  - `password`：密码（字符串）

#### 3.1.2 业务逻辑（简要）

1. 根据 `username` 从 `User` 表中查找用户。
2. 校验密码是否匹配：
   - 不匹配 → 返回错误 `User.PasswordIsWrong`。
3. 校验用户状态：
   - `Status != "启用"` → 返回错误 `User.Disable`。
4. 在 `Bind` 表中查找该用户是否绑定了设备：
   - 无绑定记录 → 返回错误 `User.NoBindDevice`。
5. 根据绑定中的 `deviceId` 查找 `Device`：
   - 未找到设备或 `device.Ip` 为空 → `User.NoBindDevice`。
6. 返回绑定设备的 IP 和 PIN。

#### 3.1.3 响应数据格式

成功示例（`code = 0`）：

```json
{
  "code": 0,
  "data": {
    "userName": "testuser",
    "ip": "192.168.1.100",
    "pin": "123456"
  },
  "msg": "登录成功"
}
```

- 字段说明（`ClientLoginResponse`）：
  - `userName`：用户名称
  - `ip`：绑定设备的 IP 地址
  - `pin`：设备 PIN 码（用于后续客户端与设备的认证）

失败示例：

```json
{
  "code": 2,
  "data": {},
  "msg": "User.PasswordIsWrong"
}
```

可能的 `msg` 值（错误码）：

- `"User.PasswordIsWrong"`：用户名存在但密码不正确
- `"User.Disable"`：用户被禁用
- `"User.NoBindDevice"`：用户未绑定任何设备或设备信息无效
- `"Common.InvalidParam"`：参数绑定失败（一般为 JSON 结构不符合要求）

---

## 4. Android 客户端密码重置

### 4.1 重置密码（Android 客户端）

- **URL**：`/api/stream/v1/reset_password`
- **Method**：`POST`
- **是否需要认证**：否（免 Cookie）
- **Content-Type**：`application/x-www-form-urlencoded`

#### 4.1.1 请求参数（form）

| 字段名       | 位置   | 类型   | 必选 | 说明            |
| ------------ | ------ | ------ | ---- | --------------- |
| `username`   | form   | string | 是   | 用户名          |
| `oldpassword`| form   | string | 是   | 原密码          |
| `newpassword`| form   | string | 是   | 新密码          |

示例请求（表单）：

```text
POST /api/stream/v1/reset_password
Content-Type: application/x-www-form-urlencoded

username=testuser&oldpassword=123456&newpassword=654321
```

#### 4.1.2 业务逻辑（简要）

1. 根据 `username` 查找用户：
   - 不存在 → `"User.NotExist"`
2. 校验用户状态：
   - 非 `"启用"` → `"User.Disable"`
3. 校验原密码是否正确：
   - 不匹配 → `"User.PasswordIsWrong"`
4. 更新用户密码为 `newpassword`。

#### 4.1.3 响应数据格式

成功示例：

```json
{
  "code": 0,
  "data": "testuser",
  "msg": "查询成功"
}
```

- `data`：成功重置密码的用户名

失败示例：

```json
{
  "code": 2,
  "data": {},
  "msg": "User.PasswordIsWrong"
}
```

可能的错误码：

- `"User.NotExist"`：用户不存在
- `"User.Disable"`：用户状态不是启用
- `"User.PasswordIsWrong"`：原密码错误

---

## 5.（可选）授权相关接口

视客户端是否需要直接参与授权激活/查询而定。如果授权主要通过 Web 管理端操作，客户端可以不调用这一组接口。

### 5.1 获取授权状态

- **URL**：`/api/stream/v1/licenses`
- **Method**：`GET`
- **是否需要认证**：是（需要 Cookie 中 `accessToken`、`userId`）
- **用途**：获取当前机器的授权校验结果（是否已授权/是否过期）。

#### 5.1.1 响应数据格式

成功示例：

```json
{
  "code": 0,
  "data": {
    "machineCode": "ABCDE-12345-XYZ",
    "isChecked": true
  },
  "msg": "查询成功"
}
```

- 字段说明（`GetUserLicenseResponse`）：
  - `machineCode`：服务器机器码
  - `isChecked`：是否授权有效（布尔值）

失败示例（授权过期等情况）：

```json
{
  "code": 2,
  "data": {},
  "msg": "User.LicenseExpired"
}
```

可能的错误码（部分）：

- `"User.LicenseExpired"`：授权已过期
- `"User.LicenseInvalid"`：授权无效（某些条件下可能会删除授权记录）

### 5.2 激活授权

- **URL**：`/api/stream/v1/licenses`
- **Method**：`POST`
- **是否需要认证**：是（需要 Cookie）
- **Content-Type**：`application/x-www-form-urlencoded`

> 通常由管理端调用，客户端一般不直接调用，如需由客户端发起可按此规范。

#### 5.2.1 请求参数（form）

| 字段名        | 位置 | 类型   | 必选 | 说明            |
| ------------- | ---- | ------ | ---- | --------------- |
| `machineCode` | form | string | 是   | 机器码          |
| `licenseCode` | form | string | 是   | 授权码          |

逻辑概要：

1. 校验 `form.MachineCode` 是否与 `utils.GetMachineCode()` 一致。
2. 检查授权码是否已存在。
3. 使用 `CheckLicense` 校验授权码有效性和类型（测试/正式）。
4. 写入 `License` 表，记录授权类型、激活时间、过期时间等。

#### 5.2.2 响应数据格式

成功：

```json
{
  "code": 0,
  "data": {
    "id": "...",
    "licenseCode": "...",
    "licenseType": "1",
    "activeTime": 1690000000000,
    "expiredTime": 1692592000000,
    "expireFlag": 1
  },
  "msg": "查询成功"
}
```

> 字段具体以 `License` 模型定义为准，这里只说明大致含义。

失败可能返回：

- `"User.LicenseExists"`：授权已存在
- `"User.LicenseActiveFailed"`：授权码校验失败
- `"Common.InvalidParam"`：参数不合法（机器码不匹配等）

---

## 6. 统一错误处理与客户端建议

- 所有接口失败时，**HTTP 状态码通常仍为 200**，需要根据 `code` 字段判断：
  - `code == 0`：成功
  - `code == 2`：失败
- `msg` 一般为**错误码字符串**，建议客户端做一个映射表，将错误码转换为用户可读的提示文案，例如：

  - `User.PasswordIsWrong` → “用户名或密码错误”
  - `User.NoBindDevice` → “当前账号未绑定任何云桌面”
  - `User.Disable` → “账号已被禁用，请联系管理员”
  - `User.LicenseExpired` → “授权已过期，请联系管理员续期”

---

## 7. 客户端主要调用流程建议

- **首次登录获取云桌面信息**：
  1. `POST /client/login`（JSON）：提交用户名和密码。
  2. 成功后拿到：
     - `ip`：云桌面 IP  
     - `pin`：PIN 码（用于后续连接）。
- **Android 客户端修改密码**：
  1. `POST /reset_password`（form）。
- **如客户端需要展示授权状态**（可选）：
  1. 先通过 Web 登录获取 Cookie（或在服务端中转）。  
  2. 调用 `GET /licenses` 获取授权状态。
