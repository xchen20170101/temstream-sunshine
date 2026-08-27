# RESTful API 使用文档

本文档详细说明了Hyper-V VDI Linux客户端中使用的所有RESTful API接口。

## 目录

1. [通用说明](#通用说明)
2. [用户登录接口](#1-用户登录接口)
3. [获取用户配置接口](#2-获取用户配置接口)
4. [检查虚拟机密码接口](#3-检查虚拟机密码接口)
5. [获取设备列表接口](#4-获取设备列表接口)
6. [修改密码接口](#5-修改密码接口)
7. [虚拟机操作接口](#6-虚拟机操作接口)

## 通用说明

### 基础信息
- **协议**: HTTPS
- **证书验证**: 已跳过 (`InsecureSkipVerify: true`)
- **请求超时**: 10-60秒（根据操作类型而定）
- **认证方式**: 基于Cookie的userId和accessToken

### 通用请求头
```http
Referer: https://{serverIP}
```

### 认证Cookie格式
```http
Cookie: userId={userID}; accessToken={accessToken}
```

---

## 1. 用户登录接口

### 接口描述
用户登录验证接口，获取用户ID和访问令牌。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/login`
- **方法**: `POST`
- **Content-Type**: `multipart/form-data`

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

### 请求示例
```http
POST https://192.168.1.100/api/cloud/v1/login
Content-Type: multipart/form-data
Referer: https://192.168.1.100

username=admin&password=123456
```

### 响应格式
```json
{
    "code": 0,
    "message": "登录成功",
    "data": {
        "user_id": "123456789",
        "value": "some_value",
        "token": "access_token_string"
    }
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| message | string | 响应消息 |
| data.user_id | string | 用户ID |
| data.value | string | 其他值信息 |
| data.token | string | 访问令牌 |

### 错误响应示例
```json
{
    "code": 1001,
    "message": "用户名或密码错误",
    "data": null
}
```

### 使用说明
1. 此接口不需要认证
2. 成功登录后获取的`user_id`和`token`用于后续接口的认证
3. 登录成功后，服务器可能会设置额外的Cookie

---

## 2. 获取用户配置接口

### 接口描述
获取已登录用户的配置信息。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/user_profile`
- **方法**: `GET`
- **认证**: 需要Cookie认证

### 请求参数
无URL参数，通过Cookie传递认证信息。

### 请求示例
```http
GET https://192.168.1.100/api/cloud/v1/user_profile
Referer: https://192.168.1.100
Cookie: userId=123456789; accessToken=access_token_string
```

### 响应格式
```json
{
    "code": 0,
    "msg": "获取成功",
    "data": {
        "username": "admin",
        "password": "encrypted_password",
        "email": "admin@example.com",
        "phone": "13800138000"
    }
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| msg | string | 响应消息 |
| data.username | string | 用户名 |
| data.password | string | 加密后的密码 |
| data.email | string | 邮箱地址 |
| data.phone | string | 手机号码 |

### 错误响应示例
```json
{
    "code": 1002,
    "msg": "认证失败",
    "data": null
}
```

### 使用说明
1. 必须先通过登录接口获取认证信息
2. 需要在Cookie中提供有效的userId和accessToken

---

## 3. 检查虚拟机密码接口

### 接口描述
检查指定虚拟机的用户名和密码是否正确。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/check_vm_pwd`
- **方法**: `POST`
- **Content-Type**: `multipart/form-data`
- **认证**: 需要Cookie认证

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vmname | string | 是 | 虚拟机名称 |
| username | string | 是 | 虚拟机内的用户名 |
| userpwd | string | 是 | 虚拟机内的用户密码 |

### 请求示例
```http
POST https://192.168.1.100/api/cloud/v1/check_vm_pwd
Content-Type: multipart/form-data
Referer: https://192.168.1.100
Cookie: userId=123456789; accessToken=access_token_string

vmname=WIN-TEST&username=administrator&userpwd=password123
```

### 响应格式
```json
{
    "code": 0,
    "msg": "验证成功",
    "data": {
        "valid": true
    }
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| msg | string | 响应消息 |
| data.valid | boolean | 密码是否有效 |

### 错误响应示例
```json
{
    "code": 1003,
    "msg": "密码验证失败",
    "data": {
        "valid": false
    }
}
```

### 使用说明
1. 需要有效的用户认证
2. 用于在远程连接前验证虚拟机的登录凭据
3. 建议在建立RDP连接前调用此接口验证

---

## 4. 获取设备列表接口

### 接口描述
获取用户可访问的虚拟机设备列表。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/devices?count=100&index=1`
- **方法**: `GET`
- **认证**: 需要Cookie认证

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| count | int | 否 | 每页数量，默认100 |
| index | int | 否 | 页码，从1开始 |

### 请求示例
```http
GET https://192.168.1.100/api/cloud/v1/devices?count=100&index=1
Referer: https://192.168.1.100
Cookie: userId=123456789; accessToken=access_token_string
```

### 响应格式
```json
{
    "code": 0,
    "msg": "获取成功",
    "data": {
        "devices": [
            {
                "id": "vm-001",
                "name": "Windows 10 Desktop",
                "username": "administrator",
                "virtualIp": "192.168.100.10",
                "templateInfo": "Windows 10 Enterprise",
                "status": "running",
                "createdTime": "2023-10-01 10:00:00",
                "memoryInfo": "8GB",
                "cpuInfo": "4 vCPUs",
                "gpuInfo": "Virtual GPU"
            }
        ],
        "totalNum": 1
    }
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| msg | string | 响应消息 |
| data.devices | array | 设备列表 |
| data.totalNum | int | 设备总数 |

#### 设备信息字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | string | 虚拟机ID |
| name | string | 虚拟机名称 |
| username | string | 默认用户名 |
| virtualIp | string | 虚拟机IP地址 |
| templateInfo | string | 模板信息 |
| status | string | 运行状态 |
| createdTime | string | 创建时间 |
| memoryInfo | string | 内存信息 |
| cpuInfo | string | CPU信息 |
| gpuInfo | string | GPU信息 |

### 设备状态说明
- `running`: 运行中
- `stopped`: 已停止
- `starting`: 启动中
- `stopping`: 停止中
- `restarting`: 重启中

### 使用说明
1. 需要有效的用户认证
2. 返回用户有权限访问的所有虚拟机
3. 支持分页查询

---

## 5. 修改密码接口

### 接口描述
修改用户登录密码。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/reset_password`
- **方法**: `POST`
- **Content-Type**: `multipart/form-data`

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| oldpassword | string | 是 | 原密码 |
| newpassword | string | 是 | 新密码 |

### 请求示例
```http
POST https://192.168.1.100/api/cloud/v1/reset_password
Content-Type: multipart/form-data
Referer: https://192.168.1.100

username=admin&oldpassword=old123456&newpassword=new123456
```

### 响应格式
```json
{
    "code": 0,
    "msg": "密码修改成功",
    "data": "操作完成"
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| msg | string | 响应消息 |
| data | string | 操作结果描述 |

### 错误响应示例
```json
{
    "code": 1004,
    "msg": "原密码错误",
    "data": null
}
```

### 使用说明
1. 不需要认证Cookie，通过用户名和原密码验证
2. 新密码不能与原密码相同
3. 建议在密码修改成功后重新登录

---

## 6. 虚拟机操作接口

### 接口描述
对指定虚拟机执行操作（开机、关机、重启）。

### 请求信息
- **URL**: `https://{serverIP}/api/cloud/v1/vm/operate`
- **方法**: `POST`
- **Content-Type**: `multipart/form-data`
- **认证**: 需要Cookie认证
- **超时**: 30-60秒（根据操作类型）

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| vm_id | string | 是 | 虚拟机ID |
| action | string | 是 | 操作类型：1-开机，2-关机，3-重启 |

### 请求示例
```http
POST https://192.168.1.100/api/cloud/v1/vm/operate
Content-Type: multipart/form-data
Referer: https://192.168.1.100
Cookie: userId=123456789; accessToken=access_token_string

vm_id=vm-001&action=1
```

### 响应格式
```json
{
    "code": 0,
    "msg": "操作成功",
    "data": {
        "result": true
    }
}
```

### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | int | 响应码，0表示成功 |
| msg | string | 响应消息 |
| data.result | boolean | 操作是否成功 |

### 操作类型说明
| 值 | 操作 | 说明 |
|----|------|------|
| 1 | 开机 | 启动虚拟机 |
| 2 | 关机 | 关闭虚拟机 |
| 3 | 重启 | 重启虚拟机 |

### 错误响应示例
```json
{
    "code": 1005,
    "msg": "虚拟机不存在或无权限",
    "data": {
        "result": false
    }
}
```

### 使用说明
1. 需要有效的用户认证
2. 关机和重启操作超时时间为60秒，其他操作为30秒
3. 如果操作超时但是关机/重启操作，可能实际上已经成功
4. 建议操作后查询设备列表确认状态变更

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001 | 登录失败 |
| 1002 | 认证失败 |
| 1003 | 密码验证失败 |
| 1004 | 密码修改失败 |
| 1005 | 虚拟机操作失败 |

## 使用流程示例

### 完整操作流程
1. **用户登录** → 获取userId和accessToken
2. **获取设备列表** → 查看可用虚拟机
3. **检查虚拟机密码**（可选）→ 验证连接凭据
4. **虚拟机操作**（可选）→ 开机/关机/重启
5. **获取用户配置**（可选）→ 查看用户信息
6. **修改密码**（可选）→ 更新登录密码

### 注意事项
1. 所有接口都使用HTTPS协议
2. 证书验证已跳过，适用于内网环境
3. 认证信息通过Cookie传递
4. POST请求使用multipart/form-data格式
5. 建议实现适当的错误处理和重试机制
