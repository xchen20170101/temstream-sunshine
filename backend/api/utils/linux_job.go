//go:build !windows
// +build !windows

package utils

import (
	"fmt"
	"os/exec"
	"strings"
)

type WindowsJob struct{}

func (w WindowsJob) getCPUSerialNumber() (string, error) {
	return "", nil
}

// removeDuplicates 函数用于移除重复的元素
func removeDuplicates(elements []string) []string {
	encountered := map[string]bool{}
	result := []string{}

	for v := range elements {
		if encountered[elements[v]] == true {
			// 已经存在，跳过
		} else {
			// 未出现过，添加到结果中
			encountered[elements[v]] = true
			result = append(result, elements[v])
		}
	}

	return result
}

func (w WindowsJob) getBoardSerialNumber() (string, error) {
	return "", nil
}

type LinuxJob struct{}

func (w LinuxJob) getCPUSerialNumber() (string, error) {
	// 执行 sudo dmidecode -t 4 命令
	cmd := exec.Command("sudo", "dmidecode", "-t", "4")

	// 捕获命令输出
	output, err := cmd.Output()
	if err != nil {
		return "", fmt.Errorf("error running dmidecode: %v", err)
	}

	// 将输出转换为字符串
	outputStr := string(output)

	// 在输出中查找包含 "ID" 的行
	lines := strings.Split(outputStr, "\n")
	var cpuIDs []string
	for _, line := range lines {
		if strings.Contains(line, "ID") {
			// 使用分隔符 ':' 分割行，并获取第二部分作为CPU ID
			parts := strings.Split(line, ":")
			if len(parts) == 2 {
				cpuID := strings.ReplaceAll(strings.TrimSpace(parts[1]), " ", "")

				cpuIDs = append(cpuIDs, cpuID)
			}
		}
	}

	// 去除重复的CPU ID
	cpuIDs = removeDuplicates(cpuIDs)

	// 如果存在多个CPU ID，返回第一个
	if len(cpuIDs) > 0 {
		return cpuIDs[0], nil
	}

	return "", fmt.Errorf("CPU ID not found")
}

func (w LinuxJob) getBoardSerialNumber() (string, error) {
	// TODO: 使用固定的
	return "b7bb37a2061b11efa9c61904aec7c7c1", nil
}
