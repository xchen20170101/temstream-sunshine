import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.3

import ComputerManager 1.0
import StreamingPreferences 1.0

Page {
    id: root
    title: qsTr("连接方式")
    objectName: qsTr("连接方式")

    signal directConnectSelected()
    signal loginSelected()
    signal lanModeChanged()
    signal serverConfigSelected()

    Rectangle {
        anchors.fill: parent
        color: "#020511"

        Image {
            anchors.fill: parent
            source: "qrc:/res/login_background.png"
            fillMode: Image.PreserveAspectCrop
        }

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
            height: 460
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
                    text: qsTr("请选择访问方式")
                    font.pixelSize: 24
                    font.bold: true
                    color: "white"
                }

                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: qsTr("你可以直接输入设备 ID 访问，也可以通过账号登录进入设备列表")
                    color: "#8ea5c9"
                    font.pixelSize: 14
                    wrapMode: Text.WordWrap
                    horizontalAlignment: Text.AlignHCenter
                    Layout.fillWidth: true
                }

                Button {
                    text: qsTr("输入设备 ID 访问")
                    width: parent.width
                    height: 50
                    onClicked: directConnectSelected()

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
                    text: qsTr("账号登录")
                    width: parent.width
                    height: 50
                    onClicked: loginSelected()

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
                    id: serverConfigBtn
                    text: qsTr("服务端地址配置")
                    width: parent.width
                    height: 44
                    font.pointSize: 14
                    onClicked: serverConfigSelected()

                    background: Rectangle {
                        color: parent.hovered ? "#4A4A4A" : "#3D3D3D"
                        radius: 8
                    }

                    contentItem: Text {
                        text: serverConfigBtn.text
                        color: "#8ea5c9"
                        font.pointSize: 14
                        horizontalAlignment: Text.AlignHCenter
                        verticalAlignment: Text.AlignVCenter
                    }
                }
            }
        }
    }
}
