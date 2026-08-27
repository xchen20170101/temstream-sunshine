package com.limelight.api.model;

/**
 * 设备操作请求数据模型
 */
public class DeviceActionRequest {
    private String action;

    public DeviceActionRequest(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // 常用操作常量
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_RESTART = "restart";
}
