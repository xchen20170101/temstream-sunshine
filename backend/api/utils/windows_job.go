//go:build windows
// +build windows

package utils

import (
	"gin-vue/global"
	"os/exec"
	"strings"

	"github.com/StackExchange/wmi"
)

type WindowsJob struct{}

func (w WindowsJob) getCPUSerialNumber() (string, error) {
	cmd := exec.Command("wmic", "cpu", "get", "ProcessorId")
	output, err := cmd.Output()
	if err != nil {
		return "", err
	}

	// 提取ProcessorId
	cpuInfo := strings.Split(string(output), "\n")
	if len(cpuInfo) >= 2 {
		return strings.TrimSpace(cpuInfo[1]), nil
	}

	return "", nil
}

func (w WindowsJob) getBoardSerialNumber() (string, error) {
	var baseBoard []Win32_BaseBoard
	query := "select SerialNumber from Win32_BaseBoard"
	if err := wmi.Query(query, &baseBoard); err != nil {
		global.Logger.Printf("WMI query failed:%+v\n", err)
		return "", err
	}
	serialNumber := ""
	for _, board := range baseBoard {
		serialNumber += board.SerialNumber
	}
	return serialNumber, nil
}

type LinuxJob struct{}

func (w LinuxJob) getCPUSerialNumber() (string, error) {
	return "", nil
}

func (w LinuxJob) getBoardSerialNumber() (string, error) {
	return "", nil
}
