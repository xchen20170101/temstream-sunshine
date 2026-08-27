package com.limelight.api.model;

/**
 * 客户端登录请求数据模型
 * 对应 api.md 中的 /api/stream/v1/client/login 接口
 * 请求格式：
 * {
 *     "username": "testuser",
 *     "password": "123456"
 * }
 */
public class ClientLoginRequest {
    private String username;
    private String password;

    public ClientLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
