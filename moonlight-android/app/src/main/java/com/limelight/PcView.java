package com.limelight;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import com.limelight.binding.PlatformBinding;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.grid.PcGridAdapter;
import com.limelight.grid.assets.DiskAssetLoader;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.nvstream.http.PairingManager.PairState;
import com.limelight.nvstream.wol.WakeOnLanSender;
import com.limelight.preferences.AddComputerManually;
import com.limelight.preferences.GlPreferences;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettings;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.UiHelper;
import com.limelight.auth.UserAuthManager;
import com.limelight.api.ApiClient;
import com.limelight.api.model.Device;
import com.limelight.api.model.DeviceListResponse;
import com.limelight.api.service.CloudApiService;
import com.limelight.utils.DeviceConverter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.xmlpull.v1.XmlPullParserException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PcView extends Activity implements AdapterFragmentCallbacks {
    private RelativeLayout noPcFoundLayout;
    private PcGridAdapter pcGridAdapter;
    private ShortcutHelper shortcutHelper;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private boolean freezeUpdates, runningPolling, inForeground, completeOnCreateCalled;
    
    // 【新增】设备列表缓存，用于虚拟机操作选择
    private java.util.List<Device> cachedDeviceList = new java.util.ArrayList<>();
    
    // 【新增】从登录页传递过来的设备列表 JSON
    private String pendingDevicesJson = null;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    localBinder.waitForReady();

                    // Now make the binder visible
                    managerBinder = localBinder;

                    // 根据登录信息尝试添加主机
                    PcView.this.addCloudDesktopFromLogin();

                    // Start updates
                    startComputerUpdates();

                    // Force a keypair to be generated early to avoid discovery delays
                    new AndroidCryptoProvider(PcView.this).getClientCertificate();
                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Only reinitialize views if completeOnCreate() was called
        // before this callback. If it was not, completeOnCreate() will
        // handle initializing views with the config change accounted for.
        // This is not prone to races because both callbacks are invoked
        // in the main thread.
        if (completeOnCreateCalled) {
            // Reinitialize views just in case orientation changed
            initializeViews();
        }
    }

    // 【已移除】PAIR_ID 和 UNPAIR_ID - 不再需要PIN码配对
    // private final static int PAIR_ID = 2;
    // private final static int UNPAIR_ID = 3;
    private final static int WOL_ID = 4;
    private final static int DELETE_ID = 5;
    private final static int RESUME_ID = 6;
    private final static int QUIT_ID = 7;
    private final static int VIEW_DETAILS_ID = 8;
    private final static int FULL_APP_LIST_ID = 9;
    private final static int TEST_NETWORK_ID = 10;
    private final static int GAMESTREAM_EOL_ID = 11;
    private static final String CLOUD_DESKTOP_UUID = "CLOUD_DESKTOP";

    private void initializeViews() {
        try {
            setContentView(R.layout.activity_pc_view);

            UiHelper.notifyNewRootView(this);

            // Allow floating expanded PiP overlays while browsing PCs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setShouldDockBigOverlays(false);
            }

            // Set default preferences if we've never been run
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

            // Set the correct layout for the PC grid
            if (pcGridAdapter != null) {
                pcGridAdapter.updateLayoutWithPreferences(this, PreferenceConfiguration.readPreferences(this));
            }

            // Setup the list view
            ImageButton settingsButton = findViewById(R.id.settingsButton);
            ImageButton refreshButton = findViewById(R.id.refreshButton);
            ImageButton btnChangePassword = findViewById(R.id.btnChangePassword);
            ImageButton btnLogout = findViewById(R.id.btnLogout);
            // 【已移除】addComputerButton - 设备通过云端API获取，不需要手动添加
            // 【已移除】helpButton - 简化界面
            // 【已移除】menuButton - 功能按钮直接放到左侧

            if (settingsButton != null) {
                settingsButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            startActivity(new Intent(PcView.this, StreamSettings.class));
                        } catch (Exception e) {
                            LimeLog.severe("Failed to start StreamSettings: " + e.getMessage());
                        }
                    }
                });
            }
            
            if (refreshButton != null) {
                refreshButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            // 手动刷新设备列表
                            Toast.makeText(PcView.this, "正在刷新主机列表...", Toast.LENGTH_SHORT).show();
                            
                            // 停止当前轮询
                            if (managerBinder != null) {
                                managerBinder.stopPolling();
                            }
                            
                            // 重新开始轮询以触发设备列表更新
                            startComputerUpdates();
                        } catch (Exception e) {
                            LimeLog.severe("Failed to refresh device list: " + e.getMessage());
                            Toast.makeText(PcView.this, "刷新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            // 修改密码按钮
            if (btnChangePassword != null) {
                btnChangePassword.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PcView.this, ChangePasswordActivity.class);
                        startActivity(intent);
                    }
                });
            }
            
            // 退出登录按钮
            if (btnLogout != null) {
                btnLogout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performLogout();
                    }
                });
            }
            
            // 【已移除】addComputerButton 点击事件 - 不再需要手动添加设备功能

            // 安全地添加Fragment
            try {
                if (!isFinishing() && !isDestroyed()) {
                    getFragmentManager().beginTransaction()
                        .replace(R.id.pcFragmentContainer, new AdapterFragment())
                        .commitAllowingStateLoss();
                }
            } catch (Exception e) {
                LimeLog.severe("Failed to add AdapterFragment: " + e.getMessage());
            }

            noPcFoundLayout = findViewById(R.id.no_pc_found_layout);
            if (noPcFoundLayout != null) {
                if (pcGridAdapter != null && pcGridAdapter.getCount() == 0) {
                    noPcFoundLayout.setVisibility(View.VISIBLE);
                }
                else {
                    noPcFoundLayout.setVisibility(View.INVISIBLE);
                }
            }
            
            if (pcGridAdapter != null) {
                pcGridAdapter.notifyDataSetChanged();
            }
            
            LimeLog.info("PcView initializeViews completed successfully");
            
        } catch (Exception e) {
            LimeLog.severe("Error in initializeViews: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让上层处理
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 接收登录传递的设备列表
            Intent intent = getIntent();
            String devicesJson = null;
            if (intent != null) {
                devicesJson = intent.getStringExtra("devices_json");
                if (devicesJson != null && !devicesJson.isEmpty()) {
                    LimeLog.info("Received devices from login: " + devicesJson);
                }
            }

            // 检查登录状态（普通用户登录、设备直连登录或局域网模式登录）
            UserAuthManager authManager = UserAuthManager.getInstance(this);
            boolean isLanMode = authManager.isLanModeLogin();
            if (!authManager.isLoggedIn() && !authManager.isDeviceDirectLogin() && !isLanMode) {
                // Not logged in, redirect to login activity
                LimeLog.info("User not logged in, redirecting to LoginActivity");
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                finish();
                return;
            }

            // 保存设备列表到成员变量，供 serviceConnection 使用
            pendingDevicesJson = devicesJson;

            // Assume we're in the foreground when created to avoid a race
            // between binding to CMS and onResume()
            inForeground = true;

            // 简化启动流程，直接完成onCreate而不进行复杂的OpenGL初始化
            // 这可以避免OpenGL相关的崩溃问题
            LimeLog.info("Starting PcView with simplified initialization");
            completeOnCreate();
            
        } catch (Exception e) {
            LimeLog.severe("Error in PcView onCreate: " + e.getMessage());
            e.printStackTrace();
            
            // 发生异常时，尝试跳转到登录界面
            try {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
            } catch (Exception ex) {
                LimeLog.severe("Failed to start LoginActivity: " + ex.getMessage());
            }
            finish();
        }
    }

    private void completeOnCreate() {
        try {
            completeOnCreateCalled = true;

            shortcutHelper = new ShortcutHelper(this);

            UiHelper.setLocale(this);

            // Bind to the computer manager service
            bindService(new Intent(PcView.this, ComputerManagerService.class), serviceConnection,
                    Service.BIND_AUTO_CREATE);

            pcGridAdapter = new PcGridAdapter(this, PreferenceConfiguration.readPreferences(this));

            initializeViews();
            
            LimeLog.info("PcView completeOnCreate finished successfully");
            
        } catch (Exception e) {
            LimeLog.severe("Error in completeOnCreate: " + e.getMessage());
            e.printStackTrace();
            
            // 如果初始化失败，尝试跳转到登录界面
            try {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
            } catch (Exception ex) {
                LimeLog.severe("Failed to start LoginActivity from completeOnCreate: " + ex.getMessage());
            }
            finish();
        }
    }

    private void startComputerUpdates() {
        // Only allow polling to start if we're bound to CMS, polling is not already running,
        // and our activity is in the foreground.
        if (managerBinder != null && !runningPolling && inForeground) {
            freezeUpdates = false;
            managerBinder.startPolling(new ComputerManagerListener() {
                @Override
                public void notifyComputerUpdated(final ComputerDetails details) {
                    if (!freezeUpdates) {
                        PcView.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateComputer(details);
                            }
                        });

                        // Add a launcher shortcut for this PC (off the main thread to prevent ANRs)
                        if (details.pairState == PairState.PAIRED) {
                            shortcutHelper.createAppViewShortcutForOnlineHost(details);
                        }
                    }
                }
            });
            runningPolling = true;
        }
        else {
            LimeLog.info("startComputerUpdates skipped; managerBinder=" + (managerBinder != null) +
                    ", runningPolling=" + runningPolling + ", inForeground=" + inForeground);
        }
    }
    
    /**
     * 从云端API获取设备列表
     */
    private void fetchDevicesFromCloud() {
        LimeLog.info("Starting to fetch devices from cloud API...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApiClient apiClient = ApiClient.getInstance(PcView.this);
                    if (apiClient == null) {
                        LimeLog.warning("ApiClient is null, cannot fetch devices");
                        return;
                    }
                    
                    CloudApiService cloudApiService = apiClient.getCloudApiService();
                    if (cloudApiService == null) {
                        LimeLog.warning("CloudApiService is null, cannot fetch devices");
                        return;
                    }

                    LimeLog.info("Calling cloud API to get device list...");
                    retrofit2.Call<DeviceListResponse> call = cloudApiService.getUserDevices();
                    retrofit2.Response<DeviceListResponse> response = call.execute();
                    
                    LimeLog.info("Cloud API response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        final DeviceListResponse deviceListResponse = response.body();
                        
                        if (deviceListResponse.isSuccess() && deviceListResponse.getDevices() != null) {
                            final int deviceCount = deviceListResponse.getDevices().size();
                            LimeLog.info("Successfully fetched " + deviceCount + " devices from cloud");
                            
                            // 【新增】缓存设备列表，用于虚拟机操作
                            cachedDeviceList.clear();
                            cachedDeviceList.addAll(deviceListResponse.getDevices());
                            
                            // 将Device转换为ComputerDetails并添加到管理器
                            int addedCount = 0;
                            for (Device device : deviceListResponse.getDevices()) {
                                LimeLog.info("Processing device: " + device.getName() + " (UUID: " + device.getUuid() + ")");
                                ComputerDetails details = DeviceConverter.deviceToComputerDetails(device);
                                if (details != null && managerBinder != null) {
                                    try {
                                        // 添加设备到本地数据库和轮询列表
                                        managerBinder.addComputerBlocking(details);
                                        addedCount++;
                                        LimeLog.info("Added device: " + device.getName());
                                    } catch (InterruptedException e) {
                                        LimeLog.warning("Failed to add device: " + device.getName());
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                            final int finalAddedCount = addedCount;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 刷新UI显示
                                    if (pcGridAdapter != null) {
                                        pcGridAdapter.notifyDataSetChanged();
                                    }
                                    LimeLog.info("UI refreshed, total devices added: " + finalAddedCount);
                                }
                            });
                        } else {
                            String message = deviceListResponse != null ? deviceListResponse.getMessage() : "null response";
                            LimeLog.warning("Device list API returned unsuccessful: " + message);
                        }
                    } else {
                        LimeLog.warning("Failed to fetch devices from cloud: HTTP " + response.code() + 
                            " - " + (response.message() != null ? response.message() : ""));
                    }
                } catch (Exception e) {
                    LimeLog.severe("Error fetching devices from cloud: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addCloudDesktopFromLogin() {
        // 检查是否有从登录页传递过来的设备列表
        if (pendingDevicesJson == null || pendingDevicesJson.isEmpty()) {
            LimeLog.info("No devices from login, skip adding cloud desktops");
            return;
        }

        if (managerBinder == null) {
            LimeLog.warning("ManagerBinder is null, cannot add cloud desktop");
            return;
        }

        try {
            // 检查是否为局域网模式
            Intent intent = getIntent();
            boolean isLanMode = intent != null && intent.getBooleanExtra("lan_mode", false);
            String lanPassword = (intent != null) ? intent.getStringExtra("lan_password") : null;
            LimeLog.info("Login mode: " + (isLanMode ? "LAN Mode" : "Normal/Device Direct") + ", has lanPassword: " + (lanPassword != null && !lanPassword.isEmpty()));

            // 解析设备列表 JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<com.limelight.api.model.ClientLoginResponse.DeviceInfo>>(){}.getType();
            java.util.List<com.limelight.api.model.ClientLoginResponse.DeviceInfo> devices = gson.fromJson(pendingDevicesJson, listType);

            if (devices == null || devices.isEmpty()) {
                LimeLog.info("Devices list is empty after parsing");
                return;
            }

            LimeLog.info("Processing " + devices.size() + " devices from login response");

            // 遍历所有设备并添加
            for (com.limelight.api.model.ClientLoginResponse.DeviceInfo device : devices) {
                String ip = device.getIp();
                String pin = device.getPin();
                String deviceName = device.getDeviceName();

                if (ip == null || ip.isEmpty()) {
                    LimeLog.warning("Device has no IP, skipping: " + deviceName);
                    continue;
                }

                // 局域网模式下使用局域网设备名称
                if (isLanMode) {
                    deviceName = getString(R.string.lan_mode_device_name);
                }

                LimeLog.info("[CloudDesktop] Adding device: " + deviceName + " at " + ip);

                // Moonlight/Sunshine 端口配置
                int httpPort = 47989;
                int httpsPort = 47984;

                // 创建临时 ComputerDetails
                ComputerDetails tempDetails = new ComputerDetails();
                tempDetails.name = (deviceName != null && !deviceName.isEmpty()) ? deviceName : "主机";
                ComputerDetails.AddressTuple addressTuple = new ComputerDetails.AddressTuple(ip, httpPort);
                tempDetails.manualAddress = addressTuple;
                tempDetails.localAddress = addressTuple;
                tempDetails.activeAddress = addressTuple;
                tempDetails.httpsPort = httpsPort;
                tempDetails.nvidiaServer = true;

                String pairingToast = null;
                PairState pairState = PairState.FAILED;
                ComputerDetails finalDetails = tempDetails;

                // 如果有 PIN，执行配对
                if (pin != null && !pin.isEmpty()) {
                    try {
                        NvHTTP httpConn = new NvHTTP(
                                addressTuple,
                                httpsPort,
                                managerBinder.getUniqueId(),
                                null,
                                PlatformBinding.getCryptoProvider(PcView.this)
                        );

                        PairingManager pm = httpConn.getPairingManager();

                        // 使用默认 serverInfo：Sunshine 版本 7+ 使用 SHA-256
                        String defaultServerInfo = "<?xml version=\"1.0\"?>" +
                                "<root status_code=\"200\">" +
                                "<appversion>7.0.0.0</appversion>" +
                                "</root>";

                        LimeLog.info("[CloudDesktop] Starting PIN-based pairing with PIN: " + pin + ", hasPassword: " + (lanPassword != null && !lanPassword.isEmpty()));
                        pairState = pm.pair(defaultServerInfo, pin, lanPassword);
                        LimeLog.info("[CloudDesktop] Pairing result: " + pairState);

                        if (pairState == PairState.PAIRED) {
                            // 保存服务器证书
                            tempDetails.serverCert = pm.getPairedCert();

                            // 配对成功后获取服务器信息
                            try {
                                ComputerDetails polledDetails = httpConn.getComputerDetails(false);
                                if (polledDetails != null) {
                                    finalDetails = polledDetails;
                                    finalDetails.serverCert = tempDetails.serverCert;
                                    finalDetails.manualAddress = addressTuple;
                                    finalDetails.localAddress = addressTuple;
                                    finalDetails.activeAddress = addressTuple;

                                    if (polledDetails.uuid == null) {
                                        finalDetails.uuid = "cloud-desktop-" + System.currentTimeMillis() + "-" + ip;
                                    }
                                    LimeLog.info("[CloudDesktop] Got server info - Name: " + polledDetails.name + ", UUID: " + polledDetails.uuid);
                                }
                            } catch (Exception e) {
                                LimeLog.warning("[CloudDesktop] Failed to fetch server details: " + e.getMessage());
                                finalDetails.uuid = "cloud-desktop-" + System.currentTimeMillis() + "-" + ip;
                            }
                            pairingToast = "主机 " + tempDetails.name + " 已完成配对";
                        } else if (pairState == PairState.PIN_WRONG) {
                            pairingToast = "主机 " + tempDetails.name + " 配对失败：PIN 错误";
                        } else {
                            pairingToast = "主机 " + tempDetails.name + " 配对失败：" + pairState;
                        }
                    } catch (Exception e) {
                        LimeLog.severe("[CloudDesktop] Pairing error for " + ip + ": " + e.getMessage());
                        pairingToast = "主机 " + tempDetails.name + " 配对异常：" + e.getMessage();
                    }
                } else {
                    LimeLog.info("[CloudDesktop] No PIN for device " + ip + ", adding without pairing");
                    finalDetails.uuid = "cloud-desktop-" + System.currentTimeMillis() + "-" + ip;
                }

                // 添加主机到列表
                try {
                    boolean addSuccess = managerBinder.addComputerBlocking(finalDetails);
                    if (addSuccess) {
                        LimeLog.info("[CloudDesktop] Successfully added: " + finalDetails.name);
                    } else {
                        LimeLog.warning("[CloudDesktop] Failed to add: " + finalDetails.name);
                    }
                } catch (InterruptedException e) {
                    LimeLog.warning("[CloudDesktop] Interrupted while adding: " + finalDetails.name);
                    Thread.currentThread().interrupt();
                }

                // 显示配对结果
                if (pairingToast != null) {
                    final String toast = pairingToast;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PcView.this, toast, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            // 清除已使用的设备列表
            pendingDevicesJson = null;

        } catch (Exception e) {
            LimeLog.severe("[CloudDesktop] Error processing devices: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopComputerUpdates(boolean wait) {
        if (managerBinder != null) {
            if (!runningPolling) {
                return;
            }

            freezeUpdates = true;

            managerBinder.stopPolling();

            if (wait) {
                managerBinder.waitForPollingStopped();
            }

            runningPolling = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (managerBinder != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(this);

        inForeground = true;
        startComputerUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        inForeground = false;
        stopComputerUpdates(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Dialog.closeDialogs();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        stopComputerUpdates(false);

        // Call superclass
        super.onCreateContextMenu(menu, v, menuInfo);
                
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(info.position);

        // Add a header with PC status details
        menu.clearHeader();
        String headerTitle = computer.details.name + " - ";
        switch (computer.details.state)
        {
            case ONLINE:
                headerTitle += getResources().getString(R.string.pcview_menu_header_online);
                break;
            case OFFLINE:
                menu.setHeaderIcon(R.drawable.ic_pc_offline);
                headerTitle += getResources().getString(R.string.pcview_menu_header_offline);
                break;
            case UNKNOWN:
                headerTitle += getResources().getString(R.string.pcview_menu_header_unknown);
                break;
        }

        menu.setHeaderTitle(headerTitle);

        // Inflate the context menu
        // 【已移除】配对相关的菜单项 - 云端设备默认已配对
        if (computer.details.state == ComputerDetails.State.OFFLINE ||
            computer.details.state == ComputerDetails.State.UNKNOWN) {
            menu.add(Menu.NONE, WOL_ID, 1, getResources().getString(R.string.pcview_menu_send_wol));
            menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 2, getResources().getString(R.string.pcview_menu_eol));
        }
        else {
            // 在线设备直接显示操作菜单
            if (computer.details.runningGameId != 0) {
                menu.add(Menu.NONE, RESUME_ID, 1, getResources().getString(R.string.applist_menu_resume));
                menu.add(Menu.NONE, QUIT_ID, 2, getResources().getString(R.string.applist_menu_quit));
            }

            if (computer.details.nvidiaServer) {
                menu.add(Menu.NONE, GAMESTREAM_EOL_ID, 3, getResources().getString(R.string.pcview_menu_eol));
            }

            menu.add(Menu.NONE, FULL_APP_LIST_ID, 4, getResources().getString(R.string.pcview_menu_app_list));
        }

        menu.add(Menu.NONE, TEST_NETWORK_ID, 5, getResources().getString(R.string.pcview_menu_test_network));
        menu.add(Menu.NONE, DELETE_ID, 6, getResources().getString(R.string.pcview_menu_delete_pc));
        menu.add(Menu.NONE, VIEW_DETAILS_ID, 7,  getResources().getString(R.string.pcview_menu_details));
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        // For some reason, this gets called again _after_ onPause() is called on this activity.
        // startComputerUpdates() manages this and won't actual start polling until the activity
        // returns to the foreground.
        startComputerUpdates();
    }

    private void doPair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.pair_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(PcView.this, getResources().getString(R.string.pairing), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                boolean success = false;
                try {
                    // Stop updates and wait while pairing
                    stopComputerUpdates(true);

                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(PcView.this));
                    if (httpConn.getPairState() == PairState.PAIRED) {
                        // Don't display any toast, but open the app list
                        message = null;
                        success = true;
                    }
                    else {
                        final String pinStr = PairingManager.generatePinString();

                        // Spin the dialog off in a thread because it blocks
                        Dialog.displayDialog(PcView.this, getResources().getString(R.string.pair_pairing_title),
                                getResources().getString(R.string.pair_pairing_msg)+" "+pinStr+"\n\n"+
                                getResources().getString(R.string.pair_pairing_help), false);

                        PairingManager pm = httpConn.getPairingManager();

                        PairState pairState = pm.pair(httpConn.getServerInfo(true), pinStr);
                        if (pairState == PairState.PIN_WRONG) {
                            message = getResources().getString(R.string.pair_incorrect_pin);
                        }
                        else if (pairState == PairState.FAILED) {
                            if (computer.runningGameId != 0) {
                                message = getResources().getString(R.string.pair_pc_ingame);
                            }
                            else {
                                message = getResources().getString(R.string.pair_fail);
                            }
                        }
                        else if (pairState == PairState.ALREADY_IN_PROGRESS) {
                            message = getResources().getString(R.string.pair_already_in_progress);
                        }
                        else if (pairState == PairState.PAIRED) {
                            // Just navigate to the app view without displaying a toast
                            message = null;
                            success = true;

                            // Pin this certificate for later HTTPS use
                            managerBinder.getComputer(computer.uuid).serverCert = pm.getPairedCert();

                            // Invalidate reachability information after pairing to force
                            // a refresh before reading pair state again
                            managerBinder.invalidateStateForComputer(computer.uuid);
                        }
                        else {
                            // Should be no other values
                            message = null;
                        }
                    }
                } catch (UnknownHostException e) {
                    message = getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                    message = e.getMessage();
                }

                Dialog.closeDialogs();

                final String toastMessage = message;
                final boolean toastSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toastMessage != null) {
                            Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
                        }

                        if (toastSuccess) {
                            // Open the app list after a successful pairing attempt
                            doAppList(computer, true, false);
                        }
                        else {
                            // Start polling again if we're still in the foreground
                            startComputerUpdates();
                        }
                    }
                });
            }
        }).start();
    }

    private void doWakeOnLan(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.ONLINE) {
            Toast.makeText(PcView.this, getResources().getString(R.string.wol_pc_online), Toast.LENGTH_SHORT).show();
            return;
        }

        if (computer.macAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.wol_no_mac), Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    WakeOnLanSender.sendWolPacket(computer);
                    message = getResources().getString(R.string.wol_waking_msg);
                } catch (IOException e) {
                    message = getResources().getString(R.string.wol_fail);
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doUnpair(final ComputerDetails computer) {
        if (computer.state == ComputerDetails.State.OFFLINE || computer.activeAddress == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(PcView.this, getResources().getString(R.string.unpairing), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                NvHTTP httpConn;
                String message;
                try {
                    httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                            computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                            PlatformBinding.getCryptoProvider(PcView.this));
                    if (httpConn.getPairState() == PairingManager.PairState.PAIRED) {
                        httpConn.unpair();
                        if (httpConn.getPairState() == PairingManager.PairState.NOT_PAIRED) {
                            message = getResources().getString(R.string.unpair_success);
                        }
                        else {
                            message = getResources().getString(R.string.unpair_fail);
                        }
                    }
                    else {
                        message = getResources().getString(R.string.unpair_error);
                    }
                } catch (UnknownHostException e) {
                    message = getResources().getString(R.string.error_unknown_host);
                } catch (FileNotFoundException e) {
                    message = getResources().getString(R.string.error_404);
                } catch (XmlPullParserException | IOException e) {
                    message = e.getMessage();
                    e.printStackTrace();
                }

                final String toastMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PcView.this, toastMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void doAppList(ComputerDetails computer, boolean newlyPaired, boolean showHiddenGames) {
        if (computer.state == ComputerDetails.State.OFFLINE) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_pc_offline), Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(this, AppView.class);
        i.putExtra(AppView.NAME_EXTRA, computer.name);
        i.putExtra(AppView.UUID_EXTRA, computer.uuid);
        i.putExtra(AppView.NEW_PAIR_EXTRA, newlyPaired);
        i.putExtra(AppView.SHOW_HIDDEN_APPS_EXTRA, showHiddenGames);
        startActivity(i);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(info.position);
        switch (item.getItemId()) {
            // 【已移除】PAIR_ID 和 UNPAIR_ID 案例 - 不再需要PIN码配对
            // case PAIR_ID:
            //     doPair(computer.details);
            //     return true;
            //
            // case UNPAIR_ID:
            //     doUnpair(computer.details);
            //     return true;

            case WOL_ID:
                doWakeOnLan(computer.details);
                return true;

            case DELETE_ID:
                if (ActivityManager.isUserAMonkey()) {
                    LimeLog.info("Ignoring delete PC request from monkey");
                    return true;
                }
                UiHelper.displayDeletePcConfirmationDialog(this, computer.details, new Runnable() {
                    @Override
                    public void run() {
                        if (managerBinder == null) {
                            Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
                            return;
                        }
                        removeComputer(computer.details);
                    }
                }, null);
                return true;

            case FULL_APP_LIST_ID:
                doAppList(computer.details, false, true);
                return true;

            case RESUME_ID:
                if (managerBinder == null) {
                    Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
                    return true;
                }

                ServerHelper.doStart(this, new NvApp("app", computer.details.runningGameId, false), computer.details, managerBinder);
                return true;

            case QUIT_ID:
                if (managerBinder == null) {
                    Toast.makeText(PcView.this, getResources().getString(R.string.error_manager_not_running), Toast.LENGTH_LONG).show();
                    return true;
                }

                // Display a confirmation dialog first
                UiHelper.displayQuitConfirmationDialog(this, new Runnable() {
                    @Override
                    public void run() {
                        ServerHelper.doQuit(PcView.this, computer.details,
                                new NvApp("app", 0, false), managerBinder, null);
                    }
                }, null);
                return true;

            case VIEW_DETAILS_ID:
                Dialog.displayDialog(PcView.this, getResources().getString(R.string.title_details), computer.details.toString(), false);
                return true;

            case TEST_NETWORK_ID:
                ServerHelper.doNetworkTest(PcView.this);
                return true;

            case GAMESTREAM_EOL_ID:
                HelpLauncher.launchGameStreamEolFaq(PcView.this);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }
    
    private void removeComputer(ComputerDetails details) {
        managerBinder.removeComputer(details);

        new DiskAssetLoader(this).deleteAssetsForComputer(details.uuid);

        // Delete hidden games preference value
        getSharedPreferences(AppView.HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .remove(details.uuid)
                .apply();

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            if (details.equals(computer.details)) {
                // Disable or delete shortcuts referencing this PC
                shortcutHelper.disableComputerShortcut(details,
                        getResources().getString(R.string.scut_deleted_pc));

                pcGridAdapter.removeComputer(computer);
                pcGridAdapter.notifyDataSetChanged();

                if (pcGridAdapter.getCount() == 0) {
                    // Show the "Discovery in progress" view
                    noPcFoundLayout.setVisibility(View.VISIBLE);
                }

                break;
            }
        }
    }
    
    private void updateComputer(ComputerDetails details) {
        ComputerObject existingEntry = null;

        for (int i = 0; i < pcGridAdapter.getCount(); i++) {
            ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(i);

            // Check if this is the same computer
            if (details.uuid.equals(computer.details.uuid)) {
                existingEntry = computer;
                break;
            }
        }

        if (existingEntry != null) {
            // Replace the information in the existing entry
            existingEntry.details = details;
        }
        else {
            // Add a new entry
            pcGridAdapter.addComputer(new ComputerObject(details));

            // Remove the "Discovery in progress" view
            noPcFoundLayout.setVisibility(View.INVISIBLE);
        }

        // Notify the view that the data has changed
        pcGridAdapter.notifyDataSetChanged();
    }

    @Override
    public int getAdapterFragmentLayoutId() {
        return R.layout.pc_grid_view;
    }

    @Override
    public void receiveAbsListView(AbsListView listView) {
        listView.setAdapter(pcGridAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long id) {
                ComputerObject computer = (ComputerObject) pcGridAdapter.getItem(pos);
                if (computer.details.state == ComputerDetails.State.UNKNOWN ||
                    computer.details.state == ComputerDetails.State.OFFLINE) {
                    // Open the context menu if a PC is offline or refreshing
                    openContextMenu(arg1);
                } else {
                    // 【已移除】配对检查 - 云端设备默认已配对，直接打开应用列表
                    doAppList(computer.details, false, false);
                }
            }
        });
        UiHelper.applyStatusBarPadding(listView);
        registerForContextMenu(listView);
    }

    /**
     * 执行退出登录
     */
    private void performLogout() {
        Dialog.displayDialog(this, "退出登录", 
            "确定要退出登录吗？", 
            new Runnable() {
                @Override
                public void run() {
                    UserAuthManager authManager = UserAuthManager.getInstance(PcView.this);
                    
                    // 清除认证信息
                    authManager.clearLoginInfo();
                    // 清除设备直连登录状态
                    authManager.clearDeviceDirectLogin();
                    // 清除局域网模式登录状态
                    authManager.clearLanModeLogin();
                    
                    // 跳转到登录界面
                    Intent intent = new Intent(PcView.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
    }
    

    public static class ComputerObject {
        public ComputerDetails details;

        public ComputerObject(ComputerDetails details) {
            if (details == null) {
                throw new IllegalArgumentException("details must not be null");
            }
            this.details = details;
        }

        @Override
        public String toString() {
            return details.name;
        }
    }
}
