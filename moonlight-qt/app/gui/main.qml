import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3
import QtQuick.Window 2.2
import QtQuick.Controls.Material 2.2

import ComputerManager 1.0
import AutoUpdateChecker 1.0
import StreamingPreferences 1.0
import SystemProperties 1.0
import SdlGamepadKeyNavigation 1.0
import ErrorMessages 1.0

ApplicationWindow {
    id: window
    // 导出 window 对象给子页面访问
    property alias rootWindow: window
    property bool pollingActive: false
    property bool initialized: false

    // Set by SettingsView to force the back operation to pop all
    // pages except the initial view. This is required when doing
    // a retranslate() because AppView breaks for some reason.
    property bool clearOnBack: false
    visible: true
    width: 1280
    height: 600

    // Override the background color to Material 2 colors for Qt 6.5+
    // in order to improve contrast between GFE's placeholder box art
    // and the background of the app grid.
    Component.onCompleted: {
        if (SystemProperties.usesMaterial3Theme) {
            Material.background = "#303030"
        }
    }

    visibility: {
        if (SystemProperties.hasDesktopEnvironment) {
            if (StreamingPreferences.uiDisplayMode == StreamingPreferences.UI_WINDOWED) return "Windowed"
            else if (StreamingPreferences.uiDisplayMode == StreamingPreferences.UI_MAXIMIZED) return "Maximized"
            else if (StreamingPreferences.uiDisplayMode == StreamingPreferences.UI_FULLSCREEN) return "FullScreen"
        } else {
            return "FullScreen"
        }
    }

    // V4: Component definition for mode select view
    Component {
        id: modeSelectViewComponent
        ModeSelectView {
            onDirectConnectSelected: {
                navigateToConnectEntryView()
            }

            onLoginSelected: {
                navigateToLoginView()
            }

            onLanModeChanged: {
                refreshHomeView()
            }

            onServerConfigSelected: {
                navigateToServerConfigView()
            }
        }
    }

    // V4: Component definition for server config view
    Component {
        id: serverConfigViewComponent
        ServerConfigView {
            onBackRequested: {
                stackView.pop()
            }
        }
    }

    // V4: Component definition for LAN mode view (direct IP connection)
    Component {
        id: lanModeViewComponent
        LanModeView {
            id: lanModeView

            onConnectRequested: function(serverIp, password) {
                connectToLanServer(serverIp, password)
            }

            onLanModeChanged: {
                refreshHomeView()
            }
        }
    }

    // V4: Component definition for direct connect entry view
    Component {
        id: connectEntryViewComponent
        ConnectEntryView {
            id: connectEntryView

            onDirectConnectRequested: function(deviceId, devicePassword) {
                var serverIp = ComputerManager.getServerIp()
                if (serverIp === "") {
                    stopBusy()
                    showError(qsTr("请先在设置中配置服务端地址"))
                    return
                }

                var status = ComputerManager.queryDeviceStatus(serverIp, deviceId, devicePassword)
                stopBusy()

                if (!status || !status.ip) {
                    if (status && status.msg) {
                        showError(ErrorMessages.translate(status.msg))
                    } else {
                        showError(qsTr("无法连接服务端，请检查服务端地址和网络"))
                    }
                    return
                }

                // 密码校验失败（服务端返回错误信息）
                if (status.code !== undefined && status.code !== 0) {
                    showError(ErrorMessages.translate(status.msg) || qsTr("设备密码校验失败"))
                    return
                }

                ComputerManager.clearAllComputer()
                ComputerManager.setServerIp(serverIp)
                ComputerManager.setPin(status.pin || "")
                ComputerManager.addNewHostManually(status.ip)
                navigateToPcView()
            }

            onLoginRequested: {
                navigateToLoginView()
            }

            onBackToHomeRequested: {
                navigateToModeSelectView()
            }
        }
    }

    // V4: Component definition for login view
    Component {
        id: loginViewComponent
        LoginView {
            onBackToHomeRequested: {
                navigateToModeSelectView()
            }

            onLoginSuccess: {
                console.log("DEBUG main.qml: received onLoginSuccess")
                navigateToPcView()
            }
        }
    }

    // Navigation helpers
    function navigateToPcView() {
        console.log("DEBUG main.qml: navigateToPcView called, depth:", stackView.depth)

        // 直接 push PcView，不检查是否已存在
        // 这样可以避免 objectName 检查的问题
        console.log("DEBUG main.qml: pushing PcView.qml")
        stackView.push("qrc:/gui/PcView.qml")

        console.log("DEBUG main.qml: after push, depth:", stackView.depth)
        clearOnBack = true
    }

    function navigateToLoginView() {
        stackView.replace(loginViewComponent)
        clearOnBack = false
    }

    function navigateToModeSelectView() {
        stackView.replace(modeSelectViewComponent)
        clearOnBack = false
    }

    function navigateToConnectEntryView() {
        stackView.replace(connectEntryViewComponent)
        clearOnBack = false
    }

    function navigateToLanModeView() {
        stackView.replace(lanModeViewComponent)
        clearOnBack = false
    }

    function navigateToServerConfigView() {
        stackView.push(serverConfigViewComponent)
        clearOnBack = false
    }

    // 根据 lanFixedPin 配置刷新首页视图
    function refreshHomeView() {
        if (StreamingPreferences.lanFixedPin) {
            stackView.replace(lanModeViewComponent)
        } else {
            stackView.replace(modeSelectViewComponent)
        }
    }

    // Connect to Sunshine server in LAN mode (no management server needed)
    function connectToLanServer(serverIp, password) {
        console.log("DEBUG main.qml: connectToLanServer called with IP:", serverIp)

        // Validate IP format (basic validation)
        var ipPattern = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
        if (!ipPattern.test(serverIp)) {
            var currentItem = stackView.currentItem
            if (currentItem && currentItem.showError) {
                currentItem.showError(qsTr("请输入有效的 IP 地址"))
            }
            return
        }

        // Validate password
        if (!password || password.trim() === "") {
            var currentItem2 = stackView.currentItem
            if (currentItem2 && currentItem2.showError) {
                currentItem2.showError(qsTr("请输入访问密码"))
            }
            return
        }

        // Password will be verified during pairing process
        // No separate verification API call needed

        // Clear existing computers
        ComputerManager.clearAllComputer()

        // Save to lanServerIp (separate from serverIp which is for management server)
        ComputerManager.setLanServerIp(serverIp)

        // Save password to configuration
        StreamingPreferences.lanPassword = password
        StreamingPreferences.lanFixedPin = true  // Enable LAN fixed PIN mode
        StreamingPreferences.save()

        // Reload LAN fixed PIN mode in ComputerManager
        ComputerManager.reloadLanFixedPinMode()

        // Add the host manually
        ComputerManager.addNewHostManually(serverIp)

        // Navigate to PC view
        navigateToPcView()
    }

    // V4: Navigate to DeviceSelectView (ID input + online device list)
    function navigateToDeviceSelectView(serverIp, loginData) {
        var component = Qt.createComponent("qrc:/gui/DeviceSelectView.qml")
        var view = component.createObject(stackView, {m_ServerIp: serverIp})
        view.back.connect(function() {
            stackView.pop()
        })
        view.deviceConnected.connect(function() {
            navigateToPcView()
        })
        stackView.push(view)
        clearOnBack = false
    }

    // This configures the maximum width of the singleton attached QML ToolTip. If left unconstrained,
    // it will never insert a line break and just extend on forever.
    ToolTip.toolTip.contentWidth: ToolTip.toolTip.implicitContentWidth < 400 ? ToolTip.toolTip.implicitContentWidth : 400

    function goBack() {
        if (clearOnBack) {
            // Pop all items except the first one
            stackView.pop(null)
            clearOnBack = false
        }
        else {
            stackView.pop()
        }
    }

    // 从 SettingsView 返回时强制只 pop 一层，回到进入 SettingsView 之前的上一级页面
    // （如 PcView），避免在 clearOnBack=true 时被错误地一次性弹空栈而回到登录/连接页
    function goBackToPrevious() {
        if (stackView.currentItem && qmltypeof(stackView.currentItem, "SettingsView")) {
            clearOnBack = false
            stackView.pop()
        }
        else {
            goBack()
        }
    }

    StackView {
        id: stackView
        initialItem: StreamingPreferences.lanFixedPin ? lanModeViewComponent : modeSelectViewComponent
        anchors.fill: parent
        focus: true

        onCurrentItemChanged: {
            // Ensure focus travels to the next view when going back
            if (currentItem) {
                currentItem.forceActiveFocus()
            }
        }

        Keys.onEscapePressed: {
            if (depth > 1) {
                goBackToPrevious()
            }
            else {
                quitConfirmationDialog.open()
            }
        }

        Keys.onBackPressed: {
            if (depth > 1) {
                goBackToPrevious()
            }
            else {
                quitConfirmationDialog.open()
            }
        }

        Keys.onMenuPressed: {
            settingsButton.clicked()
        }

        // This is a keypress we've reserved for letting the
        // SdlGamepadKeyNavigation object tell us to show settings
        // when Menu is consumed by a focused control.
        Keys.onHangupPressed: {
            settingsButton.clicked()
        }
    }

    // This timer keeps us polling for 5 minutes of inactivity
    // to allow the user to work with Moonlight on a second display
    // while dealing with configuration issues. This will ensure
    // machines come online even if the input focus isn't on Moonlight.
    Timer {
        id: inactivityTimer
        interval: 5 * 60000
        onTriggered: {
            if (!active && pollingActive) {
                ComputerManager.stopPollingAsync()
                pollingActive = false
            }
        }
    }

    onVisibleChanged: {
        // When we become invisible while streaming is going on,
        // stop polling immediately.
        if (!visible) {
            inactivityTimer.stop()

            if (pollingActive) {
                ComputerManager.stopPollingAsync()
                pollingActive = false
            }
        }
        else if (active) {
            // When we become visible and active again, start polling
            inactivityTimer.stop()

            // Restart polling if it was stopped
            if (!pollingActive) {
                ComputerManager.startPolling()
                pollingActive = true
            }
        }
    }

    onActiveChanged: {
        if (active) {
            // Stop the inactivity timer
            inactivityTimer.stop()

            // Restart polling if it was stopped
            if (!pollingActive) {
                ComputerManager.startPolling()
                pollingActive = true
            }
        }
        else {
            // Start the inactivity timer to stop polling
            // if focus does not return within a few minutes.
            inactivityTimer.restart()
        }
    }

    // BUG: Using onAfterSynchronizing: here causes very strange
    // failures on Linux. Many shaders fail to compile and we
    // eventually segfault deep inside the Qt OpenGL code.
    onAfterRendering: {
        // We use this callback to trigger dialog display because
        // it only happens once the window is fully constructed.
        // Doing it earlier can lead to the dialog appearing behind
        // the window or otherwise without input focus.
        if (!initialized) {
            // Set initialized before calling anything else, because
            // pumping the event loop can cause us to get another
            // onAfterRendering call and potentially reenter this code.
            initialized = true;

            if (SystemProperties.isWow64) {
                wow64Dialog.open()
            }
            else if (!SystemProperties.hasHardwareAcceleration) {
                if (SystemProperties.isRunningXWayland) {
                    xWaylandDialog.open()
                }
                else {
                    noHwDecoderDialog.open()
                }
            }

            if (SystemProperties.unmappedGamepads) {
                unmappedGamepadDialog.unmappedGamepads = SystemProperties.unmappedGamepads
                unmappedGamepadDialog.open()
            }
        }
    }

    // Workaround for lack of instanceof in Qt 5.9.
    //
    // Based on https://stackoverflow.com/questions/13923794/how-to-do-a-is-a-typeof-or-instanceof-in-qml
    function qmltypeof(obj, className) { // QtObject, string -> bool
        // className plus "(" is the class instance without modification
        // className plus "_QML" is the class instance with user-defined properties
        var str = obj.toString();
        return str.startsWith(className + "(") || str.startsWith(className + "_QML");
    }

    function navigateTo(url, objectType)
    {
        var existingItem = stackView.find(function(item, index) {
            return qmltypeof(item, objectType)
        })

        if (existingItem !== null) {
            // Pop to the existing item
            stackView.pop(existingItem)
        }
        else {
            // Create a new item
            stackView.push(url)
        }
    }

    header: ToolBar {
        id: toolBar
        height: 60
        anchors.topMargin: 5
        anchors.bottomMargin: 5
        visible: {
            var current = stackView.currentItem
            if (!current) return false
            // Hide toolbar on login page
            var name = current.objectName || ""
            return name !== qsTr("登录")
        }

        Label {
            id: titleLabel
            visible: toolBar.width > 700
            anchors.fill: parent
            text: stackView.currentItem.objectName
            font.pointSize: 20
            elide: Label.ElideRight
            horizontalAlignment: Qt.AlignHCenter
            verticalAlignment: Qt.AlignVCenter
        }

        RowLayout {
            spacing: 10
            anchors.leftMargin: 10
            anchors.rightMargin: 10
            anchors.fill: parent

            NavigableToolButton {
                // Only make the button visible if the user has navigated somewhere.
                visible: stackView.depth > 1

                iconSource: "qrc:/res/arrow_left.svg"

                onClicked: goBackToPrevious()

                Keys.onDownPressed: {
                    stackView.currentItem.forceActiveFocus(Qt.TabFocus)
                }
            }

            // This label will appear when the window gets too small and
            // we need to ensure the toolbar controls don't collide
            Label {
                id: titleRowLabel
                font.pointSize: titleLabel.font.pointSize
                elide: Label.ElideRight
                horizontalAlignment: Qt.AlignHCenter
                verticalAlignment: Qt.AlignVCenter
                Layout.fillWidth: true

                // We need this label to always be visible so it can occupy
                // the remaining space in the RowLayout. To "hide" it, we
                // just set the text to empty string.
                text: !titleLabel.visible ? stackView.currentItem.objectName : ""
            }

            // 不显示右上角版本号
            // Label {
            //     id: versionLabel
            //     visible: qmltypeof(stackView.currentItem, "SettingsView")
            //     text: qsTr("Version %1").arg(SystemProperties.versionString)
            //     font.pointSize: 12
            //     horizontalAlignment: Qt.AlignRight
            //     verticalAlignment: Qt.AlignVCenter
            // }

            // NavigableToolButton {
            //     id: discordButton
            //     visible: SystemProperties.hasBrowser &&
            //              qmltypeof(stackView.currentItem, "SettingsView")

            //     iconSource: "qrc:/res/discord.svg"

            //     ToolTip.delay: 1000
            //     ToolTip.timeout: 3000
            //     ToolTip.visible: hovered
            //     ToolTip.text: qsTr("Join our community on Discord")

            //     // TODO need to make sure browser is brought to foreground.
            //     onClicked: Qt.openUrlExternally("https://moonlight-stream.org/discord");

            //     Keys.onDownPressed: {
            //         stackView.currentItem.forceActiveFocus(Qt.TabFocus)
            //     }
            // }

            NavigableToolButton {
                id: addPcButton
                visible: stackView.currentItem && stackView.currentItem.objectName === qsTr("Computers")

                iconSource:  "qrc:/res/ic_add_to_queue_white_48px.svg"

                ToolTip.delay: 1000
                ToolTip.timeout: 3000
                ToolTip.visible: hovered
                ToolTip.text: qsTr("Add PC via Device ID") + (newPcShortcut.nativeText ? (" ("+newPcShortcut.nativeText+")") : "")

                Shortcut {
                    id: newPcShortcut
                    sequence: StandardKey.New
                    onActivated: addPcButton.clicked()
                }

                onClicked: {
                    // V4: Open DeviceSelectView for ID-based device discovery
                    var component = Qt.createComponent("qrc:/gui/DeviceSelectView.qml")
                    var dialog = component.createObject(window, {})
                    dialog.visible = true
                }

                Keys.onDownPressed: {
                    stackView.currentItem.forceActiveFocus(Qt.TabFocus)
                }
            }

            NavigableToolButton {
                property string browserUrl: ""

                id: updateButton

                iconSource: "qrc:/res/update.svg"

                ToolTip.delay: 1000
                ToolTip.timeout: 3000
                ToolTip.visible: hovered || visible

                // Invisible until we get a callback notifying us that
                // an update is available
                visible: false

                onClicked: {
                    if (SystemProperties.hasBrowser) {
                        Qt.openUrlExternally(browserUrl);
                    }
                }

                function updateAvailable(version, url)
                {
                    ToolTip.text = qsTr("Update available for Moonlight: Version %1").arg(version)
                    updateButton.browserUrl = url
                    // 不显示更新按钮
                    // updateButton.visible = true
                }

                Component.onCompleted: {
                    AutoUpdateChecker.onUpdateAvailable.connect(updateAvailable)
                    AutoUpdateChecker.start()
                }

                Keys.onDownPressed: {
                    stackView.currentItem.forceActiveFocus(Qt.TabFocus)
                }
            }

            // NavigableToolButton {
            //     id: helpButton
            //     visible: SystemProperties.hasBrowser

            //     iconSource: "qrc:/res/question_mark.svg"

            //     ToolTip.delay: 1000
            //     ToolTip.timeout: 3000
            //     ToolTip.visible: hovered
            //     ToolTip.text: qsTr("Help") + (helpShortcut.nativeText ? (" ("+helpShortcut.nativeText+")") : "")

            //     Shortcut {
            //         id: helpShortcut
            //         sequence: StandardKey.HelpContents
            //         onActivated: helpButton.clicked()
            //     }

            //     // TODO need to make sure browser is brought to foreground.
            //     onClicked: Qt.openUrlExternally("https://github.com/moonlight-stream/moonlight-docs/wiki/Setup-Guide");

            //     Keys.onDownPressed: {
            //         stackView.currentItem.forceActiveFocus(Qt.TabFocus)
            //     }
            // }

            NavigableToolButton {
                // TODO: Implement gamepad mapping then unhide this button
                visible: false

                ToolTip.delay: 1000
                ToolTip.timeout: 3000
                ToolTip.visible: hovered
                ToolTip.text: qsTr("Gamepad Mapper")

                iconSource: "qrc:/res/ic_videogame_asset_white_48px.svg"

                onClicked: navigateTo("qrc:/gui/GamepadMapper.qml", "GamepadMapper")

                Keys.onDownPressed: {
                    stackView.currentItem.forceActiveFocus(Qt.TabFocus)
                }
            }

            NavigableToolButton {
                id: settingsButton

                visible: {
                    var current = stackView.currentItem
                    if (!current) return false
                    var name = current.objectName || ""
                    // 在首页(连接方式)、设备ID连接输入页(连接)、设备列表页(选择设备)和服务端配置页不显示设置按钮
                    return name !== qsTr("连接方式") && name !== qsTr("连接") && name !== qsTr("选择设备") && name !== qsTr("服务端配置")
                }

                iconSource:  "qrc:/res/settings.svg"

                onClicked: navigateTo("qrc:/gui/SettingsView.qml", "SettingsView")

                Keys.onDownPressed: {
                    stackView.currentItem.forceActiveFocus(Qt.TabFocus)
                }

                Shortcut {
                    id: settingsShortcut
                    sequence: StandardKey.Preferences
                    onActivated: settingsButton.clicked()
                }

                ToolTip.delay: 1000
                ToolTip.timeout: 3000
                ToolTip.visible: hovered
                ToolTip.text: qsTr("Settings") + (settingsShortcut.nativeText ? (" ("+settingsShortcut.nativeText+")") : "")
            }
        }
    }

    ErrorMessageDialog {
        id: noHwDecoderDialog
        text: qsTr("No functioning hardware accelerated video decoder was detected by Moonlight. " +
                   "Your streaming performance may be severely degraded in this configuration.")
        helpText: qsTr("Click the Help button for more information on solving this problem.")
        helpUrl: "https://github.com/moonlight-stream/moonlight-docs/wiki/Fixing-Hardware-Decoding-Problems"
    }

    ErrorMessageDialog {
        id: xWaylandDialog
        text: qsTr("Hardware acceleration doesn't work on XWayland. Continuing on XWayland may result in poor streaming performance. " +
                   "Try running with QT_QPA_PLATFORM=wayland or switch to X11.")
        helpText: qsTr("Click the Help button for more information.")
        helpUrl: "https://github.com/moonlight-stream/moonlight-docs/wiki/Fixing-Hardware-Decoding-Problems"
    }

    NavigableMessageDialog {
        id: wow64Dialog
        standardButtons: Dialog.Ok | Dialog.Cancel
        text: qsTr("This version of Moonlight isn't optimized for your PC. Please download the '%1' version of Moonlight for the best streaming performance.").arg(SystemProperties.friendlyNativeArchName)
        onAccepted: {
            Qt.openUrlExternally("https://github.com/moonlight-stream/moonlight-qt/releases");
        }
    }

    ErrorMessageDialog {
        id: unmappedGamepadDialog
        property string unmappedGamepads : ""
        text: qsTr("Moonlight detected gamepads without a mapping:") + "\n" + unmappedGamepads
        helpTextSeparator: "\n\n"
        helpText: qsTr("Click the Help button for information on how to map your gamepads.")
        helpUrl: "https://github.com/moonlight-stream/moonlight-docs/wiki/Gamepad-Mapping"
    }

    // This dialog appears when quitting via keyboard or gamepad button
    NavigableMessageDialog {
        id: quitConfirmationDialog
        standardButtons: Dialog.Yes | Dialog.No
        text: qsTr("Are you sure you want to quit?")
        // For keyboard/gamepad navigation
        onAccepted: Qt.quit()
    }

    // HACK: This belongs in StreamSegue but keeping a dialog around after the parent
    // dies can trigger bugs in Qt 5.12 that cause the app to crash. For now, we will
    // host this dialog in a QML component that is never destroyed.
    //
    // To repro: Start a stream, cut the network connection to trigger the "Connection
    // terminated" dialog, wait until the app grid times out back to the PC grid, then
    // try to dismiss the dialog.
    ErrorMessageDialog {
        id: streamSegueErrorDialog

        property bool quitAfter: false

        onClosed: {
            if (quitAfter) {
                Qt.quit()
            }

            // StreamSegue assumes its dialog will be re-created each time we
            // start streaming, so fake it by wiping out the text each time.
            text = ""
        }
    }

    NavigableDialog {
        id: addPcDialog
        property string label: qsTr("Enter the IP address of your host PC:")

        standardButtons: Dialog.Ok | Dialog.Cancel

        onOpened: {
            // Force keyboard focus on the textbox so keyboard navigation works
            editText.forceActiveFocus()
        }

        onClosed: {
            editText.clear()
        }

        onAccepted: {
            if (editText.text) {
                ComputerManager.addNewHostManually(editText.text.trim())
            }
        }

        ColumnLayout {
            Label {
                text: addPcDialog.label
                font.bold: true
            }

            TextField {
                id: editText
                Layout.fillWidth: true
                focus: true

                Keys.onReturnPressed: {
                    addPcDialog.accept()
                }

                Keys.onEnterPressed: {
                    addPcDialog.accept()
                }
            }
        }
    }
}
