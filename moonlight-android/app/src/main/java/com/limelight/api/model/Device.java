package com.limelight.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备数据模型
 */
public class Device {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("ip")
    private String ip;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("mac")
    private String mac;
    
    @SerializedName("port")
    private int port = 47989;
    
    @SerializedName("uuid")
    private String uuid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isOnline() {
        return "online".equalsIgnoreCase(status);
    }
    
    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", ip='" + ip + '\'' +
                ", type='" + type + '\'' +
                ", mac='" + mac + '\'' +
                ", port=" + port +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
