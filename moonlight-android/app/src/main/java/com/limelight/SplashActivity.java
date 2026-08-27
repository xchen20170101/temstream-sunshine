package com.limelight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.limelight.utils.DebugHelper;

/**
 * 启动页面，始终导航到登录界面
 */
public class SplashActivity extends Activity {
    
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 1000; // 1秒延迟
    private Handler mainHandler;
    private boolean isDestroyed = false;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // 设置布局文件
            setContentView(R.layout.activity_splash);
            
            // 获取状态文本视图
            statusText = findViewById(R.id.statusText);
            
            // 初始化Handler
            mainHandler = new Handler(Looper.getMainLooper());
            
            Log.d(TAG, "SplashActivity created successfully");
            
            // 记录应用启动信息和检查崩溃信息
            DebugHelper.logAppStartInfo(this, "SplashActivity");
            DebugHelper.checkAndShowCrashInfo(this);
            DebugHelper.logCrashStats(this);
            
            // 延迟检查登录状态，给用户一个启动的视觉反馈
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isDestroyed && !isFinishing()) {
                        checkLoginAndNavigate();
                    }
                }
            }, SPLASH_DELAY);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // 发生异常时直接跳转到登录界面
            navigateToLogin();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        Log.d(TAG, "SplashActivity destroyed");
    }
    
    private void checkLoginAndNavigate() {
        try {
            Log.d(TAG, "Navigating to login screen...");
            updateStatus("正在启动...");
            
            // 【修改】禁用自动登录，始终跳转到登录界面
            // 用户需要手动点击登录按钮才能登录
            navigateToLogin();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in checkLoginAndNavigate", e);
            updateStatus("发生错误，跳转到登录界面...");
            navigateToLogin();
        }
    }
    
    private void updateStatus(String status) {
        try {
            if (statusText != null && !isDestroyed && !isFinishing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (statusText != null) {
                            statusText.setText(status);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update status text", e);
        }
    }
    
    private void navigateToLogin() {
        try {
            // 检查Activity是否还存活
            if (isDestroyed || isFinishing()) {
                Log.w(TAG, "Activity is finishing, skipping login navigation");
                return;
            }
            
            updateStatus("跳转到登录界面...");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Critical error: Cannot navigate to login", e);
            updateStatus("无法启动应用");
            // 如果连跳转到登录界面都失败，则关闭应用
            finish();
        }
    }
}
