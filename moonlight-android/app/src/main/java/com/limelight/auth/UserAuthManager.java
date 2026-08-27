package com.limelight.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 用户认证管理类
 * 局域网环境：仅保留用户名/token/记住我状态，密码不再落地到本地
 */
public class UserAuthManager {
    private static final String PREF_NAME = "user_auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EXPIRY = "expiry";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_DEVICE_DIRECT_LOGIN = "device_direct_login"; // 设备直连登录标记
    private static final String KEY_LAN_MODE_LOGIN = "lan_mode_login"; // 局域网模式登录标记
    private static final String KEY_LAN_SERVER_IP = "lan_server_ip"; // 局域网模式服务器IP
    private static final String KEY_LAN_PASSWORD = "lan_password"; // 局域网模式访问密码

    private static UserAuthManager instance;
    private SharedPreferences preferences;

    private UserAuthManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UserAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserAuthManager(context);
        }
        return instance;
    }

    /**
     * 保存登录信息
     * 局域网环境：password 参数不再写入本地，仅保留用户名/token/记住我状态
     */
    public void saveLoginInfo(String username, String password, String token, long expiry, boolean rememberMe) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_EXPIRY, expiry);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);

        // 兼容旧版本残留：始终清除本地可能存在的旧密码
        editor.remove(KEY_PASSWORD);

        // 只有勾选"记住我"时才保存用户名
        if (rememberMe) {
            editor.putString(KEY_USERNAME, username);
        } else {
            // 未勾选时清除已保存的用户名
            editor.remove(KEY_USERNAME);
        }

        editor.commit(); // 使用同步保存，确保数据立即写入，避免跳转到PcView时数据未保存完成
    }

    /**
     * 获取存储的Token
     */
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    /**
     * 获取存储的用户名
     */
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    /**
     * 获取Token过期时间
     */
    public long getTokenExpiry() {
        return preferences.getLong(KEY_EXPIRY, 0);
    }

    /**
     * 是否记住登录状态
     */
    public boolean isRememberMe() {
        return preferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * 检查是否已登录且Token有效
     */
    public boolean isLoggedIn() {
        String token = getToken();
        if (TextUtils.isEmpty(token)) {
            return false;
        }

        // 检查Token是否过期
        long expiry = getTokenExpiry();
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            // Token已过期，清除登录信息
            clearLoginInfo();
            return false;
        }

        return true;
    }

    /**
     * 获取Authorization头部值
     */
    public String getAuthorizationHeader() {
        String token = getToken();
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        return "Bearer " + token;
    }

    /**
     * 清除登录信息（保留记住的用户名）
     */
    public void clearLoginInfo() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_EXPIRY);
        // 清除可能存在的旧密码字段
        editor.remove(KEY_PASSWORD);
        // 注意：不清除 KEY_USERNAME 和 KEY_REMEMBER_ME，以便下次登录时仍然记住用户名
        editor.commit(); // 使用同步保存，确保数据立即清除
    }

    /**
     * 清除所有信息（包括记住的用户名）
     */
    public void clearAllInfo() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_EXPIRY);
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_DEVICE_DIRECT_LOGIN);
        editor.remove(KEY_LAN_MODE_LOGIN);
        editor.remove(KEY_LAN_SERVER_IP);
        editor.commit(); // 使用同步保存，确保数据立即清除
    }

    /**
     * 登出
     */
    public void logout() {
        clearLoginInfo();
    }

    /**
     * 保存设备直连登录状态
     * 设备直连模式不使用token，使用专用标记来标识已验证
     */
    public void saveDeviceDirectLogin(String deviceId, long expiry) {
        SharedPreferences.Editor editor = preferences.edit();
        // 使用设备ID作为临时token，expiry作为过期时间
        editor.putString(KEY_TOKEN, "DEVICE_DIRECT:" + deviceId);
        editor.putLong(KEY_EXPIRY, expiry);
        editor.putBoolean(KEY_DEVICE_DIRECT_LOGIN, true);
        editor.commit();
    }

    /**
     * 检查是否为设备直连登录
     */
    public boolean isDeviceDirectLogin() {
        return preferences.getBoolean(KEY_DEVICE_DIRECT_LOGIN, false);
    }

    /**
     * 清除设备直连登录状态
     */
    public void clearDeviceDirectLogin() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DEVICE_DIRECT_LOGIN);
        editor.commit();
    }

    /**
     * 保存局域网模式登录状态
     * @param serverIp 服务器 IP 地址
     * @param password 访问密码（用于 Sunshine 配对验证）
     * @param expiry 过期时间戳
     */
    public void saveLanModeLogin(String serverIp, String password, long expiry) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN, "LAN_MODE:" + serverIp);
        editor.putLong(KEY_EXPIRY, expiry);
        editor.putBoolean(KEY_LAN_MODE_LOGIN, true);
        editor.putString(KEY_LAN_SERVER_IP, serverIp);
        editor.putString(KEY_LAN_PASSWORD, password);
        editor.commit();
    }

    /**
     * 检查是否为局域网模式登录
     */
    public boolean isLanModeLogin() {
        return preferences.getBoolean(KEY_LAN_MODE_LOGIN, false);
    }

    /**
     * 获取局域网模式服务器 IP
     */
    public String getLanServerIp() {
        return preferences.getString(KEY_LAN_SERVER_IP, null);
    }

    /**
     * 获取局域网模式访问密码
     */
    public String getLanPassword() {
        return preferences.getString(KEY_LAN_PASSWORD, null);
    }

    /**
     * 清除局域网模式登录状态
     */
    public void clearLanModeLogin() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_LAN_MODE_LOGIN);
        editor.remove(KEY_LAN_SERVER_IP);
        editor.remove(KEY_LAN_PASSWORD);
        editor.commit();
    }
}
