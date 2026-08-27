package com.limelight.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 设备列表响应数据模型
 */
public class DeviceListResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("devices")
    private List<Device> devices;
    
    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "DeviceListResponse{" +
                "success=" + success +
                ", devices=" + (devices != null ? devices.size() + " devices" : "null") +
                ", message='" + message + '\'' +
                '}';
    }
}
