package com.limelight.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证拦截器 - 添加Cookie认证信息
 */
public class AuthInterceptor implements Interceptor {
    private static final String PREF_NAME = "api_config";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    
    private Context context;
    
    public AuthInterceptor(Context context) {
        this.context = context;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 获取保存的认证信息
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, "");
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, "");
        
        // 如果有认证信息，添加到Cookie中
        if (!userId.isEmpty() && !accessToken.isEmpty()) {
            String cookieValue = "userId=" + userId + "; accessToken=" + accessToken;
            Request newRequest = originalRequest.newBuilder()
                    .header("Cookie", cookieValue)
                    .build();
            return chain.proceed(newRequest);
        }
        
        return chain.proceed(originalRequest);
    }
}

