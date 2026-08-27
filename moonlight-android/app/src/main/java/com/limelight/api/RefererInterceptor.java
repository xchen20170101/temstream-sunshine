package com.limelight.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Referer拦截器 - 添加Referer头
 */
public class RefererInterceptor implements Interceptor {
    private ApiClient apiClient;
    
    public RefererInterceptor(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 获取基础URL作为Referer
        String baseUrl = apiClient.getBaseUrl();
        // 移除末尾的斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // 添加Referer头
        Request newRequest = originalRequest.newBuilder()
                .header("Referer", baseUrl)
                .build();
        
        return chain.proceed(newRequest);
    }
}

