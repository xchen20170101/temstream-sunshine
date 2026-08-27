package utils

import (
	"bytes"
	"encoding/json"
	"fmt"
	"gin-vue/global"
	"gin-vue/modles/models"
	"io/ioutil"
	"math/rand"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"time"

	"golang.org/x/text/encoding/simplifiedchinese"
	"golang.org/x/text/transform"
)

type GpuInfo struct {
	Prefix   string
	Hardware string
	Position string
	Identify string
	Desc     string
}

// Headscale用户创建响应结构体
type HeadscaleUserResponse struct {
	ID        int                    `json:"id"`
	Name      string                 `json:"name"`
	CreatedAt map[string]interface{} `json:"created_at"`
}

// Headscale预授权密钥创建响应结构体
type HeadscalePreauthKeyResponse struct {
	User       HeadscaleUserResponse `json:"user"`
	ID         int                   `json:"id"`
	Key        string                `json:"key"`
	Reusable   bool                  `json:"reusable"`
	Expiration map[string]interface{} `json:"expiration"`
	CreatedAt  map[string]interface{} `json:"created_at"`
}

// Headscale节点信息结构体
type HeadscaleNode struct {
	ID          int                    `json:"id"`
	MachineKey  string                 `json:"machine_key"`
	NodeKey     string                 `json:"node_key"`
	DiscoKey    string                 `json:"disco_key"`
	IPAddresses []string               `json:"ip_addresses"`
	Name        string                 `json:"name"`
	User        HeadscaleUserResponse  `json:"user"`
	LastSeen    map[string]interface{} `json:"last_seen"`
	Expiry      map[string]interface{} `json:"expiry"`
	PreAuthKey  map[string]interface{} `json:"pre_auth_key,omitempty"`
	CreatedAt   map[string]interface{} `json:"created_at"`
	RegisterMethod int                 `json:"register_method"`
	GivenName   string                 `json:"given_name"`
	Online      bool                   `json:"online"`
}

type PowerShell struct {
	powerShell string
}

func New() *PowerShell {
	ps, _ := exec.LookPath("powershell.exe")
	return &PowerShell{
		powerShell: ps,
	}
}

func (p *PowerShell) Execute(args ...string) (stdOut string, stdErr string, err error) {
	args = append([]string{"-NoProfile", "-NonInteractive"}, args...)
	cmd := exec.Command(p.powerShell, args...)

	var stdout bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err = cmd.Run()
	stdOut, stdErr = stdout.String(), stderr.String()
	return
}

func GetUserInfoByDevice(deviceId string) string {
	// 根据设备ID查找到绑定的用户名
	if deviceId == "" {
		return ""
	}
	var bind models.Bind
	global.DB.Where("device_id = ?", deviceId).First(&bind)
	if bind.UserId == "" {
		return ""
	}
	var user models.User
	global.DB.Where("id = ?", bind.UserId).First(&user)
	return user.Name

}

func GetBindDeviceIds() []string {
	var deviceIds []string
	var binds []*models.Bind
	global.DB.Find(&binds)
	for _, bind := range binds {
		deviceIds = append(deviceIds, bind.DeviceId)
	}
	return deviceIds
}

func GetDeviceIdByName(deviceName string) string {
	if deviceName == "" {
		return ""
	}
	var device models.Device
	global.DB.Where("name = ?", deviceName).First(&device)
	return device.ID
}

func GetDeviceByName(deviceName string) *models.Device {
	if deviceName == "" {
		return nil
	}
	var device models.Device
	global.DB.Where("name =?", deviceName).First(&device)
	return &device
}

// 根据时间戳转换成格式化时间
func TransferTimeStamp(timeStamp int64) string {
	return time.Unix(timeStamp/1000, 0).Format("2006-01-02 15:04:05")
}

// 判断字符串是否在数组中
func IsInArray(target string, srcArray []string) bool {
	for _, value := range srcArray {
		if target == value {
			return true
		}
	}
	return false
}

// 获取当前路径
func GetCurrentPath() string {
	dir, err := os.Getwd()
	if err != nil {
		return "."
	}
	return dir
}

// 是否win11
func IsWindows11() bool {
	cmd := exec.Command("powershell", "Get-CimInstance Win32_OperatingSystem | Select-Object -ExpandProperty Caption")
	stdout, err := cmd.Output()
	if err != nil {
		global.Logger.Printf("is win11 result:%+v\n", err)
		return false
	}
	if strings.Contains(string(stdout), "Windows 11") {
		global.Logger.Println("the system is win11")
		return true
	}
	return false
}

// token校验
func CheckAccessToken(accessToken string, userId string) bool {
	// 1. 校验token是否存在
	// 2. 校验token是否过期
	// 3. 更新token
	var token models.Token
	global.DB.Where("user_id=?", userId).First(&token)
	if token.Value != accessToken {
		global.Logger.Println("token is not equal")
		return false
	}
	nowTime := time.Now().UnixNano() / int64(time.Millisecond)
	if nowTime-token.CreatedTime > token.Cryptoperiod {
		// token过期
		global.Logger.Println("token is expire")
		return false
	}
	myToken := models.Token{
		CreatedTime: nowTime,
	}
	global.DB.Model(token).Updates(myToken)
	return true
}

// 根据用户名检查是否存在token记录
func UpdateOrCreateToken(userId string, accessToken string) {
	var token models.Token
	global.DB.Where("user_id = ?", userId).First(&token)
	if token.Value == "" {
		// 新增
		myToken := &models.Token{
			ID:           models.NewUUID(),
			Value:        accessToken,
			Cryptoperiod: 24 * 60 * 60 * 1000,
			CreatedTime:  time.Now().UnixNano() / int64(time.Millisecond),
			UserId:       userId,
		}
		global.DB.Create(&myToken)
	} else {
		// 更新
		myToken := models.Token{
			Value:        accessToken,
			Cryptoperiod: 24 * 60 * 60 * 1000,
			CreatedTime:  time.Now().UnixNano() / int64(time.Millisecond),
		}
		global.DB.Model(token).Updates(myToken)
	}
}

// 解码中文文本
func DecodeGBK(input string) (string, error) {
	decoder := simplifiedchinese.GBK.NewDecoder()
	inputReader := strings.NewReader(input)
	transformReader := transform.NewReader(inputReader, decoder)

	out, err := ioutil.ReadAll(transformReader)
	if err != nil {
		return "", err
	}

	return string(out), nil
}

// 获取CPU序列号
// func getCPUSerialNumber() (string, error) {
// 	cmd := exec.Command("wmic", "cpu", "get", "ProcessorId")
// 	output, err := cmd.Output()
// 	if err != nil {
// 		return "", err
// 	}

// 	// 提取ProcessorId
// 	cpuInfo := strings.Split(string(output), "\n")
// 	if len(cpuInfo) >= 2 {
// 		return strings.TrimSpace(cpuInfo[1]), nil
// 	}

// 	return "", nil
// }

type Win32_BaseBoard struct {
	SerialNumber string
}

// 获取主板序列号，主板若为多个，则连接
// func getBoardSerialNumber() (string, error) {
// 	var baseBoard []Win32_BaseBoard
// 	query := "select SerialNumber from Win32_BaseBoard"
// 	if err := wmi.Query(query, &baseBoard); err != nil {
// 		global.Logger.Printf("WMI query failed:%+v\n", err)
// 		return "", err
// 	}
// 	serialNumber := ""
// 	for _, board := range baseBoard {
// 		serialNumber += board.SerialNumber
// 	}
// 	return serialNumber, nil
// }

func IsWindows() bool {
	return runtime.GOOS == "windows"
}

// 获取机器码
func GetMachineCode() string {
	// 采用cpu序列号+主板序列号拼接后生成md5值
	var serialNum string
	var boardNum string
	var err error
	if IsWindows() {
		wj := WindowsJob{}
		serialNum, err = wj.getCPUSerialNumber()
		if err != nil {
			global.Logger.Printf("get cpu serial number failed:%+v\n", err)
			serialNum = ""
		}
		boardNum, err = wj.getBoardSerialNumber()
		if err != nil {
			global.Logger.Printf("get board serial number failed:%+v\n", err)
			boardNum = ""
		}
	} else {
		lj := LinuxJob{}
		serialNum, err = lj.getCPUSerialNumber()
		if err != nil {
			global.Logger.Printf("get cpu serial number failed:%+v\n", err)
			serialNum = ""
		}
		boardNum, err = lj.getBoardSerialNumber()
		if err != nil {
			global.Logger.Printf("get board serial number failed:%+v\n", err)
			boardNum = ""
		}
	}
	global.Logger.Printf("serialNum:%+v\n", serialNum)
	global.Logger.Printf("boardNum:%+v\n", boardNum)
	var target string
	if serialNum == "" && boardNum == "" {
		target = "1234567890123456"
	} else {
		target = serialNum + boardNum
	}
	md5Value := calculateMD5(target)
	return md5Value
}

// 校验授权码
func CheckLicense(machineCode string, licenseCode string) (bool, string) {
	if licenseCode == "" {
		return false, ""
	}
	value, err := decrypt(licenseCode)
	if err != nil {
		global.Logger.Printf("decrypt failed:%+v\n", err)
		return false, ""
	}
	temps := strings.Split(value, "&")
	if len(temps) != 2 {
		return false, ""
	}
	if temps[0] != machineCode {
		global.Logger.Println("license check failed")
		return false, ""
	}
	global.Logger.Println("license check pass")
	return true, temps[1]
}

func CheckSystemTimeModify() bool {
	// 获取当天的时间,若返回false，则认为系统时间被修改
	nowDay := time.Now().Format("2006-01-02")
	var licenseDate models.LicenseDate
	global.DB.Where("day_date = ?", nowDay).First(&licenseDate)
	if licenseDate.DayDate != "" {
		global.Logger.Println("system time is modify, license invalid")
		return false
	}
	return true
}

// 执行Linux系统命令
func ExecuteLinuxCommand(command string, args ...string) error {
	cmd := exec.Command(command, args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		global.Logger.Printf("Command execution failed: %s %v, output: %s, error: %v\n", command, args, string(output), err)
		return fmt.Errorf("command execution failed: %s %v, output: %s, error: %v", command, args, string(output), err)
	}
	global.Logger.Printf("Command executed successfully: %s %v, output: %s\n", command, args, string(output))
	return nil
}

// 执行Linux系统命令并返回输出
func ExecuteLinuxCommandWithOutput(command string, args ...string) (string, error) {
	cmd := exec.Command(command, args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		global.Logger.Printf("Command execution failed: %s %v, output: %s, error: %v\n", command, args, string(output), err)
		return "", fmt.Errorf("command execution failed: %s %v, output: %s, error: %v", command, args, string(output), err)
	}
	global.Logger.Printf("Command executed successfully: %s %v, output: %s\n", command, args, string(output))
	return string(output), nil
}

// 创建headscale用户
func CreateHeadscaleUser(username string) (*HeadscaleUserResponse, error) {
	global.Logger.Printf("Creating headscale user: %s\n", username)

	output, err := ExecuteLinuxCommandWithOutput("headscale", "users", "create", username, "-o", "json")
	if err != nil {
		return nil, fmt.Errorf("failed to create headscale user: %v", err)
	}

	var userResp HeadscaleUserResponse
	if err := json.Unmarshal([]byte(output), &userResp); err != nil {
		global.Logger.Printf("Failed to parse headscale user response JSON: %v, output: %s\n", err, output)
		return nil, fmt.Errorf("failed to parse headscale user response: %v", err)
	}

	global.Logger.Printf("Headscale user created successfully: ID=%d, Name=%s\n", userResp.ID, userResp.Name)
	return &userResp, nil
}

// 创建headscale预授权密钥
func CreateHeadscalePreauthKey(userID int) (*HeadscalePreauthKeyResponse, error) {
	global.Logger.Printf("Creating headscale preauth key for user ID: %d\n", userID)

	userIDStr := fmt.Sprintf("%d", userID)
	output, err := ExecuteLinuxCommandWithOutput("headscale", "preauthkeys", "create", "--reusable", "--ephemeral=false", "--user", userIDStr, "--expiration", "3650d", "-o", "json")
	if err != nil {
		return nil, fmt.Errorf("failed to create headscale preauth key: %v", err)
	}

	var keyResp HeadscalePreauthKeyResponse
	if err := json.Unmarshal([]byte(output), &keyResp); err != nil {
		global.Logger.Printf("Failed to parse headscale preauth key response JSON: %v, output: %s\n", err, output)
		return nil, fmt.Errorf("failed to parse headscale preauth key response: %v", err)
	}

	global.Logger.Printf("Headscale preauth key created successfully: KeyID=%d, Key=%s\n", keyResp.ID, keyResp.Key)
	return &keyResp, nil
}

// 删除headscale用户
func DeleteHeadscaleUser(username string) error {
	global.Logger.Printf("Deleting headscale user: %s\n", username)
	return ExecuteLinuxCommand("headscale", "users", "delete", "--name", username, "--force")
}

// 删除headscale预授权密钥
func DeleteHeadscalePreauthKey(userID int) error {
	global.Logger.Printf("Deleting headscale preauth keys for user ID: %d\n", userID)

	userIDStr := fmt.Sprintf("%d", userID)
	return ExecuteLinuxCommand("headscale", "preauthkeys", "delete", "--user", userIDStr)
}

// 获取headscale用户关联的节点列表
func GetHeadscaleUserNodes(username string) ([]HeadscaleNode, error) {
	global.Logger.Printf("Getting headscale nodes for user: %s\n", username)

	output, err := ExecuteLinuxCommandWithOutput("headscale", "nodes", "list", "--user", username, "-o", "json")
	if err != nil {
		return nil, fmt.Errorf("failed to get headscale nodes for user %s: %v", username, err)
	}

	var nodes []HeadscaleNode
	if err := json.Unmarshal([]byte(output), &nodes); err != nil {
		global.Logger.Printf("Failed to parse headscale nodes response JSON: %v, output: %s\n", err, output)
		return nil, fmt.Errorf("failed to parse headscale nodes response: %v", err)
	}

	global.Logger.Printf("Found %d headscale nodes for user %s\n", len(nodes), username)
	return nodes, nil
}

// 删除headscale节点
func DeleteHeadscaleNode(nodeID int) error {
	global.Logger.Printf("Deleting headscale node with ID: %d\n", nodeID)

	nodeIDStr := fmt.Sprintf("%d", nodeID)
	return ExecuteLinuxCommand("headscale", "nodes", "delete", "-i", nodeIDStr, "--force")
}

func CreateRandPin() string {
	// 设置随机种子
	rand.Seed(time.Now().UnixNano())

	// 生成4位随机数字字符串
	randomNumber := fmt.Sprintf("%04d", rand.Intn(10000))
	return randomNumber
}

// AllocateDeviceId 生成8位纯数字设备ID（00000001~99999999）
// 通过随机生成 + 去重检查确保唯一性，最多重试10次
func AllocateDeviceId() string {
	maxRetries := 10
	for i := 0; i < maxRetries; i++ {
		// 生成 00000001 ~ 99999999 范围的随机数
		rand.Seed(time.Now().UnixNano() + int64(i)*int64(rand.Intn(100000)))
		id := rand.Intn(99999999) + 1
		deviceId := fmt.Sprintf("%08d", id)

		// 检查数据库中是否已存在
		var count int64
		global.DB.Model(&models.Device{}).Where("device_id = ?", deviceId).Count(&count)
		if count == 0 {
			global.Logger.Printf("AllocateDeviceId: allocated %s", deviceId)
			return deviceId
		}
		global.Logger.Printf("AllocateDeviceId: conflict with %s, retrying (%d/%d)", deviceId, i+1, maxRetries)
	}
	global.Logger.Println("AllocateDeviceId: failed after max retries")
	return ""
}
