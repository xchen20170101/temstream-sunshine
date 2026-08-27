import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3

import ComputerManager 1.0
import ComputerModel 1.0
import ErrorMessages 1.0
import StreamingPreferences 1.0

Page {
    id: loginPage
    title: qsTr("登录")
    objectName: qsTr("登录")

    signal loginSuccess()
    signal backToHomeRequested()

    Component.onCompleted: {
        if (StreamingPreferences.rememberUsername && StreamingPreferences.rememberedUsername !== "") {
            usernameInput.text = StreamingPreferences.rememberedUsername
            rememberCheckbox.checked = true
        }
    }

    function getRootWindow() {
        var sv = StackView.view
        if (sv) {
            var window = sv.parent
            if (window && typeof window.navigateToPcView === "function") {
                return window
            }
        }
        return null
    }

    Rectangle {
        anchors.fill: parent
        color: "#020511"

        Image {
            anchors.fill: parent
            source: "qrc:/res/login_background.png"
            fillMode: Image.PreserveAspectCrop
        }

        Rectangle {
            anchors.centerIn: parent
            width: 430
            height: 560
            radius: 18
            color: "#10172dcc"
            border.color: "#356cff"
            border.width: 1

            Column {
                anchors.fill: parent
                anchors.margins: 45
                spacing: 22

                Image {
                    width: 90
                    height: 90
                    anchors.horizontalCenter: parent.horizontalCenter
                    source: "qrc:/res/login_logo.svg"
                }

                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: qsTr("TEMSTREAM")
                    font.pixelSize: 28
                    font.bold: true
                    color: "white"
                }

                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: qsTr("随时随地 · 畅享串流")
                    color: "#8ea5c9"
                    font.pixelSize: 16
                }

                TextField {
                    id: usernameInput
                    placeholderText: qsTr("输入用户名")
                    height: 50
                    width: parent.width
                    font.pixelSize: 14
                }

                TextField {
                    id: passwordInput
                    placeholderText: qsTr("输入密码")
                    echoMode: TextInput.Password
                    height: 50
                    width: parent.width
                    font.pixelSize: 14

                    Keys.onReturnPressed: loginButton.clicked()
                    Keys.onEnterPressed: loginButton.clicked()
                }

                CheckBox {
                    id: rememberCheckbox
                    text: qsTr("记住用户名")
                    checked: false
                    font.pixelSize: 12

                    onCheckedChanged: {
                        StreamingPreferences.rememberUsername = checked
                        StreamingPreferences.save()
                    }
                }

                Row {
                    width: parent.width
                    spacing: 12

                    Button {
                        id: loginButton
                        width: (parent.width - 12) * 2 / 3
                        height: 50
                        text: qsTr("登录")
                        enabled: !loginIndicator.running

                        onClicked: {
                            var username = usernameInput.text.trim()
                            var password = passwordInput.text

                            if (!loginSuccessful(username, password)) {
                                return
                            }

                            loginIndicator.running = true
                            loginButton.enabled = false
                            sendRestfulRequest(username, password)
                        }

                        background: Rectangle {
                            radius: 10
                            gradient: Gradient {
                                GradientStop { position: 0; color: "#258cff" }
                                GradientStop { position: 1; color: "#742cff" }
                            }
                        }

                        contentItem: Text {
                            text: parent.text
                            color: "white"
                            font.pixelSize: 16
                            horizontalAlignment: Text.AlignHCenter
                            verticalAlignment: Text.AlignVCenter
                        }
                    }

                    Button {
                        text: qsTr("返回首页")
                        width: (parent.width - 12) * 1 / 3
                        height: 50
                        enabled: !loginIndicator.running

                        onClicked: backToHomeRequested()

                        background: Rectangle {
                            color: parent.hovered ? "#4A4A4A" : "#3D3D3D"
                            radius: 10
                        }

                        contentItem: Text {
                            text: parent.text
                            color: "#AAAAAA"
                            font.pixelSize: 14
                            horizontalAlignment: Text.AlignHCenter
                            verticalAlignment: Text.AlignVCenter
                        }
                    }
                }

                RowLayout {
                    Layout.fillWidth: true
                    Layout.alignment: Qt.AlignHCenter
                    BusyIndicator {
                        id: loginIndicator
                        running: false
                        visible: running
                    }
                }
            }
        }
    }

    ErrorMessageDialog {
        id: errorDialog
        standardButtons: Dialog.Ok
    }

    function loginSuccessful(username, password) {
        if (username === "" || password === "") {
            errorDialog.text = qsTr("用户名或密码不能为空")
            errorDialog.open()
            return false
        }
        return true
    }

    function navigateToMain() {
        console.log("DEBUG LoginView: emit loginSuccess signal")
        loginSuccess()
    }

    function navigateToDeviceSelect(serverIp, loginData) {
        var root = getRootWindow()
        if (root) {
            root.navigateToDeviceSelectView(serverIp, loginData)
        } else {
            errorDialog.text = qsTr("无法打开设备选择页面")
            errorDialog.open()
            navigateToMain()
        }
    }

    function sendRestfulRequest(username, password) {
        var serverIp = ComputerManager.getServerIp()
        if (serverIp === "") {
            loginIndicator.running = false
            loginButton.enabled = true
            errorDialog.text = qsTr("未配置服务端IP")
            errorDialog.open()
            return
        }

        var url = "http://" + serverIp + ":8082/api/stream/v1/client/login"
        var xhr = new XMLHttpRequest()
        xhr.open("POST", url, true)
        xhr.setRequestHeader("Content-Type", "application/json")

        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200) {
                    var resp = JSON.parse(xhr.responseText)
                    if (resp.code === 0) {
                        if (rememberCheckbox.checked) {
                            StreamingPreferences.rememberedUsername = usernameInput.text.trim()
                        }
                        StreamingPreferences.save()

                        var devices = resp.data.devices || []

                        ComputerManager.setServerIp(serverIp)
                        ComputerManager.clearAllComputer()

                        if (devices.length === 0) {
                            loginIndicator.running = false
                            loginButton.enabled = true
                            navigateToMain()
                            return
                        }

                        var lastPin = devices[devices.length - 1].pin || ""
                        ComputerManager.setPin(lastPin)

                        for (var i = 0; i < devices.length; i++) {
                            (function(index) {
                                var dev = devices[index]
                                var ip = dev.ip || ""
                                if (ip) {
                                    var deviceName = dev.deviceName || ""
                                    // V4: 传递每个设备对应的 PIN
                                    var pin = dev.pin || ""
                                    ComputerManager.addNewHostManually(ip, deviceName, pin)
                                }
                            })(i)
                        }

                        loginIndicator.running = false
                        loginButton.enabled = true
                        navigateToMain()
                    } else {
                        loginIndicator.running = false
                        loginButton.enabled = true
                        errorDialog.text = ErrorMessages.translate(resp.msg) || qsTr("登录失败")
                        errorDialog.open()
                    }
                } else {
                    loginIndicator.running = false
                    loginButton.enabled = true
                    errorDialog.text = qsTr("网络错误，请检查后重试")
                    errorDialog.open()
                }
            }
        }

        xhr.send(JSON.stringify({username: username, password: password}))
    }
}
