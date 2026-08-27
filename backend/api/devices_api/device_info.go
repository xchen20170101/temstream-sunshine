package devices_api

import (
	"gin-vue/api/utils"
	"gin-vue/global"
	"gin-vue/modles/form"
	"gin-vue/modles/models"
	"gin-vue/modles/res"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

// 查询云桌面列表
func (DevicesApi) GetDevicesList(c *gin.Context) {
	var devices []*res.GetDevicesListResponse
	var totalNum int64
	//var gpu models.Gpu

	// 从数据库devices获取所有云桌面的信息
	// 根据命令<Get-VM -name 云桌面名称>从hyper-v获取到对应的云桌面的信息
	// 若云桌面的状态信息与数据库中不一致，则更新最新的状态信息到数据库中
	// 返回对应的设备信息及其所属用户
	count, _ := strconv.Atoi(c.Query("count"))
	pageNum, _ := strconv.Atoi(c.Query("index"))
	name := c.Query("name")
	var allDevices []*models.Device
	keyword := "%" + name + "%"
	offset := (pageNum - 1) * count
	if name == "" {
		global.DB.Model(&models.Device{}).Count(&totalNum)
		global.DB.Limit(count).Offset(offset).Find(&allDevices)
	} else {
		global.DB.Model(&models.Device{}).Where("name LIKE ?", keyword).Count(&totalNum)
		global.DB.Limit(count).Offset(offset).Where("name LIKE ?", keyword).Find(&allDevices)
	}
	data := make(map[string]interface{})
	for _, value := range allDevices {
		temp := &res.GetDevicesListResponse{
			ID:             value.ID,
			Name:           value.Name,
			UserName:       utils.GetUserInfoByDevice(value.ID),
			Ip:             value.Ip,
			Pin:            value.Pin,
			Status:         value.Status,
			CreatedTime:    utils.TransferTimeStamp(value.CreatedTime),
			DeviceId:       value.DeviceId,
			DevicePassword: value.DevicePassword,
		}
		devices = append(devices, temp)
	}

	data["devices"] = devices
	data["totalNum"] = totalNum

	res.OkWithData(data, c)
}

// 获取所有设备数
func (DevicesApi) DeviceAllCountGet(c *gin.Context) {
	var countNum int64
	global.DB.Model(&models.Device{}).Count(&countNum)
	data := make(map[string]interface{})
	data["num"] = countNum
	res.OkWithData(data, c)

}

// func (DevicesApi) AddSunshine(c *gin.Context) {
// 	// 1. 接收IP输入，检查是否存在device表，若存在，则报错
// 	// 2. 若不存在，则创建device表，并插入数据
// 	var form form.AddSunshineForm
// 	if err := c.ShouldBind(&form); err != nil {
// 		res.FailWithMsg("Common.InvalidParam", c)
// 		return
// 	}
// 	var device models.Device
// 	global.DB.Where("ip =?", form.Ip).First(&device)
// 	if device.ID != "" {
// 		res.FailWithMsg("Device.Exist", c)
// 		return
// 	}
// 	myNewDevice := models.Device{
// 		ID:          models.NewUUID(),
// 		Ip:          form.Ip,
// 		Pin:         utils.CreateRandPin(),
// 		Status:      "Offline",
// 		CreatedTime: time.Now().UnixNano() / int64(time.Millisecond),
// 	}
// 	global.DB.Create(&myNewDevice)
// 	res.OkWithData(myNewDevice, c)
// }

func (DevicesApi) DeleteSunshine(c *gin.Context) {
	// 根据id查找到数据库中对应的设备信息
	var device models.Device
	var bind models.Bind
	id := c.Param("id")
	global.DB.Where("id = ?", id).First(&device)
	if device.Ip == "" {
		res.FailWithMsg("Device.NotFound", c)
		return
	}

	global.DB.Where("device_id = ?", id).First(&bind)

	// 清除数据库中的设备信息、绑定关系
	result := global.DB.Delete(&device)
	if result.Error != nil {
		res.FailWithMsg("Device.DeleteVMFailed", c)
		return
	}
	if bind.UserId != "" {
		result = global.DB.Delete(&bind)
		if result.Error != nil {
			res.FailWithMsg("Device.DeleteBindFailed", c)
			return
		}
	}
	res.OkWithData(device, c)
}

func (DevicesApi) UpdateSunshine(c *gin.Context) {
	var device models.Device
	id := c.Param("id")
	result := global.DB.Where("id = ?", id).First(&device)
	if result.Error != nil {
		res.FailWithMsg("Device.NotExist", c)
		return
	}
	if device.Ip == "" {
		res.FailWithMsg("Device.NotExist", c)
		return
	}
	var form form.UpdateSunshineForm
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("update sunshine failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	myNewDevice := models.Device{
		Ip: form.Ip,
	}
	global.DB.Model(device).Updates(myNewDevice)

	res.OkWithData(device, c)
}

func (DevicesApi) DistributeDevice(c *gin.Context) {
	var form form.DistributeDeviceForm
	if err := c.ShouldBind(&form); err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	var bind models.Bind
	global.DB.Where("device_id = ?", form.DeviceId).First(&bind)
	if bind.ID != "" {
		res.FailWithMsg("Device.AlreadyBind", c)
		return
	}
	myBind := models.Bind{
		ID:       models.NewUUID(),
		UserId:   form.UserId,
		DeviceId: form.DeviceId,
	}
	global.DB.Create(&myBind)
	res.OkWithData(myBind, c)
}

func (DevicesApi) RecycleDevice(c *gin.Context) {
	var form form.RecycleDeviceForm
	if err := c.ShouldBind(&form); err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	var bind models.Bind
	global.DB.Where("device_id = ?", form.DeviceId).First(&bind)
	if bind.UserId == "" {
		res.FailWithMsg("Device.NotBind", c)
		return
	}
	global.DB.Delete(&bind)
	res.OkWithData(bind, c)
}

// GetOnlineDevices 获取所有在线设备列表（供 Client 发现设备）
func (DevicesApi) GetOnlineDevices(c *gin.Context) {
	var allDevices []*models.Device
	global.DB.Where("status = ?", "Online").Find(&allDevices)

	devices := make([]map[string]interface{}, 0)
	for _, device := range allDevices {
		// 优先使用 DeviceId，如果为空则使用内部 ID
		deviceId := device.DeviceId
		if deviceId == "" {
			deviceId = device.ID
		}

		item := map[string]interface{}{
			"deviceId":        deviceId,
			"name":            device.Name,
			"ip":              device.Ip,
			"port":            47984,
			"pin":             device.Pin, // V4: 返回 PIN 供 Moonlight 配对
			"device_password": device.DevicePassword, // V4: 设备密码
			"status":          device.Status,
		}
		devices = append(devices, item)
	}

	data := map[string]interface{}{
		"devices": devices,
	}
	res.OkWithData(data, c)
}

// AnnounceDevice Sunshine 上报自身 ID + 公钥（补充 TCP 注册的 REST 通道）
func (DevicesApi) AnnounceDevice(c *gin.Context) {
	var form form.AnnounceDeviceForm
	if err := c.ShouldBind(&form); err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	var device models.Device

	// 优先通过 DeviceId 查找，其次通过 IP 查找
	if form.DeviceId != "" {
		global.DB.Where("device_id = ?", form.DeviceId).First(&device)
	}
	if device.ID == "" && form.Ip != "" {
		global.DB.Where("ip = ?", form.Ip).First(&device)
	}
	if device.ID == "" && form.Hostname != "" {
		global.DB.Where("name = ?", form.Hostname).First(&device)
	}

	now := time.Now().UnixNano() / int64(time.Millisecond)

	if device.ID != "" {
		// 设备已存在，更新信息
		updates := map[string]interface{}{
			"status":          "Online",
			"last_active_time": now,
		}
		if form.DeviceId != "" {
			updates["device_id"] = form.DeviceId
		}
		if form.PublicKey != "" {
			updates["public_key"] = form.PublicKey
		}
		if form.Ip != "" {
			updates["ip"] = form.Ip
		}
		if form.Hostname != "" {
			updates["name"] = form.Hostname
		}
		global.DB.Model(&device).Updates(updates)
	} else {
		// 设备不存在，创建新记录
		device = models.Device{
			ID:             models.NewUUID(),
			Name:           form.Hostname,
			Ip:             form.Ip,
			Pin:            utils.CreateRandPin(),
			Status:         "Online",
			DeviceId:       form.DeviceId,
			PublicKey:      form.PublicKey,
			CreatedTime:    now,
			LastActiveTime: now,
		}
		global.DB.Create(&device)
	}

	// 返回设备基本信息
	deviceId := device.DeviceId
	if deviceId == "" {
		deviceId = device.ID
	}

	data := map[string]interface{}{
		"id":       device.ID,
		"deviceId": deviceId,
		"name":     device.Name,
		"ip":       device.Ip,
		"port":     47984,
		"status":   device.Status,
	}
	res.OkWithData(data, c)
}

// GetDeviceStatus 查询目标设备的连接状态（供免登录直连与登录后按 ID 查询复用）
// deviceId 从 URL 路径获取，device_password 从 Query 参数获取用于校验
func (DevicesApi) GetDeviceStatus(c *gin.Context) {
	deviceId := c.Param("deviceId")
	if deviceId == "" {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// device_password 必填校验（非局域网模式）
	devicePassword := c.Query("device_password")
	if devicePassword == "" {
		res.FailWithMsg("Device.PasswordRequired", c)
		return
	}

	var device models.Device
	// 优先通过 DeviceId 查找
	global.DB.Where("device_id = ?", deviceId).First(&device)
	// 如果没找到，尝试通过内部 ID 查找
	if device.ID == "" {
		global.DB.Where("id = ?", deviceId).First(&device)
	}

	if device.ID == "" {
		res.FailWithMsg("Device.NotFound", c)
		return
	}

	// 设备密码校验
	if device.DevicePassword != "" && device.DevicePassword != devicePassword {
		res.FailWithMsg("Device.PasswordMismatch", c)
		return
	}

	if device.Status != "Online" || device.Ip == "" {
		res.Fail(map[string]any{}, "目标设备当前不在线", c)
		return
	}

	resolvedDeviceId := device.DeviceId
	if resolvedDeviceId == "" {
		resolvedDeviceId = device.ID
	}

	data := map[string]interface{}{
		"deviceId":        resolvedDeviceId,
		"name":            device.Name,
		"ip":              device.Ip,
		"port":            47984,
		"pin":             device.Pin,
		"device_password": device.DevicePassword,
		"status":          device.Status,
		"paired":          false,
	}
	res.OkWithData(data, c)
}
