import QtQuick 2.9
import QtQuick.Controls 2.2
import QtQuick.Layouts 1.2

import ComputerManager 1.0
import SdlGamepadKeyNavigation 1.0

Page {
    id: root
    objectName: qsTr("服务端配置")
    title: qsTr("服务端配置")

    signal backRequested()

    background: Rectangle {
        color: "#2D2D2D"
    }

    Rectangle {
        anchors.fill: parent
        color: "#2D2D2D"

        ColumnLayout {
            anchors.centerIn: parent
            width: Math.min(parent.width - 80, 500)
            spacing: 20

            Label {
                text: qsTr("服务端地址配置")
                font.pointSize: 24
                font.bold: true
                color: "#FFFFFF"
                Layout.alignment: Qt.AlignHCenter
            }

            Label {
                text: qsTr("配置后端服务端地址，用于设备直连和用户认证")
                color: "#AAAAAA"
                font.pointSize: 12
                wrapMode: Text.WordWrap
                Layout.fillWidth: true
                horizontalAlignment: Text.AlignHCenter
            }

            TextField {
                id: serverIpInput
                Layout.fillWidth: true
                placeholderText: qsTr("例如 192.168.1.100")
                text: ComputerManager.getServerIp()
                font.pointSize: 16
                padding: 12

                background: Rectangle {
                    color: "#3D3D3D"
                    radius: 6
                    border.color: serverIpInput.focus ? "#4A90D9" : "#5D5D5D"
                    border.width: 2
                }

                onEditingFinished: {
                    ComputerManager.setServerIp(text.trim())
                }
            }

            Row {
                spacing: 15
                Layout.alignment: Qt.AlignHCenter

                Button {
                    id: saveButton
                    text: qsTr("保存")
                    width: 120
                    height: 44
                    font.pointSize: 14

                    contentItem: Text {
                        text: saveButton.text
                        color: "#FFFFFF"
                        horizontalAlignment: Text.AlignHCenter
                        verticalAlignment: Text.AlignVCenter
                    }

                    background: Rectangle {
                        color: saveButton.hovered ? "#4A90D9" : "#3D7AC7"
                        radius: 6
                    }

                    onClicked: {
                        ComputerManager.setServerIp(serverIpInput.text.trim())
                        savedLabel.visible = true
                        savedTimer.restart()
                    }
                }

                Button {
                    id: backButton
                    text: qsTr("返回")
                    width: 120
                    height: 44
                    font.pointSize: 14

                    contentItem: Text {
                        text: backButton.text
                        color: "#FFFFFF"
                        horizontalAlignment: Text.AlignHCenter
                        verticalAlignment: Text.AlignVCenter
                    }

                    background: Rectangle {
                        color: backButton.hovered ? "#5D5D5D" : "#4D4D4D"
                        radius: 6
                    }

                    onClicked: backRequested()
                }
            }

            Label {
                id: savedLabel
                visible: false
                text: qsTr("已保存！")
                font.pointSize: 14
                color: "#4CAF50"
                Layout.alignment: Qt.AlignHCenter

                Timer {
                    id: savedTimer
                    interval: 2000
                    onTriggered: {
                        savedLabel.visible = false
                    }
                }
            }
        }
    }
}
