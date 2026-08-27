import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3

import ComputerManager 1.0
import StreamingPreferences 1.0

Page {
    id: root
    title: qsTr("连接")
    objectName: qsTr("连接")

    signal connectRequested(string serverIp, string password)
    signal lanModeChanged()

    Rectangle {
        anchors.fill: parent
        color: "#020511"

        Image {
            anchors.fill: parent
            source: "qrc:/res/login_background.png"
            fillMode: Image.PreserveAspectCrop
        }

        // 右上角局域网模式复选框
        CheckBox {
            id: lanModeCheck
            anchors.top: parent.top
            anchors.right: parent.right
            anchors.margins: 20
            text: qsTr("局域网模式")
            font.pointSize: 12
            checked: StreamingPreferences.lanFixedPin
            onClicked: {
                if (checked !== StreamingPreferences.lanFixedPin) {
                    StreamingPreferences.lanFixedPin = checked
                    StreamingPreferences.save()
                    ComputerManager.reloadLanFixedPinMode()
                    lanModeChanged()
                }
            }

            ToolTip.delay: 1000
            ToolTip.timeout: 5000
            ToolTip.visible: hovered
            ToolTip.text: qsTr("勾选后将只能在局域网下使用。")
        }

        Rectangle {
            anchors.centerIn: parent
            width: 430
            height: 540
            radius: 18
            color: "#10172dcc"
            border.color: "#356cff"
            border.width: 1

            Column {
                anchors.fill: parent
                anchors.margins: 45
                spacing: 18

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
                    id: serverIpInput
                    width: parent.width
                    height: 50
                    placeholderText: qsTr("输入服务端 IP")
                    font.pixelSize: 14

                    validator: RegExpValidator {
                        regExp: /^[0-9.]*$/
                    }

                    onTextChanged: {
                        text = text.replace(/[^0-9.]/g, "")
                    }

                    Keys.onReturnPressed: connectButton.clicked()
                    Keys.onEnterPressed: connectButton.clicked()
                }

                TextField {
                    id: passwordInput
                    width: parent.width
                    height: 50
                    placeholderText: qsTr("输入访问密码")
                    font.pixelSize: 14
                    echoMode: TextInput.Password

                    Keys.onReturnPressed: connectButton.clicked()
                    Keys.onEnterPressed: connectButton.clicked()
                }

                Button {
                    id: connectButton
                    width: parent.width
                    height: 50
                    enabled: !busyIndicator.running

                    onClicked: {
                        var ip = serverIpInput.text.trim()
                        var password = passwordInput.text

                        if (ip === "") {
                            errorDialog.text = qsTr("请输入服务端 IP")
                            errorDialog.open()
                            return
                        }

                        if (password === "") {
                            errorDialog.text = qsTr("请输入访问密码")
                            errorDialog.open()
                            return
                        }

                        busyIndicator.running = true
                        connectRequested(ip, password)
                    }

                    background: Rectangle {
                        radius: 10
                        gradient: Gradient {
                            GradientStop { position: 0; color: "#258cff" }
                            GradientStop { position: 1; color: "#742cff" }
                        }
                    }

                    contentItem: Text {
                        text: qsTr("连接")
                        color: "white"
                        font.pixelSize: 16
                        horizontalAlignment: Text.AlignHCenter
                        verticalAlignment: Text.AlignVCenter
                    }
                }

                BusyIndicator {
                    id: busyIndicator
                    running: false
                    visible: running
                    anchors.horizontalCenter: parent.horizontalCenter
                }
            }
        }
    }

    function stopBusy() {
        busyIndicator.running = false
    }

    function showError(message) {
        errorDialog.text = message
        errorDialog.open()
    }

    ErrorMessageDialog {
        id: errorDialog
        standardButtons: Dialog.Ok
    }
}