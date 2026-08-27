package com.limelight.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.limelight.api.service.CloudApiService;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API客户端管理类
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String PREF_NAME = "api_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_CLOUD_DESKTOP_IP = "cloud_desktop_ip";
    private static final String KEY_CLOUD_DESKTOP_PIN = "cloud_desktop_pin";
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1";
    
    private static ApiClient instance;
    private Retrofit retrofit;
    private CloudApiService cloudApiService;
    private Context context;
    
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        try {
            initRetrofit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Retrofit", e);
            // 初始化失败时，设置为null，后续调用时会重新尝试初始化
            retrofit = null;
            cloudApiService = null;
        }
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }
    
    private void initRetrofit() {
        try {
            // 记录当前 base URL
            String currentBaseUrl = getBaseUrl();
            Log.d(TAG, "initRetrofit: Current base URL = " + currentBaseUrl);
            
            // 创建HTTP日志拦截器
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // 创建认证拦截器
            AuthInterceptor authInterceptor = new AuthInterceptor(context);
            
            // 创建Referer拦截器
            RefererInterceptor refererInterceptor = new RefererInterceptor(this);
            
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        // 信任所有客户端证书
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        // 信任所有服务器证书
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
            };
            
            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            // 创建跳过主机名验证的HostnameVerifier
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // 信任所有主机名
                }
            };
            
            // 创建OkHttp客户端
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(refererInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build();
            
            // 创建Gson转换器
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            
            // 创建Retrofit实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            
            // 创建API服务实例
            cloudApiService = retrofit.create(CloudApiService.class);
            
            Log.d(TAG, "Retrofit initialized successfully with base URL: " + currentBaseUrl);
            Log.d(TAG, "Full clientLogin URL will be: " + currentBaseUrl + "api/stream/v1/client/login");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "Error initializing SSL", e);
            throw new RuntimeException("Failed to initialize SSL", e);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Retrofit", e);
            throw new RuntimeException("Failed to initialize Retrofit", e);
        }
    }
    
    /**
     * 获取云端API服务
     */
    public CloudApiService getCloudApiService() {
        // 如果服务未初始化，尝试重新初始化
        if (cloudApiService == null) {
            try {
                initRetrofit();
            } catch (Exception e) {
                Log.e(TAG, "Failed to re-initialize CloudApiService", e);
                return null;
            }
        }
        return cloudApiService;
    }
    
    /**
     * 获取基础URL
     */
    public String getBaseUrl() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        // 保留用户输入的协议，不再强制转换为 HTTPS
        // 确保 base URL 以 / 结尾，Retrofit 要求 base URL 必须以 / 结尾
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl;
    }
    
    /**
     * 设置基础URL
     */
    public void setBaseUrl(String baseUrl) {
        try {
            // 如果没有协议前缀，自动添加 http://
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
                Log.d(TAG, "Auto-added http:// prefix to base URL: " + baseUrl);
            }

            // 自动拼接默认端口 8082（如果没有指定端口）
            baseUrl = appendDefaultPortIfNeeded(baseUrl);

            // 确保 base URL 以 / 结尾，Retrofit 要求 base URL 必须以 / 结尾
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();

            Log.d(TAG, "Base URL set to: " + baseUrl);

            // 重新初始化Retrofit
            initRetrofit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to set base URL: " + baseUrl, e);
        }
    }

    /**
     * 如果URL没有指定端口，自动拼接默认端口 8082
     */
    private String appendDefaultPortIfNeeded(String url) {
        // 检查是否已经包含端口（格式：http://host:port 或 https://host:port）
        // 使用正则检查端口模式
        String pattern = "https?://[^:]+:\\d+";
        if (url.matches(pattern)) {
            // URL已包含端口，直接返回
            return url;
        }

        // 没有端口，添加默认端口 8082
        // 移除末尾的斜杠（如果有）
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + ":8082";
    }
    
    /**
     * 保存用户认证信息
     */
    public void saveAuthInfo(String userId, String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .apply();
        Log.d(TAG, "Auth info saved: userId=" + userId);
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID, "");
    }
    
    /**
     * 获取访问令牌
     */
    public String getAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }
    
    /**
     * 保存主机信息
     */
    public void saveCloudDesktopInfo(String ip, String pin) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CLOUD_DESKTOP_IP, ip)
                .putString(KEY_CLOUD_DESKTOP_PIN, pin)
                .apply();
        Log.d(TAG, "Cloud desktop info saved: ip=" + ip + ", pin=" + pin);
    }
    
    /**
     * 获取主机IP
     */
    public String getCloudDesktopIp() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CLOUD_DESKTOP_IP, "");
    }
    
    /**
     * 获取主机PIN
     */
    public String getCloudDesktopPin() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CLOUD_DESKTOP_PIN, "");
    }
    
    /**
     * 清除认证信息
     */
    public void clearAuthInfo() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_CLOUD_DESKTOP_IP)
                .remove(KEY_CLOUD_DESKTOP_PIN)
                .apply();
        Log.d(TAG, "Auth info cleared");
    }
    
    /**
     * 测试连接
     */
    public boolean testConnection() {
        try {
            CloudApiService service = getCloudApiService();
            if (service == null) {
                Log.w(TAG, "CloudApiService is null, cannot test connection");
                return false;
            }
            
            retrofit2.Response<com.limelight.api.model.ApiResponse> response = 
                service.ping().execute();
            boolean isSuccessful = response.isSuccessful();
            Log.d(TAG, "Connection test result: " + isSuccessful);
            return isSuccessful;
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return false;
        }
    }
}
