package com.limelight.api.model;

import java.util.List;

/**
 * 客户端登录响应数据模型
 * 对应后端 /api/stream/v1/client/login 接口响应
 * 响应格式：
 * {
 *     "code": 0,
 *     "data": {
 *         "userName": "testuser",
 *         "devices": [
 *             {
 *                 "deviceId": "xxx",
 *                 "deviceName": "xxx",
 *                 "ip": "192.168.1.100",
 *                 "pin": "123456",
 *                 "authKey": "xxx"
 *             }
 *         ]
 *     },
 *     "msg": "登录成功"
 * }
 */
public class ClientLoginResponse {
    private int code;
    private String msg;
    private ClientLoginData data;

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

    public ClientLoginData getData() {
        return data;
    }

    public void setData(ClientLoginData data) {
        this.data = data;
    }

    /**
     * 客户端登录数据内部类
     */
    public static class ClientLoginData {
        private String userName;
        private List<DeviceInfo> devices;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public List<DeviceInfo> getDevices() {
            return devices;
        }

        public void setDevices(List<DeviceInfo> devices) {
            this.devices = devices;
        }
    }

    /**
     * 设备信息内部类
     */
    public static class DeviceInfo {
        private String deviceId;
        private String deviceName;
        private String ip;
        private String pin;
        private String authKey;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public String getAuthKey() {
            return authKey;
        }

        public void setAuthKey(String authKey) {
            this.authKey = authKey;
        }
    }
}
