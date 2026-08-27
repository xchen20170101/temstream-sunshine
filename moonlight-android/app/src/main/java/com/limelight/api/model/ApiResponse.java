package com.limelight.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 通用API响应数据模型
 * 响应格式：
 * {
 *     "code": 0,
 *     "msg": "操作成功",
 *     "data": {...}
 * }
 */
public class ApiResponse {
    private int code;
    
    @SerializedName("msg")
    private String message;
    
    private Object data;

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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
