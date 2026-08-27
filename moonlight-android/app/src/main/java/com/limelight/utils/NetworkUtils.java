package com.limelight.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * 网络工具类
 */
public class NetworkUtils {
    
    private static final String TAG = "NetworkUtils";
    
    /**
     * 检查网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && 
                       (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                // 兼容旧版本Android
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
            return false;
        }
    }
    
    /**
     * 检查是否连接到WiFi
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && 
                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else {
                // 兼容旧版本Android
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && 
                       networkInfo.getType() == ConnectivityManager.TYPE_WIFI && 
                       networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi connection", e);
            return false;
        }
    }
    
    /**
     * 获取网络类型描述
     */
    public static String getNetworkTypeDescription(Context context) {
        if (context == null) {
            return "Unknown";
        }
        
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return "No Connection Manager";
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return "No Active Network";
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities == null) {
                    return "No Network Capabilities";
                }
                
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Cellular";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "Ethernet";
                } else {
                    return "Other";
                }
            } else {
                // 兼容旧版本Android
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    return "No Network Info";
                }
                
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        return "WiFi";
                    case ConnectivityManager.TYPE_MOBILE:
                        return "Cellular";
                    case ConnectivityManager.TYPE_ETHERNET:
                        return "Ethernet";
                    default:
                        return "Other (" + networkInfo.getTypeName() + ")";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network type", e);
            return "Error";
        }
    }
}
