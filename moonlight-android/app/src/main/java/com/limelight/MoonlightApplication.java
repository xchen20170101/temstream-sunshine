package com.limelight;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Moonlight应用程序类，用于全局初始化和异常处理
 */
public class MoonlightApplication extends Application {
    
    private static final String TAG = "MoonlightApp";
    private static final String CRASH_PREFS = "crash_logs";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "MoonlightApplication starting...");
        
        // 设置全局异常处理器
        setupGlobalExceptionHandler();
        
        // 检查上次是否有崩溃
        checkPreviousCrash();
        
        Log.d(TAG, "MoonlightApplication initialized successfully");
    }
    
    /**
     * 设置全局异常处理器
     */
    private void setupGlobalExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
                
                try {
                    // 保存崩溃信息到本地
                    saveCrashInfo(throwable, thread);
                    
                    // 记录应用状态
                    logApplicationState();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save crash info", e);
                } finally {
                    // 调用系统默认处理器
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    } else {
                        System.exit(1);
                    }
                }
            }
        });
    }
    
    /**
     * 保存崩溃信息到本地存储
     */
    private void saveCrashInfo(Throwable throwable, Thread thread) {
        try {
            SharedPreferences prefs = getSharedPreferences(CRASH_PREFS, MODE_PRIVATE);
            
            // 获取详细的堆栈跟踪
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // 获取时间戳
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            // 获取应用版本信息
            String versionInfo = getVersionInfo();
            
            // 构建崩溃报告
            StringBuilder crashReport = new StringBuilder();
            crashReport.append("=== CRASH REPORT ===\n");
            crashReport.append("Time: ").append(timestamp).append("\n");
            crashReport.append("Thread: ").append(thread.getName()).append("\n");
            crashReport.append("Version: ").append(versionInfo).append("\n");
            crashReport.append("Exception: ").append(throwable.getClass().getSimpleName()).append("\n");
            crashReport.append("Message: ").append(throwable.getMessage()).append("\n");
            crashReport.append("Stack Trace:\n").append(stackTrace).append("\n");
            crashReport.append("===================\n");
            
            // 保存到SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_crash", crashReport.toString());
            editor.putString("last_crash_time", timestamp);
            editor.putString("last_crash_thread", thread.getName());
            editor.putString("last_crash_exception", throwable.getClass().getSimpleName());
            editor.putString("last_crash_message", throwable.getMessage());
            editor.putInt("crash_count", prefs.getInt("crash_count", 0) + 1);
            editor.putBoolean("has_crash", true);
            editor.apply();
            
            Log.d(TAG, "Crash info saved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash info", e);
        }
    }
    
    /**
     * 记录应用状态信息
     */
    private void logApplicationState() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Log.d(TAG, "=== APPLICATION STATE ===");
            Log.d(TAG, "Max Memory: " + (maxMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "Total Memory: " + (totalMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "========================");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log application state", e);
        }
    }
    
    /**
     * 获取应用版本信息
     */
    private String getVersionInfo() {
        try {
            String packageName = getPackageName();
            String versionName = getPackageManager().getPackageInfo(packageName, 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            return versionName + " (" + versionCode + ")";
        } catch (Exception e) {
            Log.w(TAG, "Failed to get version info", e);
            return "Unknown";
        }
    }
    
    /**
     * 检查上次是否有崩溃
     */
    private void checkPreviousCrash() {
        try {
            SharedPreferences prefs = getSharedPreferences(CRASH_PREFS, MODE_PRIVATE);
            boolean hasCrash = prefs.getBoolean("has_crash", false);
            
            if (hasCrash) {
                String lastCrashTime = prefs.getString("last_crash_time", "Unknown");
                String lastCrashException = prefs.getString("last_crash_exception", "Unknown");
                int crashCount = prefs.getInt("crash_count", 0);
                
                Log.w(TAG, "Previous crash detected:");
                Log.w(TAG, "Last crash time: " + lastCrashTime);
                Log.w(TAG, "Last crash exception: " + lastCrashException);
                Log.w(TAG, "Total crash count: " + crashCount);
                
                // 清除崩溃标记，但保留崩溃信息用于调试
                prefs.edit().putBoolean("has_crash", false).apply();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to check previous crash", e);
        }
    }
    
    /**
     * 获取崩溃信息（供调试使用）
     */
    public String getLastCrashInfo() {
        try {
            SharedPreferences prefs = getSharedPreferences(CRASH_PREFS, MODE_PRIVATE);
            return prefs.getString("last_crash", "No crash information available");
        } catch (Exception e) {
            Log.e(TAG, "Failed to get crash info", e);
            return "Failed to retrieve crash information";
        }
    }
    
    /**
     * 清除崩溃信息
     */
    public void clearCrashInfo() {
        try {
            SharedPreferences prefs = getSharedPreferences(CRASH_PREFS, MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "Crash info cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear crash info", e);
        }
    }
}
