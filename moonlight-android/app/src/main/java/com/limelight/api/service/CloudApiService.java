package com.limelight.api.service;

import com.limelight.api.model.ApiResponse;
import com.limelight.api.model.ClientLoginRequest;
import com.limelight.api.model.ClientLoginResponse;
import com.limelight.api.model.DeviceListResponse;
import com.limelight.api.model.DeviceStatusResponse;
import com.limelight.api.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 云端API服务接口
 */
public interface CloudApiService {
    
    /**
     * 客户端登录（获取主机访问信息）
     * 对应 api.md 中的 /api/stream/v1/client/login 接口
     * Content-Type: application/json
     */
    @POST("api/stream/v1/client/login")
    Call<ClientLoginResponse> clientLogin(@Body ClientLoginRequest request);
    
    /**
     * 用户登录（旧接口，保留兼容性）
     * Content-Type: multipart/form-data
     */
    @FormUrlEncoded
    @POST("api/cloud/v1/login")
    Call<LoginResponse> login(
            @Field("username") String username,
            @Field("password") String password
    );
    
    /**
     * 获取设备列表
     * 需要Cookie认证（userId和accessToken）
     */
    @GET("api/cloud/v1/devices")
    Call<DeviceListResponse> getDevices(
            @Query("count") int count,
            @Query("index") int index
    );
    
    @GET("api/cloud/v1/user/devices")
    Call<DeviceListResponse> getUserDevices();
    
    /**
     * 虚拟机操作（开机/关机/重启）
     * Content-Type: multipart/form-data
     * 需要Cookie认证
     */
    @FormUrlEncoded
    @POST("api/cloud/v1/vm/operate")
    Call<ApiResponse> vmOperate(
            @Field("vm_id") String vmId,
            @Field("action") String action
    );
    
    /**
     * 修改密码（对应 api.md 中的 /api/stream/v1/reset_password 接口）
     * Content-Type: application/x-www-form-urlencoded
     */
    @FormUrlEncoded
    @POST("api/stream/v1/reset_password")
    Call<ApiResponse> resetPassword(
            @Field("username") String username,
            @Field("oldpassword") String oldPassword,
            @Field("newpassword") String newPassword
    );
    
    /**
     * 检查虚拟机密码
     * Content-Type: multipart/form-data
     * 需要Cookie认证
     */
    @FormUrlEncoded
    @POST("api/cloud/v1/check_vm_pwd")
    Call<ApiResponse> checkVmPassword(
            @Field("vmname") String vmName,
            @Field("username") String username,
            @Field("userpwd") String userPassword
    );
    
    /**
     * 获取用户配置
     * 需要Cookie认证
     */
    @GET("api/cloud/v1/user_profile")
    Call<ApiResponse> getUserProfile();
    
    /**
     * 测试连接
     */
    @GET("api/cloud/v1/ping")
    Call<ApiResponse> ping();

    /**
     * 查询设备状态（用于设备直连模式）
     * GET /api/stream/v1/devices/{deviceId}/status?device_password=xxx
     */
    @GET("api/stream/v1/devices/{deviceId}/status")
    Call<DeviceStatusResponse> getDeviceStatus(
            @retrofit2.http.Path("deviceId") String deviceId,
            @Query("device_password") String devicePassword
    );
}
