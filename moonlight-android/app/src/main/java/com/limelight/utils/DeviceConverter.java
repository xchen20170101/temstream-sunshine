package com.limelight.utils;

import com.limelight.api.model.Device;
import com.limelight.nvstream.http.ComputerDetails;

/**
 * 设备数据转换工具类
 * 将云端API的Device对象转换为应用内部的ComputerDetails对象
 */
public class DeviceConverter {
    
    /**
     * 将Device转换为ComputerDetails
     */
    public static ComputerDetails deviceToComputerDetails(Device device) {
        if (device == null) {
            return null;
        }
        
        ComputerDetails details = new ComputerDetails();
        
        // 基本信息
        details.name = device.getName();
        details.uuid = device.getUuid() != null ? device.getUuid() : device.getId();
        details.macAddress = device.getMac();
        
        // 状态信息
        if (device.isOnline()) {
            details.state = ComputerDetails.State.ONLINE;
        } else {
            details.state = ComputerDetails.State.OFFLINE;
        }
        
        // 地址信息
        if (device.getIp() != null && !device.getIp().isEmpty()) {
            int port = device.getPort() > 0 ? device.getPort() : 47989;
            details.manualAddress = new ComputerDetails.AddressTuple(device.getIp(), port);
            details.localAddress = new ComputerDetails.AddressTuple(device.getIp(), port);
            
            // 设置当前活动地址
            if (device.isOnline()) {
                details.activeAddress = details.manualAddress;
            }
        }
        
        // 云端设备默认已配对（不需要PIN码配对）
        details.pairState = com.limelight.nvstream.http.PairingManager.PairState.PAIRED;
        
        // HTTPS端口设置
        details.httpsPort = 47984; // Moonlight默认HTTPS端口
        
        return details;
    }
    
    /**
     * 批量转换设备列表
     */
    public static java.util.List<ComputerDetails> devicesToComputerDetails(java.util.List<Device> devices) {
        if (devices == null) {
            return new java.util.ArrayList<>();
        }
        
        java.util.List<ComputerDetails> computerDetailsList = new java.util.ArrayList<>();
        for (Device device : devices) {
            ComputerDetails details = deviceToComputerDetails(device);
            if (details != null) {
                computerDetailsList.add(details);
            }
        }
        
        return computerDetailsList;
    }
}

