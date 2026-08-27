package com.limelight.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.limelight.R;
import com.limelight.api.ApiClient;

/**
 * 通信配置界面
 */
public class CommConfigActivity extends Activity {
    
    private TextInputEditText editTextServerUrl;
    private Button buttonSave;
    private Button buttonCancel;
    
    private ApiClient apiClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm_config);
        
        initViews();
        initData();
        setupListeners();
    }
    
    private void initViews() {
        editTextServerUrl = findViewById(R.id.editTextServerUrl);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
    }
    
    private void initData() {
        apiClient = ApiClient.getInstance(this);
        
        // 显示当前配置的服务器地址，移除内部拼接的端口后缀
        String currentUrl = apiClient.getBaseUrl();
        if (currentUrl != null && currentUrl.endsWith(":8082/")) {
            currentUrl = currentUrl.substring(0, currentUrl.length() - 6);
        } else if (currentUrl != null && currentUrl.endsWith(":8082")) {
            currentUrl = currentUrl.substring(0, currentUrl.length() - 5);
        }
        editTextServerUrl.setText(currentUrl);
    }
    
    private void setupListeners() {
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfiguration();
            }
        });
        
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void saveConfiguration() {
        String serverUrl = editTextServerUrl.getText().toString().trim();
        
        if (TextUtils.isEmpty(serverUrl)) {
            editTextServerUrl.setError(getString(R.string.error_server_url_required));
            editTextServerUrl.requestFocus();
            return;
        }
        
        if (!isValidUrl(serverUrl)) {
            editTextServerUrl.setError(getString(R.string.error_invalid_url));
            editTextServerUrl.requestFocus();
            return;
        }
        
        // 保存配置
        apiClient.setBaseUrl(serverUrl);
        
        Toast.makeText(this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private boolean isValidUrl(String url) {
        // 支持三种格式：
        // 1. http://127.0.0.1:8082
        // 2. https://127.0.0.1:8082
        // 3. 127.0.0.1 (纯IP地址，自动补全默认端口)

        // 如果有协议前缀，直接验证
        if (url.startsWith("https://")) {
            return true;
        }
        if (url.startsWith("http://")) {
            return true;  // 允许 HTTP
        }

        // 如果没有协议前缀，验证是否为有效的IP地址
        return isValidIpAddress(url);
    }
    
    /**
     * 验证IP地址格式
     * 支持IPv4格式：xxx.xxx.xxx.xxx
     */
    private boolean isValidIpAddress(String ip) {
        if (TextUtils.isEmpty(ip)) {
            return false;
        }
        
        // 简单的IPv4格式验证
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
}
