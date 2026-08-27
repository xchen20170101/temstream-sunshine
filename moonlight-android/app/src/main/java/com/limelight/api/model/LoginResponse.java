package com.limelight.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录响应数据模型
 * 响应格式：
 * {
 *     "code": 0,
 *     "message": "登录成功",
 *     "data": {
 *         "user_id": "123456789",
 *         "value": "some_value",
 *         "token": "access_token_string"
 *     }
 * }
 */
public class LoginResponse {
    private int code;
    private String message;
    private LoginData data;

    /**
     * 是否成功（code == 0）
     */
    public boolean isSuccess() {
        return code == 0;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LoginData getData() {
        return data;
    }

    public void setData(LoginData data) {
        this.data = data;
    }

    /**
     * 获取过期时间（暂时返回一个默认值，7天后过期）
     */
    public long getExpiry() {
        return System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
    }

    /**
     * 登录数据内部类
     */
    public static class LoginData {
        @SerializedName("user_id")
        private String userId;
        
        private String value;
        private String token;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
