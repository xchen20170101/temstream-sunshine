import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3

import ComputerManager 1.0

Page {
    id: root
    title: qsTr("连接")
    objectName: qsTr("连接")

    signal directConnectRequested(string deviceId, string devicePassword)
    signal loginRequested()
    signal backToHomeRequested()

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
                    id: deviceIdInput
                    width: parent.width
                    height: 50
                    placeholderText: qsTr("输入 8 位设备 ID")
                    font.pixelSize: 14
                    maximumLength: 8

                    validator: RegExpValidator {
                        regExp: /[0-9]*/
                    }

                    onTextChanged: {
                        text = text.replace(/[^0-9]/g, "")
                    }

                    Keys.onReturnPressed: directConnectButton.clicked()
                    Keys.onEnterPressed: directConnectButton.clicked()
                }

                TextField {
                    id: devicePasswordInput
                    width: parent.width
                    height: 50
                    placeholderText: qsTr("输入 6 位设备密码")
                    font.pixelSize: 14
                    echoMode: TextInput.Password
                    maximumLength: 6

                    validator: RegExpValidator {
                        regExp: /[A-Za-z0-9]*/
                    }

                    onTextChanged: {
                        text = text.replace(/[^A-Za-z0-9]/g, "")
                    }

                    Keys.onReturnPressed: directConnectButton.clicked()
                    Keys.onEnterPressed: directConnectButton.clicked()
                }

                Row {
                    width: parent.width
                    spacing: 12

                    Button {
                        id: directConnectButton
                        width: (parent.width - 12) * 2 / 3
                        height: 50
                        enabled: !busyIndicator.running

                        onClicked: {
                            var deviceId = deviceIdInput.text.trim()
                            var devicePassword = devicePasswordInput.text.trim()

                            if (deviceId.length !== 8) {
                                errorDialog.text = qsTr("请输入完整的 8 位设备 ID")
                                errorDialog.open()
                                return
                            }

                            if (devicePassword.length !== 6) {
                                errorDialog.text = qsTr("请输入完整的 6 位设备密码")
                                errorDialog.open()
                                return
                            }

                            busyIndicator.running = true
                            directConnectRequested(deviceId, devicePassword)
                        }

                        background: Rectangle {
                            radius: 10
                            gradient: Gradient {
                                GradientStop { position: 0; color: "#258cff" }
                                GradientStop { position: 1; color: "#742cff" }
                            }
                        }

                        contentItem: Text {
                            text: qsTr("直接连接")
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
                        enabled: !busyIndicator.running

                        onClicked: backToHomeRequested()

                        background: Rectangle {
                            color: parent.hovered ? "#4A4A4A" : "#3D3D3D"
                            radius: 10
                        }

                        contentItem: Text {
                            text: parent.text
                            color: "#8ea5c9"
                            font.pixelSize: 14
                            horizontalAlignment: Text.AlignHCenter
                            verticalAlignment: Text.AlignVCenter
                        }
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
