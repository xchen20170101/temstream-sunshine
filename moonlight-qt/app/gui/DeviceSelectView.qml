import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3

import ComputerManager 1.0
import ErrorMessages 1.0

Page {
    id: root
    property string m_ServerIp: ""
    property string m_DeviceId: ""
    property string m_DeviceIp: ""
    property string m_DevicePin: ""
    property string m_DeviceName: ""

    title: qsTr("选择设备")
    objectName: qsTr("选择设备")

    // 加载在线设备列表
    function loadOnlineDevices() {
        busyIndicator.running = true
        var devices = ComputerManager.fetchOnlineDevices(m_ServerIp)
        onlineDevicesList.clear()
        for (var i = 0; i < devices.length; i++) {
            var dev = devices[i]
            onlineDevicesList.append({
                deviceId: dev.deviceId || "",
                name: dev.name || "Unknown",
                ip: dev.ip || "",
                status: dev.status || "Unknown"
            })
        }
        busyIndicator.running = false
    }

    // 通过ID查询设备状态
    function queryDeviceById(deviceId) {
        if (!m_ServerIp) {
            errorDialog.text = qsTr("未配置服务端IP")
            errorDialog.open()
            return null
        }
        if (!deviceId || deviceId.length === 0) {
            errorDialog.text = qsTr("请输入设备ID")
            errorDialog.open()
            return null
        }

        var result = ComputerManager.queryDeviceStatus(m_ServerIp, deviceId)
        if (result && result.ip) {
            return result
        } else {
            var msg = (result && result.msg)
                      ? ErrorMessages.translate(result.msg)
                      : qsTr("设备不存在或离线")
            errorDialog.text = msg
            errorDialog.open()
            return null
        }
    }

    // 连接指定设备
    function connectDevice(deviceId, ip, name, pin) {
        m_DeviceId = deviceId
        m_DeviceIp = ip
        m_DevicePin = pin || ""
        m_DeviceName = name || ""

        loadingIndicator.running = true
        loadingOverlay.visible = true

        Qt.callLater(function() {
            try {
                ComputerManager.clearAllComputer()
                ComputerManager.setPin(m_DevicePin)
                ComputerManager.addNewHostManually(m_DeviceIp)

                loadingIndicator.running = false
                loadingOverlay.visible = false

                // 触发 PcView 刷新（通过重新导航）
                // 通知 main.qml 跳转到 PcView
                Qt.callLater(function() {
                    deviceConnected()
                })
            } catch(e) {
                loadingIndicator.running = false
                loadingOverlay.visible = false
                errorDialog.text = qsTr("连接失败:") + e
                errorDialog.open()
            }
        })
    }

    Rectangle {
        anchors.fill: parent
        color: "#2D2D2D"

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 20
            spacing: 16

            // 标题
            Label {
                text: qsTr("输入设备 ID 连接")
                font.pointSize: 18
                font.bold: true
                color: "white"
                Layout.alignment: Qt.AlignHCenter
            }

            // ID 输入区域
            Rectangle {
                Layout.fillWidth: true
                color: "#3D3D3D"
                radius: 8
                height: 60

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 10
                    spacing: 10

                    TextField {
                        id: idInput
                        placeholderText: qsTr("输入 8 位设备 ID")
                        font.pointSize: 16
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        horizontalAlignment: TextInput.AlignHCenter
                        verticalAlignment: TextInput.AlignVCenter

                        validator: RegExpValidator {
                            regExp: /[0-9]*/
                        }

                        maximumLength: 8

                        onTextChanged: {
                            text = text.replace(/[^0-9]/g, "")
                        }

                        Keys.onReturnPressed: connectByIdButton.clicked()
                        Keys.onEnterPressed: connectByIdButton.clicked()
                    }

                    Button {
                        id: connectByIdButton
                        text: qsTr("连接")
                        Layout.fillHeight: true
                        Layout.preferredWidth: 80

                        onClicked: {
                            if (idInput.text.length !== 8) {
                                errorDialog.text = qsTr("请输入完整的 8 位设备 ID")
                                errorDialog.open()
                                return
                            }
                            var status = queryDeviceById(idInput.text)
                            if (status) {
                                connectDevice(idInput.text, status.ip, status.name, status.pin || "")
                            }
                        }
                    }
                }
            }

            // 分隔线
            Rectangle {
                Layout.fillWidth: true
                height: 1
                color: "#555555"

                Label {
                    text: qsTr("在线设备列表")
                    font.pointSize: 12
                    color: "#AAAAAA"
                    anchors.left: parent.left
                    anchors.verticalCenter: parent.verticalCenter
                }
            }

            // 刷新按钮
            RowLayout {
                Layout.fillWidth: true

                Button {
                    text: qsTr("刷新")
                    Layout.preferredWidth: 100

                    onClicked: {
                        loadOnlineDevices()
                    }
                }

                BusyIndicator {
                    id: busyIndicator
                    running: false
                    Layout.preferredWidth: 24
                    Layout.preferredHeight: 24
                }

                Item { Layout.fillWidth: true }
            }

            // 设备列表
            ListModel {
                id: onlineDevicesList
            }

            ListView {
                id: deviceListView
                Layout.fillWidth: true
                Layout.fillHeight: true
                model: onlineDevicesList
                clip: true

                ScrollBar.vertical: ScrollBar {
                    active: true
                }

                spacing: 8

                delegate: Rectangle {
                    width: deviceListView.width
                    height: 72
                    color: "#3D3D3D"
                    radius: 6

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 12
                        spacing: 12

                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 4

                            Label {
                                text: model.name || "Unknown"
                                font.pointSize: 14
                                font.bold: true
                                color: "white"
                            }

                            Label {
                                text: "ID: " + (model.deviceId || "?")
                                font.pointSize: 11
                                color: "#AAAAAA"
                            }

                            Label {
                                text: model.ip || ""
                                font.pointSize: 11
                                color: "#888888"
                            }
                        }

                        Rectangle {
                            width: 10
                            height: 10
                            radius: 5
                            color: model.status === "Online" ? "#4CAF50" : "#9E9E9E"
                            Layout.alignment: Qt.AlignVCenter
                        }

                        Button {
                            text: qsTr("连接")
                            Layout.alignment: Qt.AlignVCenter
                            Layout.preferredWidth: 70

                            onClicked: {
                                if (!model.ip) {
                                    errorDialog.text = qsTr("设备IP无效")
                                    errorDialog.open()
                                    return
                                }
                                var status = ComputerManager.queryDeviceStatus(m_ServerIp, model.deviceId)
                                if (!status || status.code === undefined || status.code !== 0) {
                                    var msg = (status && status.msg)
                                              ? ErrorMessages.translate(status.msg)
                                              : qsTr("设备状态查询失败")
                                    errorDialog.text = msg
                                    errorDialog.open()
                                    return
                                }
                                var pin = status.pin || ""
                                connectDevice(model.deviceId, model.ip, model.name, pin)
                            }
                        }
                    }

                    MouseArea {
                        anchors.fill: parent
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (!model.ip) {
                                errorDialog.text = qsTr("设备IP无效")
                                errorDialog.open()
                                return
                            }
                            var status = ComputerManager.queryDeviceStatus(m_ServerIp, model.deviceId)
                            if (!status || status.code === undefined || status.code !== 0) {
                                var msg = (status && status.msg)
                                          ? ErrorMessages.translate(status.msg)
                                          : qsTr("设备状态查询失败")
                                errorDialog.text = msg
                                errorDialog.open()
                                return
                            }
                            var pin = status.pin || ""
                            connectDevice(model.deviceId, model.ip, model.name, pin)
                        }
                    }
                }

                Label {
                    anchors.centerIn: parent
                    visible: onlineDevicesList.count === 0
                    text: qsTr("暂无在线设备")
                    font.pointSize: 14
                    color: "#888888"
                }
            }

            // 返回按钮
            Button {
                text: qsTr("返回")
                Layout.alignment: Qt.AlignHCenter
                Layout.preferredWidth: 120

                onClicked: {
                    root.back()
                }
            }
        }

        // 加载遮罩
        Rectangle {
            id: loadingOverlay
            anchors.fill: parent
            color: "#80000000"
            visible: false

            ColumnLayout {
                anchors.centerIn: parent
                spacing: 12

                BusyIndicator {
                    id: loadingIndicator
                    running: false
                    Layout.alignment: Qt.AlignHCenter
                }

                Label {
                    text: qsTr("正在连接...")
                    font.pointSize: 14
                    color: "white"
                    Layout.alignment: Qt.AlignHCenter
                }
            }
        }
    }

    ErrorMessageDialog {
        id: errorDialog
        standardButtons: Dialog.Ok
    }

    // 初始化：加载设备列表
    Component.onCompleted: {
        if (m_ServerIp) {
            loadOnlineDevices()
        }
    }

    // 提供给外部的回调
    signal back()
    signal deviceConnected()
}
