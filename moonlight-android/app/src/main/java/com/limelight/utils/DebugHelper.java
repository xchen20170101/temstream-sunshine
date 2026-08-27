package com.limelight.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.limelight.MoonlightApplication;

/**
 * 调试辅助工具类，用于查看和管理崩溃信息
 */
public class DebugHelper {
    
    private static final String TAG = "DebugHelper";
    
    /**
     * 检查并显示上次崩溃信息
     */
    public static void checkAndShowCrashInfo(Context context) {
        try {
            if (context.getApplicationContext() instanceof MoonlightApplication) {
                MoonlightApplication app = (MoonlightApplication) context.getApplicationContext();
                String crashInfo = app.getLastCrashInfo();
                
                if (crashInfo != null && !crashInfo.equals("No crash information available")) {
                    Log.w(TAG, "=== PREVIOUS CRASH INFORMATION ===");
                    Log.w(TAG, crashInfo);
                    Log.w(TAG, "================================");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check crash info", e);
        }
    }
    
    /**
     * 清除崩溃信息
     */
    public static void clearCrashInfo(Context context) {
        try {
            if (context.getApplicationContext() instanceof MoonlightApplication) {
                MoonlightApplication app = (MoonlightApplication) context.getApplicationContext();
                app.clearCrashInfo();
                Log.d(TAG, "Crash info cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear crash info", e);
        }
    }
    
    /**
     * 记录应用启动信息
     */
    public static void logAppStartInfo(Context context, String activityName) {
        try {
            Log.d(TAG, "=== APP START INFO ===");
            Log.d(TAG, "Activity: " + activityName);
            Log.d(TAG, "Package: " + context.getPackageName());
            Log.d(TAG, "Process: " + android.os.Process.myPid());
            Log.d(TAG, "Thread: " + Thread.currentThread().getName());
            
            // 记录内存信息
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            
            Log.d(TAG, "Max Memory: " + (maxMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "Total Memory: " + (totalMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
            Log.d(TAG, "=====================");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log app start info", e);
        }
    }
    
    /**
     * 测试异常处理器（仅用于调试）
     */
    public static void testCrashHandler() {
        Log.w(TAG, "Testing crash handler...");
        throw new RuntimeException("Test crash for debugging purposes");
    }
    
    /**
     * 获取崩溃统计信息
     */
    public static void logCrashStats(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("crash_logs", Context.MODE_PRIVATE);
            int crashCount = prefs.getInt("crash_count", 0);
            String lastCrashTime = prefs.getString("last_crash_time", "Never");
            String lastCrashException = prefs.getString("last_crash_exception", "None");
            
            Log.d(TAG, "=== CRASH STATISTICS ===");
            Log.d(TAG, "Total crashes: " + crashCount);
            Log.d(TAG, "Last crash time: " + lastCrashTime);
            Log.d(TAG, "Last crash type: " + lastCrashException);
            Log.d(TAG, "========================");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log crash stats", e);
        }
    }
}
