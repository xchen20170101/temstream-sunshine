package com.limelight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.limelight.api.ApiClient;
import com.limelight.api.model.ClientLoginResponse;
import com.limelight.api.model.DeviceStatusResponse;
import com.limelight.auth.UserAuthManager;
import com.limelight.preferences.CommConfigActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 登录界面 - 支持三种登录模式
 * 1. 用户登录：用户名 + 密码
 * 2. 设备直连：设备ID + 设备密码
 * 3. 局域网模式：服务端IP + 访问密码（直接连接 Sunshine，不依赖管理端）
 */
public class LoginActivity extends Activity {

    // Tab 模式
    private static final int MODE_USER_LOGIN = 0;
    private static final int MODE_DEVICE_LOGIN = 1;
    private static final int MODE_LAN = 2;
    private int currentMode = MODE_USER_LOGIN;

    // Views - 模式切换按钮
    private Button btnUserLogin;
    private Button btnDeviceLogin;

    // Views - 用户登录
    private EditText editTextUsername;
    private EditText editTextPassword;
    private CheckBox checkBoxRememberMe;
    private LinearLayout userLoginForm;

    // Views - 设备直连
    private EditText editTextDeviceId;
    private EditText editTextDevicePassword;
    private LinearLayout deviceLoginForm;

    // Views - 局域网模式
    private Button btnLanMode;
    private EditText editTextServerIp;
    private EditText editTextLanPassword;
    private LinearLayout lanModeForm;

    // Views - 通用
    private Button buttonLogin;
    private Button buttonClose;
    private Button buttonCommConfig;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private UserAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initManagers();
        setupListeners();

        // 加载记住的用户名
        loadRememberedCredentials();

        // 默认选中用户登录模式
        updateModeSelection();
    }

    private void initViews() {
        // 模式切换按钮
        btnUserLogin = findViewById(R.id.btnUserLogin);
        btnDeviceLogin = findViewById(R.id.btnDeviceLogin);

        // 表单容器
        userLoginForm = findViewById(R.id.userLoginForm);
        deviceLoginForm = findViewById(R.id.deviceLoginForm);
        lanModeForm = findViewById(R.id.lanModeForm);

        // 用户登录视图
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxRememberMe = findViewById(R.id.checkBoxRememberMe);

        // 设备直连视图
        editTextDeviceId = findViewById(R.id.editTextDeviceId);
        editTextDevicePassword = findViewById(R.id.editTextDevicePassword);

        // 局域网模式视图
        btnLanMode = findViewById(R.id.btnLanMode);
        editTextServerIp = findViewById(R.id.editTextServerIp);
        editTextLanPassword = findViewById(R.id.editTextLanPassword);

        // 通用视图
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonClose = findViewById(R.id.buttonClose);
        buttonCommConfig = findViewById(R.id.buttonCommConfig);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initManagers() {
        apiClient = ApiClient.getInstance(this);
        authManager = UserAuthManager.getInstance(this);
    }

    private void setupListeners() {
        // 用户登录按钮
        btnUserLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode != MODE_USER_LOGIN) {
                    switchLoginMode(MODE_USER_LOGIN);
                }
            }
        });

        // 设备直连按钮
        btnDeviceLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode != MODE_DEVICE_LOGIN) {
                    switchLoginMode(MODE_DEVICE_LOGIN);
                }
            }
        });

        // 局域网模式按钮
        btnLanMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode != MODE_LAN) {
                    switchLoginMode(MODE_LAN);
                }
            }
        });

        // 登录按钮
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == MODE_USER_LOGIN) {
                    performUserLogin();
                } else if (currentMode == MODE_DEVICE_LOGIN) {
                    performDeviceLogin();
                } else if (currentMode == MODE_LAN) {
                    performLanModeConnect();
                }
            }
        });

        // 返回按钮
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 通信配置按钮
        buttonCommConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCommConfig();
            }
        });

        // 设备ID输入限制 - 只允许数字
        editTextDeviceId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String digits = s.toString().replaceAll("[^0-9]", "");
                if (!digits.equals(s.toString())) {
                    editTextDeviceId.setText(digits);
                    editTextDeviceId.setSelection(digits.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 设备密码输入限制 - 只允许字母和数字
        editTextDevicePassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filtered = s.toString().replaceAll("[^A-Za-z0-9]", "");
                if (!filtered.equals(s.toString())) {
                    editTextDevicePassword.setText(filtered);
                    editTextDevicePassword.setSelection(filtered.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 切换登录模式
     */
    private void switchLoginMode(int mode) {
        currentMode = mode;
        updateModeSelection();

        // 显示/隐藏对应表单
        if (mode == MODE_USER_LOGIN) {
            userLoginForm.setVisibility(View.VISIBLE);
            deviceLoginForm.setVisibility(View.GONE);
            lanModeForm.setVisibility(View.GONE);
            buttonLogin.setText(R.string.login);
            buttonCommConfig.setVisibility(View.VISIBLE);
        } else if (mode == MODE_DEVICE_LOGIN) {
            userLoginForm.setVisibility(View.GONE);
            deviceLoginForm.setVisibility(View.VISIBLE);
            lanModeForm.setVisibility(View.GONE);
            buttonLogin.setText(R.string.login);
            buttonCommConfig.setVisibility(View.VISIBLE);
        } else {
            userLoginForm.setVisibility(View.GONE);
            deviceLoginForm.setVisibility(View.GONE);
            lanModeForm.setVisibility(View.VISIBLE);
            buttonLogin.setText(R.string.connect);
            buttonCommConfig.setVisibility(View.GONE);
        }
    }

    /**
     * 更新模式按钮选中状态
     */
    private void updateModeSelection() {
        // 重置所有按钮样式
        btnUserLogin.setSelected(currentMode == MODE_USER_LOGIN);
        btnDeviceLogin.setSelected(currentMode == MODE_DEVICE_LOGIN);
        btnLanMode.setSelected(currentMode == MODE_LAN);

        // 更新文字颜色
        btnUserLogin.setTextColor(currentMode == MODE_USER_LOGIN ? 0xFFFFFFFF : 0xFF8ea5c9);
        btnDeviceLogin.setTextColor(currentMode == MODE_DEVICE_LOGIN ? 0xFFFFFFFF : 0xFF8ea5c9);
        btnLanMode.setTextColor(currentMode == MODE_LAN ? 0xFFFFFFFF : 0xFF8ea5c9);
    }

    /**
     * 加载记住的用户名
     */
    private void loadRememberedCredentials() {
        if (authManager.isRememberMe()) {
            String savedUsername = authManager.getUsername();
            if (!TextUtils.isEmpty(savedUsername)) {
                editTextUsername.setText(savedUsername);
                checkBoxRememberMe.setChecked(true);
            }
        }
    }

    /**
     * 用户登录
     */
    private void performUserLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 输入验证
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError(getString(R.string.error_username_required));
            editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_password_required));
            editTextPassword.requestFocus();
            return;
        }

        // 清除错误状态
        editTextUsername.setError(null);
        editTextPassword.setError(null);

        // 显示加载状态
        setLoading(true);

        com.limelight.api.model.ClientLoginRequest loginRequest =
                new com.limelight.api.model.ClientLoginRequest(username, password);

        Call<ClientLoginResponse> call = apiClient.getCloudApiService().clientLogin(loginRequest);
        call.enqueue(new Callback<ClientLoginResponse>() {
            @Override
            public void onResponse(Call<ClientLoginResponse> call, Response<ClientLoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ClientLoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        ClientLoginResponse.ClientLoginData data = loginResponse.getData();
                        if (data != null) {
                            // 保存登录信息
                            boolean rememberMe = checkBoxRememberMe.isChecked();
                            authManager.saveLoginInfo(
                                    username,
                                    null,
                                    username,
                                    System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
                                    rememberMe
                            );

                            List<ClientLoginResponse.DeviceInfo> devices = data.getDevices();

                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.login_success),
                                    Toast.LENGTH_SHORT).show();

                            navigateToMainActivity(devices);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.login_failed) + ": 响应数据为空",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String message = getErrorMessage(loginResponse.getMsg());
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = getString(R.string.network_error);
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += ": " + response.code();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ClientLoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.network_error) + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 设备直连登录
     */
    private void performDeviceLogin() {
        String deviceId = editTextDeviceId.getText().toString().trim();
        String devicePassword = editTextDevicePassword.getText().toString().trim();

        // 输入验证
        if (TextUtils.isEmpty(deviceId)) {
            editTextDeviceId.setError(getString(R.string.error_device_id_required));
            editTextDeviceId.requestFocus();
            return;
        }

        if (deviceId.length() != 8) {
            editTextDeviceId.setError(getString(R.string.error_device_id_length));
            editTextDeviceId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(devicePassword)) {
            editTextDevicePassword.setError(getString(R.string.error_device_password_required));
            editTextDevicePassword.requestFocus();
            return;
        }

        if (devicePassword.length() != 6) {
            editTextDevicePassword.setError(getString(R.string.error_device_password_length));
            editTextDevicePassword.requestFocus();
            return;
        }

        // 清除错误状态
        editTextDeviceId.setError(null);
        editTextDevicePassword.setError(null);

        // 显示加载状态
        setLoading(true);

        // 调用设备状态查询 API
        Call<DeviceStatusResponse> call = apiClient.getCloudApiService()
                .getDeviceStatus(deviceId, devicePassword);

        call.enqueue(new Callback<DeviceStatusResponse>() {
            @Override
            public void onResponse(Call<DeviceStatusResponse> call, Response<DeviceStatusResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    DeviceStatusResponse statusResponse = response.body();

                    if (statusResponse.isSuccess()) {
                        DeviceStatusResponse.DeviceStatusData data = statusResponse.getData();
                        if (data != null) {
                            // 设备在线，验证通过
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.direct_connect_success),
                                    Toast.LENGTH_SHORT).show();

                            // 保存设备信息并跳转到主界面
                            apiClient.saveCloudDesktopInfo(data.getIp(), data.getPin());

                            navigateToDirectConnect(data);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.login_failed) + ": 响应数据为空",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // 处理错误
                        String errorMsg = statusResponse.getMsg();
                        handleDeviceError(errorMsg);
                    }
                } else {
                    String errorMsg = getString(R.string.network_error);
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += ": " + response.code();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DeviceStatusResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.network_error) + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 处理设备直连错误
     */
    private void handleDeviceError(String errorMsg) {
        if (TextUtils.isEmpty(errorMsg)) {
            Toast.makeText(LoginActivity.this,
                    getString(R.string.login_failed),
                    Toast.LENGTH_LONG).show();
            return;
        }

        switch (errorMsg) {
            case "Device.NotFound":
                editTextDeviceId.setError(getString(R.string.device_not_found));
                Toast.makeText(LoginActivity.this,
                        getString(R.string.device_not_found),
                        Toast.LENGTH_LONG).show();
                break;
            case "Device.PasswordRequired":
                editTextDevicePassword.setError(getString(R.string.error_device_password_required));
                Toast.makeText(LoginActivity.this,
                        getString(R.string.error_device_password_required),
                        Toast.LENGTH_LONG).show();
                break;
            case "Device.PasswordMismatch":
                editTextDevicePassword.setError(getString(R.string.device_password_error));
                Toast.makeText(LoginActivity.this,
                        getString(R.string.device_password_error),
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(LoginActivity.this,
                        getString(R.string.login_failed) + ": " + errorMsg,
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
        buttonClose.setEnabled(!loading);

        // 禁用/启用输入框
        editTextUsername.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
        editTextDeviceId.setEnabled(!loading);
        editTextDevicePassword.setEnabled(!loading);
        editTextServerIp.setEnabled(!loading);
        editTextLanPassword.setEnabled(!loading);
    }

    private void openCommConfig() {
        Intent intent = new Intent(this, CommConfigActivity.class);
        startActivity(intent);
    }

    /**
     * 根据错误码获取用户友好的错误信息
     */
    private String getErrorMessage(String errorCode) {
        if (TextUtils.isEmpty(errorCode)) {
            return getString(R.string.login_failed);
        }

        switch (errorCode) {
            case "User.PasswordIsWrong":
                return "用户名或密码错误";
            case "User.Disable":
                return "账号已被禁用，请联系管理员";
            case "User.NoBindDevice":
                return "当前账号未绑定任何主机";
            case "Common.InvalidParam":
                return "请求参数错误";
            default:
                return getString(R.string.login_failed) + ": " + errorCode;
        }
    }

    /**
     * 跳转到主界面（用户登录）
     */
    private void navigateToMainActivity(List<ClientLoginResponse.DeviceInfo> devices) {
        Intent intent = new Intent(this, PcView.class);

        if (devices != null && !devices.isEmpty()) {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String devicesJson = gson.toJson(devices);
                intent.putExtra("devices_json", devicesJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 跳转到主界面（设备直连）
     */
    private void navigateToDirectConnect(DeviceStatusResponse.DeviceStatusData device) {
        // 保存设备直连登录状态，让 PcView 知道这是设备直连模式
        authManager.saveDeviceDirectLogin(device.getDeviceId(),
                System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // 30天有效期

        Intent intent = new Intent(this, PcView.class);

        // 创建设备列表（只有一个设备）
        List<ClientLoginResponse.DeviceInfo> devices = new ArrayList<>();
        ClientLoginResponse.DeviceInfo deviceInfo = new ClientLoginResponse.DeviceInfo();
        deviceInfo.setDeviceId(device.getDeviceId());
        deviceInfo.setDeviceName(device.getName());
        deviceInfo.setIp(device.getIp());
        deviceInfo.setPin(device.getPin());
        devices.add(deviceInfo);

        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String devicesJson = gson.toJson(devices);
            intent.putExtra("devices_json", devicesJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 局域网模式连接
     * 直接连接 Sunshine 服务器，不依赖管理端
     */
    private void performLanModeConnect() {
        String serverIp = editTextServerIp.getText().toString().trim();
        String password = editTextLanPassword.getText().toString().trim();

        // 输入验证
        if (TextUtils.isEmpty(serverIp)) {
            editTextServerIp.setError(getString(R.string.error_server_ip_required));
            editTextServerIp.requestFocus();
            return;
        }

        // 验证 IP 地址格式
        if (!isValidIpAddress(serverIp)) {
            editTextServerIp.setError(getString(R.string.error_invalid_ip));
            editTextServerIp.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextLanPassword.setError(getString(R.string.error_password_required));
            editTextLanPassword.requestFocus();
            return;
        }

        // 清除错误状态
        editTextServerIp.setError(null);
        editTextLanPassword.setError(null);

        // 保存局域网模式配置（密码会在配对时验证）
        authManager.saveLanModeLogin(serverIp, password,
                System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000));

        // 跳转到配对/连接页面
        navigateToLanMode(serverIp, password);
    }

    /**
     * 验证 IP 地址格式
     */
    private boolean isValidIpAddress(String ip) {
        if (TextUtils.isEmpty(ip)) {
            return false;
        }
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipPattern);
    }

    /**
     * 跳转到局域网模式连接（设备直连模式）
     */
    private void navigateToLanMode(String serverIp, String password) {
        Intent intent = new Intent(this, PcView.class);

        // 创建设备列表（只有一个设备，局域网模式）
        List<ClientLoginResponse.DeviceInfo> devices = new ArrayList<>();
        ClientLoginResponse.DeviceInfo deviceInfo = new ClientLoginResponse.DeviceInfo();
        deviceInfo.setDeviceId("LAN"); // 标记为局域网模式
        deviceInfo.setDeviceName(getString(R.string.lan_mode_device_name));
        deviceInfo.setIp(serverIp);
        deviceInfo.setPin("8888"); // 固定 PIN，配对时使用
        devices.add(deviceInfo);

        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String devicesJson = gson.toJson(devices);
            intent.putExtra("devices_json", devicesJson);
            intent.putExtra("lan_mode", true); // 标记为局域网模式
            intent.putExtra("lan_password", password); // 局域网访问密码
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
