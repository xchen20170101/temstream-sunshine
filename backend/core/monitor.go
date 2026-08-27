package core

import (
	"errors"
	"gin-vue/api/utils"
	"gin-vue/global"
	"gin-vue/modles/models"
	"net"
	"strings"
	"time"
)

// 固定 API Key，用于 Sunshine 设备认证
const DeviceApiKey = "SUNSHINE_DEVICE_AUTH_2026"

func DeviceMonitor() {
	ticker := time.NewTicker(20 * time.Second)
	for {
		select {
		case <-ticker.C:
			CheckAllDevice()
		}
	}
}

// 清理消息内容，去除空字符、换行符等
func cleanMessage(message string) string {
	// 去除所有控制字符和空白字符
	cleaned := strings.TrimSpace(message)
	cleaned = strings.TrimRight(cleaned, "\x00")
	cleaned = strings.ReplaceAll(cleaned, "\n", "")
	cleaned = strings.ReplaceAll(cleaned, "\r", "")
	cleaned = strings.ReplaceAll(cleaned, "\t", "")
	return cleaned
}

// 验证IP地址格式
func isValidIP(ip string) bool {
	ip = strings.TrimSpace(ip)
	if ip == "" {
		return false
	}
	parts := strings.Split(ip, ".")
	if len(parts) != 4 {
		return false
	}
	for _, part := range parts {
		if len(part) == 0 || len(part) > 3 {
			return false
		}
		for _, c := range part {
			if c < '0' || c > '9' {
				return false
			}
		}
	}
	return true
}

// parseMessage 返回值增加 deviceId、devicePassword 和 publicKey
// 返回: (ip, hostname, messageType, username, deviceId, devicePassword, publicKey)
// messageType: "register", "keepalive", "pin", "unknown"
func parseMessage(message string, defaultIp string) (string, string, string, string, string, string, string) {
	// 清理消息
	message = cleanMessage(message)
	global.Logger.Printf("parseMessage: Original message length: %d, cleaned: [%s]", len(message), message)

	if len(message) == 0 {
		global.Logger.Printf("parseMessage: Empty message after cleaning, using default IP: %s", defaultIp)
		return defaultIp, "UnknownHost", "register", "", "", "", ""
	}

	// 处理pin请求: "get pin:deviceId:<deviceId>" 或 "get pin:hostname:<hostname>" 或旧格式 "get pin:<hostname>"
	if strings.Contains(strings.ToLower(message), "pin") {
		global.Logger.Printf("parseMessage: Detected PIN request")
		// 新格式: "get pin:deviceId:<id>" 或 "get pin:hostname:<name>"
		if strings.Contains(strings.ToLower(message), "get pin:deviceid:") || strings.Contains(strings.ToLower(message), "get pin:hostname:") {
			parts := strings.SplitN(message, ":", 3)
			if len(parts) == 3 {
				idType := strings.ToLower(strings.TrimSpace(parts[1]))
				idValue := strings.TrimSpace(parts[2])
				global.Logger.Printf("parseMessage: PIN request type=%s, value=%s", idType, idValue)
				return defaultIp, "", "pin:"+idType, idValue, "", "", ""
			}
		}
		// 旧格式：只有 "get pin" 或 "get pin:<hostname>"
		colonIdx := strings.Index(strings.ToLower(message), "pin:")
		if colonIdx >= 0 {
			hostname := strings.TrimSpace(message[colonIdx+4:])
			if hostname != "" {
				global.Logger.Printf("parseMessage: PIN request with hostname (legacy): %s", hostname)
				return defaultIp, hostname, "pin:hostname", "", "", "", ""
			}
		}
		global.Logger.Printf("parseMessage: PIN request without identifier, using connection IP: %s", defaultIp)
		return defaultIp, "", "pin:hostname", "", "", "", ""
	}

	// 处理keepalive消息: keepalive:<ip>:<deviceId>:<devicePassword>:<publicKey> 或 keepalive:<ip>:<deviceId>:<devicePassword> 或 keepalive:<ip>:<deviceId> 或 keepalive:<ip> 或 keepalive
	if strings.HasPrefix(strings.ToLower(message), "keepalive") {
		parts := strings.SplitN(message, ":", 5)
		if len(parts) >= 2 {
			ip := strings.TrimSpace(parts[1])
			if !isValidIP(ip) {
				global.Logger.Printf("parseMessage: Keepalive message contains invalid IP: %s, using default: %s", ip, defaultIp)
				ip = defaultIp
			} else {
				global.Logger.Printf("parseMessage: Keepalive message contains valid IP: %s", ip)
			}
			// 新格式: keepalive:<ip>:<deviceId>:<devicePassword>:<publicKey>
			if len(parts) >= 4 {
				deviceId := strings.TrimSpace(parts[2])
				devicePassword := strings.TrimSpace(parts[3])
				publicKey := ""
				if len(parts) >= 5 {
					publicKey = strings.TrimSpace(parts[4])
				}
				global.Logger.Printf("parseMessage: Keepalive with deviceId=%s, devicePassword=%s, publicKey=%s", deviceId, devicePassword, publicKey)
				return ip, "", "keepalive", "", deviceId, devicePassword, publicKey
			}
			// 旧格式 (无 devicePassword): keepalive:<ip>:<deviceId>
			if len(parts) >= 3 {
				deviceId := strings.TrimSpace(parts[2])
				global.Logger.Printf("parseMessage: Keepalive with deviceId=%s (no devicePassword)", deviceId)
				return ip, "", "keepalive", "", deviceId, "", ""
			}
			return ip, "", "keepalive", "", "", "", ""
		}
		// 消息中不包含IP或IP无效，使用连接来源IP
		global.Logger.Printf("parseMessage: Keepalive message without IP, using connection IP: %s", defaultIp)
		return defaultIp, "", "keepalive", "", "", "", ""
	}

	// 处理注册消息: register:<hostname>:<ip>:<deviceId>:<devicePassword> (新V4格式)
	// 旧格式兼容: register:<hostname>:<ip>:<username> 或 register:<hostname>:<ip> 或 register:<hostname>
	if strings.HasPrefix(strings.ToLower(message), "register:") {
		parts := strings.SplitN(message, ":", 5) // 分割成最多5部分，支持新格式
		global.Logger.Printf("parseMessage: Register message split into %d parts", len(parts))

		if len(parts) >= 4 {
			// 新格式: register:<hostname>:<ip>:<deviceId>:<devicePassword>
			hostname := strings.TrimSpace(parts[1])
			ip := strings.TrimSpace(parts[2])
			deviceId := strings.TrimSpace(parts[3])
			devicePassword := ""
			if len(parts) >= 5 {
				devicePassword = strings.TrimSpace(parts[4])
			}

			// 验证hostname
			if hostname == "" {
				hostname = "UnknownHost"
				global.Logger.Printf("parseMessage: Empty hostname in register message, using default")
			}

			// 验证IP
			if isValidIP(ip) {
				global.Logger.Printf("parseMessage: Register message contains valid IP: %s, hostname: %s, deviceId: %s, devicePassword: %s", ip, hostname, deviceId, devicePassword)
				return ip, hostname, "register", "", deviceId, devicePassword, ""
			} else {
				global.Logger.Printf("parseMessage: Register message contains invalid IP: %s, using default: %s, hostname: %s, deviceId: %s, devicePassword: %s", ip, defaultIp, hostname, deviceId, devicePassword)
				return defaultIp, hostname, "register", "", deviceId, devicePassword, ""
			}
		} else if len(parts) == 3 {
			// 旧格式: register:<hostname>:<ip>:<username> (3个字段，包含username)
			hostname := strings.TrimSpace(parts[1])
			ip := strings.TrimSpace(parts[2])

			// 验证hostname
			if hostname == "" {
				hostname = "UnknownHost"
				global.Logger.Printf("parseMessage: Empty hostname in register message, using default")
			}

			// 验证IP
			if isValidIP(ip) {
				global.Logger.Printf("parseMessage: Register message (legacy with username): IP: %s, hostname: %s", ip, hostname)
				return ip, hostname, "register", "", "", "", ""
			} else {
				global.Logger.Printf("parseMessage: Register message (legacy) invalid IP: %s, using default: %s, hostname: %s", ip, defaultIp, hostname)
				return defaultIp, hostname, "register", "", "", "", ""
			}
		} else if len(parts) == 2 {
			// 旧格式: register:<hostname>
			hostname := strings.TrimSpace(parts[1])
			if hostname == "" {
				hostname = "UnknownHost"
				global.Logger.Printf("parseMessage: Empty hostname in register message, using default")
			}
			global.Logger.Printf("parseMessage: Register message without IP, hostname: %s, using connection IP: %s", hostname, defaultIp)
			return defaultIp, hostname, "register", "", "", "", ""
		} else {
			// 格式错误，尝试作为旧格式处理
			global.Logger.Printf("parseMessage: Invalid register format, parts count: %d", len(parts))
		}
	}

	// 旧格式：直接是hostname（不包含冒号，或包含冒号但不是register/keepalive格式）
	// 检查是否看起来像IP地址（包含3个点）
	dotCount := strings.Count(message, ".")
	if dotCount == 3 && isValidIP(message) {
		// 整个消息就是一个IP地址，这不应该作为注册消息
		global.Logger.Printf("parseMessage: Message is an IP address, treating as unknown format")
		return defaultIp, message, "unknown", "", "", "", ""
	}

	// 不包含冒号，或包含冒号但不是已知格式，认为是hostname
	if !strings.Contains(message, ":") || (strings.Contains(message, ":") && dotCount < 3) {
		hostname := message
		if hostname == "" {
			hostname = "UnknownHost"
		}
		global.Logger.Printf("parseMessage: Old format message (hostname only): %s, using connection IP: %s", hostname, defaultIp)
		return defaultIp, hostname, "register", "", "", "", ""
	}

	// 未知格式，但尝试作为注册处理（使用整个消息作为hostname）
	global.Logger.Printf("parseMessage: Unknown message format: %s, treating as register with connection IP: %s", message, defaultIp)
	return defaultIp, message, "register", "", "", "", ""
}

// 认证解析函数：验证并提取原始消息
func parseAuthMessage(message string) (string, bool) {
	message = cleanMessage(message)

	// 检查认证前缀
	const authPrefix = "SUNSHINE_API_KEY:"
	if !strings.HasPrefix(message, authPrefix) {
		global.Logger.Printf("Auth: Rejected unauthenticated request from message: [%s]", message)
		return "", false
	}

	// 提取API Key和原始消息
	remaining := strings.TrimPrefix(message, authPrefix)
	parts := strings.SplitN(remaining, ":", 2)

	if len(parts) < 2 {
		global.Logger.Printf("Auth: Invalid format after prefix")
		return "", false
	}

	apiKey := parts[0]
	originalMessage := parts[1]

	// 验证API Key
	if apiKey != DeviceApiKey {
		global.Logger.Printf("Auth: Invalid API key")
		return "", false
	}

	global.Logger.Printf("Auth: Authentication successful")
	return originalMessage, true
}

func handleConnection(conn net.Conn) {
	defer conn.Close()

	remoteAddrInfo := conn.RemoteAddr().String()
	temps := strings.Split(remoteAddrInfo, ":")
	defaultIp := temps[0]
	global.Logger.Println("remote addr:", defaultIp)
	
	// 读取客户端发送的数据
	buffer := make([]byte, 1024)
	n, err := conn.Read(buffer)
	if err != nil {
		global.Logger.Println("Error reading:", err.Error())
		return
	}
	
	message := string(buffer[:n])
	global.Logger.Println("Received message from client:", message)

	// 认证检查：验证并提取原始消息
	authenticatedMessage, ok := parseAuthMessage(message)
	if !ok {
		conn.Write([]byte("AUTH_FAILED: Invalid or missing API key"))
		return
	}

	// 使用认证后的消息继续处理
	ip, hostname, msgType, username, deviceId, devicePassword, publicKey := parseMessage(authenticatedMessage, defaultIp)
	global.Logger.Printf("Parsed message - IP: %s, Hostname: %s, Type: %s, Username: %s, DeviceId: %s, DevicePassword: %s, PublicKey: %s", ip, hostname, msgType, username, deviceId, devicePassword, publicKey)

	// 方案 B：后端二次校验。后端已经能从 conn.RemoteAddr() 拿到 Sunshine 的真实源 IP。
	// - 如果 Sunshine 自报的 IP 与 connection 源 IP 一致，直接采用
	// - 如果不一致，以 connection 源 IP 为准（这是后端真正能看到的，可回连）
	// - 如果 Sunshine 没有报 IP（空或无效），直接使用 connection 源 IP
	remoteAddr := conn.RemoteAddr().String()
	ipParts := strings.Split(remoteAddr, ":")
	connectionIp := ipParts[0]
	if connectionIp != defaultIp {
		// 正常情况下 defaultIp == connectionIp；保留对老格式的兼容
		connectionIp = defaultIp
	}

	if !isValidIP(ip) {
		// Sunshine 上报 IP 无效或为空，使用 connection 源 IP
		global.Logger.Printf("handleConnection: Sunshine reported IP invalid/empty (%q), using connection IP: %s", ip, connectionIp)
		ip = connectionIp
	} else if ip != connectionIp {
		// Sunshine 上报 IP 与后端看到的 connection 源 IP 不一致
		// 通常意味着 Sunshine 选了错误的网卡（虚拟网卡干扰），覆盖为真实值
		global.Logger.Printf("handleConnection: Sunshine reported IP (%s) != connection IP (%s), overriding with connection IP", ip, connectionIp)
		ip = connectionIp
	}

	// 提取消息基础类型和子类型（如 pin:hostname, pin:deviceid）
	baseType := msgType
	var pinQueryId string
	if strings.HasPrefix(baseType, "pin:") {
		baseType = "pin"
		pinQueryId = strings.TrimPrefix(msgType, "pin:")
	}

	if baseType == "pin" {
		global.Logger.Printf("handleConnection: Processing PIN request from IP: %s, pinQueryId: %s", ip, pinQueryId)
		handlePin(conn, ip, pinQueryId)
	} else if baseType == "keepalive" {
		global.Logger.Printf("handleConnection: Processing keepalive from IP: %s", ip)
		handleKeepalive(ip, deviceId, devicePassword, publicKey)
	} else if baseType == "register" {
		global.Logger.Printf("handleConnection: Processing register - IP: %s, Hostname: %s, Username: %s, DeviceId: %s, DevicePassword: %s", ip, hostname, username, deviceId, devicePassword)
		err := handleRegister(conn, ip, hostname, username, deviceId, devicePassword, publicKey)
		if err != nil {
			global.Logger.Printf("handleConnection: Register failed: %v", err)
		} else {
			global.Logger.Printf("handleConnection: Register successful - IP: %s, Hostname: %s, Username: %s, DeviceId: %s, DevicePassword: %s", ip, hostname, username, deviceId, devicePassword)
		}
	} else {
		global.Logger.Printf("handleConnection: Unknown message type: %s, treating as register - IP: %s, Hostname: %s", baseType, ip, hostname)
		err := handleRegister(conn, ip, hostname, username, deviceId, devicePassword, publicKey)
		if err != nil {
			global.Logger.Printf("handleConnection: Register (from unknown type) failed: %v", err)
		}
	}
}

func handleKeepalive(serverIp string, deviceId string, devicePassword string, publicKey string) {
	var device models.Device

	// 优先通过 deviceId 查找（V4），其次通过 IP 查找（兼容旧格式）
	if deviceId != "" {
		global.DB.Where("device_id = ?", deviceId).First(&device)
	}
	if device.ID == "" {
		global.DB.Where("ip = ?", serverIp).First(&device)
	}
	if device.ID == "" {
		return
	}
	// 更新last_active_time
	device.LastActiveTime = time.Now().UnixNano() / int64(time.Millisecond)
	if device.Status == "Offline" {
		device.Status = "Online"
	}
	// V4: 设备上报了 deviceId 但数据库中没有，则写入
	if deviceId != "" && device.DeviceId == "" {
		device.DeviceId = deviceId
	}
	// V4: 设备上报了 devicePassword 且与数据库不一致，则更新
	if devicePassword != "" && device.DevicePassword != devicePassword {
		device.DevicePassword = devicePassword
	}
	// 方案 B: 如果 Sunshine 真实通信 IP 与存储 IP 不一致（如换网卡/换网络），同步更新
	// 这样 Moonlight 客户端总是能拿到最新可达的 IP
	if device.Ip != serverIp {
		global.Logger.Printf("handleKeepalive: device IP changed from %s to %s", device.Ip, serverIp)
		device.Ip = serverIp
	}
	global.DB.Save(&device)
}

func handlePin(conn net.Conn, serverIp string, pinQueryId string) {
	var device models.Device

	// V4: 解析 pinQueryId 格式 "deviceid:<id>" 或 "hostname:<name>"
	if strings.HasPrefix(pinQueryId, "deviceid:") {
		id := strings.TrimPrefix(pinQueryId, "deviceid:")
		id = strings.TrimSpace(id)
		if id != "" {
			global.DB.Where("device_id = ?", id).First(&device)
			if device.ID != "" {
				global.Logger.Printf("handlePin: Found device by deviceId: %s, IP: %s", id, device.Ip)
			} else {
				global.Logger.Printf("handlePin: Device not found by deviceId: %s, falling back to IP", id)
			}
		}
	} else if strings.HasPrefix(pinQueryId, "hostname:") {
		hostname := strings.TrimPrefix(pinQueryId, "hostname:")
		hostname = strings.TrimSpace(hostname)
		if hostname != "" {
			global.DB.Where("name = ?", hostname).First(&device)
			if device.ID != "" {
				global.Logger.Printf("handlePin: Found device by hostname: %s, IP: %s", hostname, device.Ip)
			} else {
				global.Logger.Printf("handlePin: Device not found by hostname: %s, falling back to IP", hostname)
			}
		}
	}

	// 兼容旧格式：通过 IP 查找（作为 fallback）
	if device.ID == "" {
		global.DB.Where("ip = ?", serverIp).First(&device)
		if device.ID != "" {
			global.Logger.Printf("handlePin: Found device by IP (fallback): %s", serverIp)
		}
	}

	if device.ID == "" || device.Pin == "" {
		global.Logger.Printf("Device.NotFound - IP: %s, pinQueryId: %s", serverIp, pinQueryId)
		return
	}

	// 发送响应给客户端
	_, err := conn.Write([]byte(device.Pin))
	if err != nil {
		global.Logger.Println("Error writing:", err.Error())
		return
	}
	// 更新其状态为Online
	device.Status = "Online"
	global.DB.Save(&device)
}

func handleRegister(conn net.Conn, serverIp string, hostname string, username string, deviceId string, devicePassword string, publicKey string) error {
	var device models.Device
	global.DB.Where("ip =?", serverIp).First(&device)
	if device.ID != "" {
		global.Logger.Println("Device.Exist", serverIp)

		// 方案 A: 检测设备重装情况
		// 如果设备已存在但 device_id 不同，认为是设备重装了，直接覆盖 device_id
		if deviceId != "" && device.DeviceId != deviceId {
			if device.DeviceId != "" {
				global.Logger.Printf("handleRegister: Device reinstall detected! IP=%s, oldDeviceId=%s, newDeviceId=%s, updating device_id",
					serverIp, device.DeviceId, deviceId)
			}
			device.DeviceId = deviceId
		}

		// V4: 设备上报了 devicePassword 且与数据库不一致，则更新
		if devicePassword != "" && device.DevicePassword != devicePassword {
			device.DevicePassword = devicePassword
			global.Logger.Printf("handleRegister: Updated devicePassword for IP=%s", serverIp)
		}
		if publicKey != "" && device.PublicKey == "" {
			device.PublicKey = publicKey
		}
		if deviceId != "" || devicePassword != "" || publicKey != "" {
			global.DB.Save(&device)
			global.Logger.Printf("handleRegister: Updated device %s with deviceId=%s, devicePassword=%s", serverIp, deviceId, devicePassword)
		}
		return nil
	}

	// V4: device_id 必填检查
	if deviceId == "" {
		conn.Write([]byte("ERROR: device_id is required"))
		global.Logger.Println("handleRegister: device_id is required")
		return errors.New("device_id is required")
	}

	// V4: device_password 必填检查（非局域网模式必须上报）
	if devicePassword == "" {
		conn.Write([]byte("ERROR: device_password is required"))
		global.Logger.Println("handleRegister: device_password is required")
		return errors.New("device_password is required")
	}

	// V4: 冲突检测 - 检查 device_id 是否已被其他设备使用
	var existingDevice models.Device
	global.DB.Where("device_id = ?", deviceId).First(&existingDevice)
	if existingDevice.ID != "" {
		// 方案 A: 如果 device_id 已被使用，但该设备与当前 IP 不同
		// 可能是 IP 变化的情况，尝试通过 device_id 找到设备并更新 IP
		if existingDevice.Ip != serverIp {
			global.Logger.Printf("handleRegister: device_id %s exists on different IP (%s), updating to new IP %s",
				deviceId, existingDevice.Ip, serverIp)
			existingDevice.Ip = serverIp
			if devicePassword != "" {
				existingDevice.DevicePassword = devicePassword
			}
			if hostname != "" {
				existingDevice.Name = hostname
			}
			existingDevice.LastActiveTime = time.Now().UnixNano() / int64(time.Millisecond)
			global.DB.Save(&existingDevice)
			conn.Write([]byte("OK"))
			global.Logger.Printf("handleRegister: Updated existing device with device_id %s to new IP %s", deviceId, serverIp)
			return nil
		}
		conn.Write([]byte("ERROR: device_id already exists"))
		global.Logger.Printf("handleRegister: device_id %s already exists on same IP", deviceId)
		return errors.New("device_id already exists")
	}

	targetHostName := strings.ReplaceAll(hostname, "\u0000", "")

	// 注册成功，返回 OK
	conn.Write([]byte("OK"))

	myNewDevice := models.Device{
		ID:             models.NewUUID(),
		Name:           targetHostName,
		Ip:             serverIp,
		Pin:            utils.CreateRandPin(),
		DevicePassword: devicePassword,
		Status:         "Offline",
		DeviceId:       deviceId,
		PublicKey:      publicKey,
		CreatedTime:    time.Now().UnixNano() / int64(time.Millisecond),
		LastActiveTime: time.Now().UnixNano() / int64(time.Millisecond),
	}
	global.DB.Create(&myNewDevice)

	// 如果提供了用户名，尝试建立用户与设备的绑定关系
	if username != "" {
		var user models.User
		global.DB.Where("name = ?", username).First(&user)
		if user.ID != "" {
			// 用户存在，建立绑定关系
			bind := models.Bind{
				ID:       models.NewUUID(),
				DeviceId: myNewDevice.ID,
				UserId:   user.ID,
			}
			global.DB.Create(&bind)
			global.Logger.Printf("Device bound to user: device=%s, user=%s", myNewDevice.Name, username)
		} else {
			global.Logger.Printf("User not found: %s, skipping binding", username)
		}
	}

	return nil
}

// sunshine状态管理检查
func CheckSunshineStatus() {
	listener, err := net.Listen("tcp", ":12345")
	if err != nil {
		global.Logger.Println("Error listening:", err.Error())
		return
	}
	defer listener.Close()
	global.Logger.Println("Server started, waiting for clients...")

	// 处理客户端连接
	for {
		conn, err := listener.Accept()
		if err != nil {
			global.Logger.Println("Error accepting:", err.Error())
			break
		}
		go handleConnection(conn)
	}
}

// 检查所有云桌面的状态并更新数据库
func CheckAllDevice() {
	global.Logger.Println("Checking all devices")
	var allDevices []*models.Device
	global.DB.Find(&allDevices)
	// 遍历所有设备，若当前时间超过last_active_time 3分钟，则将状态更新为Offline
	for _, value := range allDevices {
		if value.Status == "Online" {
			currentTime := time.Now().UnixNano() / int64(time.Millisecond)
			if currentTime-value.LastActiveTime > 3*60*1000 {
				value.Status = "Offline"
				global.DB.Save(&value)
			}
		}
	}
}

// 定期往数据库中写入时间（测试授权防修改时间）
func TriggerLicenseDateTask() {
	currentTime := time.Now()

	nextTriggerTime := time.Date(currentTime.Year(), currentTime.Month(), currentTime.Day(), 23, 59, 0, 0, currentTime.Location())
	if currentTime.After(nextTriggerTime) {
		nextTriggerTime = nextTriggerTime.Add(24 * time.Hour)
	}

	durationUntilNextTrigger := nextTriggerTime.Sub(currentTime)
	ticker := time.NewTicker(durationUntilNextTrigger)

	for {
		select {
		case <-ticker.C:
			global.Logger.Printf("TriggerLicenseDateTask add license date:%v", currentTime)
			AddLicenseDate()

			nextTriggerTime = nextTriggerTime.Add(24 * time.Hour)
			durationUntilNextTrigger = time.Until(nextTriggerTime)
			ticker.Reset(durationUntilNextTrigger)
		}
	}
}

// 往license_date表中添加数据
func AddLicenseDate() {
	var licenseDate models.LicenseDate
	currentTime := time.Now()
	dateString := currentTime.Format("2006-01-02")
	licenseDate.ID = models.NewUUID()
	licenseDate.DayDate = dateString
	global.DB.Create(&licenseDate)
}

// func InitTemplateInfo() {
// 	templates := global.Config.Vm.GetTemplateMap()
// 	for key, value := range templates {
// 		utils.InsertOrUpdateTemplate(key, value)
// 	}
// }

