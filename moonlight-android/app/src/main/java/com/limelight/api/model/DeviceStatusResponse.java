package com.limelight.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备状态响应数据模型
 * 对应后端 /api/stream/v1/devices/{deviceId}/status 接口响应
 * 响应格式：
 * {
 *     "code": 0,
 *     "data": {
 *         "deviceId": "xxx",
 *         "name": "xxx",
 *         "ip": "192.168.1.100",
 *         "port": 47984,
 *         "pin": "123456",
 *         "device_password": "xxx",
 *         "status": "Online",
 *         "paired": false
 *     },
 *     "msg": "成功"
 * }
 */
public class DeviceStatusResponse {
    private int code;
    private String msg;
    private DeviceStatusData data;

    public boolean isSuccess() {
        return code == 0;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DeviceStatusData getData() {
        return data;
    }

    public void setData(DeviceStatusData data) {
        this.data = data;
    }

    /**
     * 设备状态数据内部类
     */
    public static class DeviceStatusData {
        private String deviceId;
        private String name;
        private String ip;
        private int port;
        private String pin;
        private String devicePassword;
        private String status;
        private boolean paired;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public String getDevicePassword() {
            return devicePassword;
        }

        public void setDevicePassword(String devicePassword) {
            this.devicePassword = devicePassword;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isPaired() {
            return paired;
        }

        public void setPaired(boolean paired) {
            this.paired = paired;
        }
    }
}
