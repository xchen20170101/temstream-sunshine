package com.limelight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.api.ApiClient;
import com.limelight.api.model.ApiResponse;
import com.limelight.auth.UserAuthManager;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 修改密码Activity
 */
public class ChangePasswordActivity extends Activity {
    
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private TextView tvPasswordStrength;
    private Button btnConfirm;
    private Button btnCancel;
    private ImageButton btnBack;
    
    private UserAuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        
        authManager = UserAuthManager.getInstance(this);
        
        // 初始化视图
        initViews();
        
        // 设置监听器
        setupListeners();
    }
    
    private void initViews() {
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.btnBack);
    }
    
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // 取消按钮
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // 确认修改按钮
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptChangePassword();
            }
        });
    }
    
    /**
     * 尝试修改密码
     */
    private void attemptChangePassword() {
        // 重置错误
        etOldPassword.setError(null);
        etNewPassword.setError(null);
        etConfirmPassword.setError(null);
        
        // 获取输入值
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        
        boolean cancel = false;
        View focusView = null;
        
        // 验证当前密码
        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("请输入当前密码");
            focusView = etOldPassword;
            cancel = true;
        }
        
        // 验证新密码
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("请输入新密码");
            focusView = etNewPassword;
            cancel = true;
        } else if (!isPasswordValid(newPassword)) {
            etNewPassword.setError("密码至少8位，需包含字母和数字");
            focusView = etNewPassword;
            cancel = true;
        }
        
        // 验证确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("请再次输入新密码");
            focusView = etConfirmPassword;
            cancel = true;
        } else if (!confirmPassword.equals(newPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            focusView = etConfirmPassword;
            cancel = true;
        }
        
        if (cancel) {
            // 如果有错误，聚焦到第一个错误字段
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // 执行密码修改
            performChangePassword(oldPassword, newPassword);
        }
    }
    
    /**
     * 验证密码强度
     */
    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            return false;
        }
        
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 执行密码修改API调用
     */
    private void performChangePassword(final String oldPassword, final String newPassword) {
        // 禁用按钮，防止重复提交
        btnConfirm.setEnabled(false);
        btnConfirm.setText("修改中...");
        
        String username = authManager.getUsername();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "未找到用户信息，请重新登录", Toast.LENGTH_SHORT).show();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("确认修改");
            return;
        }

        // 局域网环境：直接以明文方式发送用户名、旧密码、新密码
        // 调用API
        ApiClient.getInstance(this).getCloudApiService().resetPassword(username, oldPassword, newPassword)
            .enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("确认修改");
                            
                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse apiResponse = response.body();
                                if (apiResponse.isSuccess()) {
                                    Toast.makeText(ChangePasswordActivity.this, 
                                        "密码修改成功，请重新登录", Toast.LENGTH_LONG).show();
                                    
                                    // 清除登录信息
                                    authManager.clearLoginInfo();
                                    
                                    // 跳转到登录界面并清空活动栈
                                    Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    String errorMsg = apiResponse.getMessage();
                                    if (TextUtils.isEmpty(errorMsg)) {
                                        errorMsg = "密码修改失败";
                                    }
                                    Toast.makeText(ChangePasswordActivity.this, 
                                        errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(ChangePasswordActivity.this, 
                                    "密码修改失败：服务器错误", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnConfirm.setEnabled(true);
                            btnConfirm.setText("确认修改");
                            
                            String errorMsg = "网络错误：" + t.getMessage();
                            Toast.makeText(ChangePasswordActivity.this, 
                                errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
    }
}

