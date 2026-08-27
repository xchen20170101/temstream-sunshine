package user_api

import (
	"gin-vue/api/utils"
	"gin-vue/global"
	"gin-vue/modles/form"
	"gin-vue/modles/models"
	"gin-vue/modles/res"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

func (UsersApi) UserCount(c *gin.Context) {
	var user []models.User
	rows := global.DB.Find(&user).RowsAffected
	res.OkWithData(rows, c)
}

// 修改密码
func (UsersApi) PasswordModify(c *gin.Context) {
	var myUser models.User

	result := global.DB.First(&myUser, "name", "admin")
	if result.Error != nil {
		res.FailWithMsg("failed to find user", c)
		return
	}

	userName := "admin"
	password := c.PostForm("password")
	userRole := "1"
	myNewUser := models.User{
		Name:     userName,
		Password: password,
		Role:     userRole,
	}

	global.DB.Model(myUser).Updates(myNewUser)
	//fmt.Println("user:", myNewUser)
	//res.Ok(user, "add user success", c)
	res.OkWithData(myNewUser, c)
}

// 安卓客户端重置密码
func (UsersApi) AndroidResetPassword(c *gin.Context) {
	var user models.User
	userName := c.PostForm("username")
	global.DB.Where("name =?", userName).First(&user)
	if user.Name == "" {
		res.FailWithMsg("User.NotExist", c)
		return
	}
	if user.Status != "启用" {
		res.FailWithMsg("User.Disable", c)
		return
	}
	password := c.PostForm("oldpassword")
	if user.Password != password {
		res.FailWithMsg("User.PasswordIsWrong", c)
		return
	}
	newPassword := c.PostForm("newpassword")
	myNewUser := models.User{
		Password: newPassword,
	}
	global.DB.Model(user).Updates(myNewUser)
	res.OkWithData(userName, c)
}

func (UsersApi) UserAdd(c *gin.Context) {
	var form form.AddUserForm
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("add user failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	userName := form.Name
	password := form.Password
	userRole := form.Role
	status := form.Status

	// 显式校验用户名非空
	if userName == "" {
		global.Logger.Println("add user failed: user name is empty")
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	// 校验用户名长度
	if len(userName) > 32 {
		global.Logger.Println("add user failed: user name too long")
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	// 校验密码长度
	if len(password) < 4 || len(password) > 32 {
		global.Logger.Println("add user failed: password length invalid")
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	// 校验角色值合法
	if userRole != "管理员" && userRole != "普通用户" {
		global.Logger.Println("add user failed: invalid role")
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	// 校验状态值合法
	if status != "启用" && status != "禁用" {
		global.Logger.Println("add user failed: invalid status")
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// 检查用户名是否已存在于管理系统
	var existingUser models.User
	if err := global.DB.Where("name = ?", userName).First(&existingUser).Error; err == nil {
		res.FailWithMsg("User.Exists", c)
		return
	}

	// 创建管理系统用户
	global.Logger.Printf("Creating management system user: %s\n", userName)

	var myUser models.User
	myUser.ID = models.NewUUID()
	myUser.Name = userName
	myUser.Password = password
	myUser.Role = userRole
	myUser.Status = status

	if err := global.DB.Create(&myUser).Error; err != nil {
		global.Logger.Printf("create user failed: %v\n", err)
		res.FailWithMsg("User.CreateFailed", c)
		return
	}

	global.Logger.Printf("User created successfully: %s\n", userName)
	res.OkWithData(myUser, c)
}

func (UsersApi) UserUpdate(c *gin.Context) {
	var myUser models.User
	id := c.Param("id")
	result := global.DB.First(&myUser, "id", id)
	if result.Error != nil {
		res.FailWithMsg("User.NotExist", c)
		return
	}
	var form form.UpdateUserForm

	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("update user failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	password := form.Password
	userRole := form.Role
	status := form.Status

	// admin不能修改角色
	if myUser.Name == "admin" && userRole == "云桌面用户" {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// admin不能被禁用
	if myUser.Name == "admin" && status == "禁用" {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// 用户名需要保持唯一
	// 不允许修改用户名
	myNewUser := models.User{
		Name:     myUser.Name,
		Password: password,
		Role:     userRole,
		Status:   status,
	}

	global.DB.Model(myUser).Updates(myNewUser)
	global.Logger.Println("user:", myNewUser)
	res.OkWithData(myNewUser, c)
}

func (UsersApi) UserUpdateSelfPassword(c *gin.Context) {
	var myUser models.User
	userId, err := c.Cookie("userId")
	if err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	result := global.DB.First(&myUser, "id", userId)
	if result.Error != nil {
		res.FailWithMsg("User.NotExist", c)
		return
	}

	password := c.PostForm("password")

	myNewUser := models.User{
		Name:     myUser.Name,
		Password: password,
	}

	global.DB.Model(myUser).Updates(myNewUser)
	res.OkWithData(myNewUser, c)
}

func (UsersApi) UserGet(c *gin.Context) {
	var users []models.User
	var totalNum int64
	count, _ := strconv.Atoi(c.Query("count"))
	pageNum, _ := strconv.Atoi(c.Query("index"))
	name := c.Query("name")
	keyword := "%" + name + "%"
	offset := (pageNum - 1) * count
	if name == "" {
		global.DB.Model(&models.User{}).Count(&totalNum)
		global.DB.Limit(count).Offset(offset).Find(&users)
	} else {
		global.DB.Model(&models.User{}).Where("name LIKE ?", keyword).Count(&totalNum)
		global.DB.Limit(count).Offset(offset).Where("name LIKE ?", keyword).Find(&users)
	}
	data := make(map[string]interface{})
	data["users"] = users
	data["totalNum"] = totalNum
	res.OkWithData(data, c)
}

func (UsersApi) UsersAllGet(c *gin.Context) {
	var users []models.User
	global.DB.Find(&users)
	data := make(map[string]interface{})
	data["users"] = users
	res.OkWithData(data, c)
}

// 获取所有用户数
func (UsersApi) UserAllCountGet(c *gin.Context) {
	var users []models.User
	global.DB.Find(&users)
	data := make(map[string]interface{})
	data["num"] = len(users)
	res.OkWithData(data, c)
}

// 获取用户信息
func (UsersApi) UserProfileGet(c *gin.Context) {
	userId, err := c.Cookie("userId")
	if err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	var user models.User
	global.DB.Where("id = ?", userId).First(&user)
	data := &res.GetUserProfileReponse{
		UserName: user.Name,
		Password: user.Password,
	}
	res.OkWithData(data, c)
}

// 获取授权信息
func (UsersApi) UserLicenseGet(c *gin.Context) {
	var license models.License
	var isChecked bool
	global.DB.First(&license)
	if license.LicenseCode == "" {
		isChecked = false
	}
	machineCode := utils.GetMachineCode()
	isChecked, _ = utils.CheckLicense(machineCode, license.LicenseCode)
	// 检查授权是否过期
	now := time.Now().UnixNano() / int64(time.Millisecond)
	//fmt.Println("now:", now)
	//fmt.Println("time:", license.ExpiredTime)
	if license.ExpiredTime != -1 && license.ExpiredTime != 0 && license.ExpireFlag != 2 {
		timeInterval := now - license.ExpiredTime
		//fmt.Println("timeInterval:", timeInterval)
		if timeInterval > 0 {
			//global.DB.Where("id = ?", license.ID).Delete(&models.License{})
			myLicense := models.License{
				ExpireFlag: 2,
			}
			global.DB.Model(license).Updates(myLicense)
			res.FailWithMsg("User.LicenseExpired", c)
			return
		}
	}

	if license.ExpireFlag == 2 {
		isChecked = false
	}

	// isSystemTimeModify := utils.CheckSystemTimeModify()
	// if !isSystemTimeModify {
	// 	// 因服务器时间变更导致授权异常，删除授权
	// 	global.DB.Where("license_code = ?", license.LicenseCode).Delete(&models.License{})
	// 	res.FailWithMsg("User.LicenseInvalid", c)
	// 	return
	// }

	data := &res.GetUserLicenseResponse{
		MachineCode: machineCode,
		IsChecked:   isChecked,
	}
	res.OkWithData(data, c)
}

// 授权激活
func (UsersApi) LicenseActive(c *gin.Context) {
	var license models.License
	var form form.ActiveLicenseForm
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("license active failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	if form.MachineCode != utils.GetMachineCode() {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	global.DB.Where("license_code = ?", form.LicenseCode).First(&license)
	if license.LicenseCode != "" {
		res.FailWithMsg("User.LicenseExists", c)
		return
	}
	isChecked, licenseType := utils.CheckLicense(utils.GetMachineCode(), form.LicenseCode)
	if !isChecked {
		res.FailWithMsg("User.LicenseActiveFailed", c)
		return
	}
	now := time.Now().UnixNano() / int64(time.Millisecond)
	license.ID = models.NewUUID()
	license.LicenseCode = form.LicenseCode
	license.LicenseType = licenseType
	license.ActiveTime = now
	var expireFlag int64
	var expiredTime int64
	if licenseType == "1" {
		expiredTime = now + 30*24*3600*1000
		expireFlag = 1
	} else {
		expiredTime = -1
		expireFlag = 0
	}
	license.ExpiredTime = expiredTime
	license.ExpireFlag = expireFlag
	global.DB.Create(&license)
	res.OkWithData(license, c)
}

// 授权查询
func (UsersApi) GetLicenses(c *gin.Context) {
	var licenses []models.License
	var myLicenses []*res.GetLicensesResponse
	global.DB.Where("expire_flag < ?", "2").Find(&licenses)
	data := make(map[string]interface{})
	for _, value := range licenses {
		var expireTime string
		if value.ExpiredTime == -1 {
			expireTime = "-"
		} else {
			expireTime = utils.TransferTimeStamp(value.ExpiredTime)
		}
		// 授权类型
		var licenseTypeTarget string
		if value.LicenseType == "1" {
			licenseTypeTarget = "测试授权"
		} else {
			licenseTypeTarget = "正式授权"
		}
		temp := &res.GetLicensesResponse{
			MachineCode: utils.GetMachineCode(),
			LicenseCode: value.LicenseCode,
			LicenseType: licenseTypeTarget,
			ExpireTime:  expireTime,
		}
		myLicenses = append(myLicenses, temp)
	}
	data["licenses"] = myLicenses
	res.OkWithData(data, c)
}

// 用户绑定设备
func (UsersApi) UserBindDevices(c *gin.Context) {
	// 1. 根据用户ID查找对应的用户信息，使用命令在云桌面里创建对应的用户
	// 2. 在bind表建立映射关系
	// var user models.User
	// var form form.BindUserForm
	// if err := c.ShouldBind(&form); err != nil {
	// 	global.Logger.Printf("user bind device failed:%v\n", err.Error())
	// 	res.FailWithMsg("Common.InvalidParam", c)
	// 	return
	// }
	// userId := form.UserId
	// deviceName := form.StrDeviceName
	// deviceNames := strings.Split(deviceName, ",")
	// global.DB.Where("id = ?", userId).First(&user)
	// if user.Name == "" {
	// 	res.FailWithMsg("User.NotExist", c)
	// 	return
	// }

	// for _, value := range deviceNames {
	// 	device := utils.GetDeviceByName(value)
	// 	if device == nil {
	// 		continue
	// 	}
	// 	var template models.Template
	// 	global.DB.Where("id =?", device.TemplateId).First(&template)
	// 	err := utils.CreateVMLocalUser(user.Name, user.Password, value, template.UserName, template.UserPwd)
	// 	if err != nil {
	// 		continue
	// 	}
	// 	myBind := &models.Bind{
	// 		ID:       models.NewUUID(),
	// 		DeviceId: device.ID,
	// 		UserId:   userId,
	// 	}
	// 	global.DB.Create(&myBind)
	// }
	//fmt.Println("userId:", userId)
	//fmt.Println("deviceName:", deviceName)
	res.OkWithData(nil, c)
}

func (UsersApi) UserDel(c *gin.Context) {
	var myUser models.User
	id := c.Param("id")
	queryResult := global.DB.First(&myUser, "id", id)
	if queryResult.Error != nil {
		res.FailWithMsg("User.NotExist", c)
		return
	}

	var myBinds []*models.Bind
	global.DB.Find(&myBinds, "user_id", id)
	if len(myBinds) > 0 {
		res.FailWithMsg("User.HasBind", c)
		return
	}

	// 删除数据库用户
	delResult := global.DB.Delete(&myUser)
	global.Logger.Println("userDel error:", delResult.Error)
	if delResult.Error != nil {
		res.FailWithMsg("User.NotExist", c)
		return
	}
	// 删除该用户的绑定关系
	global.DB.Where("user_id = ?", myUser.ID).Delete(&models.Bind{})

	res.OkWithData(myUser, c)
}

// UserLogin 用户登录
func (UsersApi) UserLogin(c *gin.Context) {
	// 1. 校验用户名和密码是否正确，若不正确，返回对应的错误
	// 2. 若校验通过，则生成access_token，存入token表
	// 3. 构造cookie（accessToken="xxxxx";userId=yyyyyy）,将cookie设置到response.headers中
	global.Logger.Println("UserAgent:", c.Request.UserAgent())

	var user models.User
	var token models.Token
	userName := c.PostForm("username")
	pwd := c.PostForm("password")
	global.DB.Where("name = ?", userName).First(&user)

	// web只允许管理员登录
	requestFrom := c.Request.UserAgent()
	if strings.Contains(requestFrom, "Windows") && user.Role == "普通用户" {
		res.FailWithMsg("User.NoPermission", c)
		return
	}

	if user.Password != pwd {
		res.FailWithMsg("User.PasswordIsWrong", c)
		return
	}

	if user.Status != "启用" {
		res.FailWithMsg("User.Disable", c)
		return
	}
	tokenValue := models.NewUUID()
	// myToken := &models.Token{
	// 	ID:           models.NewUUID(),
	// 	Value:        token_value,
	// 	Cryptoperiod: 30 * 60 * 1000,
	// 	CreatedTime:  time.Now().UnixNano() / int64(time.Millisecond),
	// }
	// global.DB.Create(&myToken)
	utils.UpdateOrCreateToken(user.ID, tokenValue)
	c.SetCookie("accessToken", tokenValue, 86400, "/", "", false, true)
	c.SetCookie("userId", user.ID, 86400, "/", "", false, true)
	global.DB.Where("user_id=?", user.ID).First(&token)

	res.Ok(token, "登录成功", c)
}

// 客户端登录
func (UsersApi) UserClientLogin(c *gin.Context) {
	var user models.User

	var form form.ClientLoginRequest
	if err := c.ShouldBind(&form); err != nil {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}
	global.Logger.Println("username:", form.Username)
	global.Logger.Println("password:", form.Password)
	global.DB.Where("name = ?", form.Username).First(&user)

	if user.Password != form.Password {
		res.FailWithMsg("User.PasswordIsWrong", c)
		return
	}

	if user.Status != "启用" {
		res.FailWithMsg("User.Disable", c)
		return
	}

	// V4: 查询该用户关联的所有设备（通过 Bind 表）
	var binds []models.Bind
	global.DB.Where("user_id = ?", user.ID).Find(&binds)

	if len(binds) == 0 {
		// 用户未绑定任何设备
		data := res.ClientLoginResponse{
			UserName: user.Name,
			Devices:  []res.DeviceInfo{},
		}
		res.Ok(data, "登录成功", c)
		return
	}

	// 根据 Bind 表中的 DeviceId 查找对应的 Device 信息
	var devices []res.DeviceInfo
	for _, bind := range binds {
		var dev models.Device
		// 优先使用 DeviceId 查找，否则使用 ID
		if bind.DeviceId != "" {
			global.DB.Where("device_id = ? OR id = ?", bind.DeviceId, bind.DeviceId).First(&dev)
		} else {
			global.DB.Where("id = ?", bind.DeviceId).First(&dev)
		}

		if dev.ID != "" && dev.Ip != "" {
			deviceId := dev.DeviceId
			if deviceId == "" {
				deviceId = dev.ID
			}
			deviceName := dev.Name
			if deviceName == "" {
				deviceName = "Unknown"
			}
			devices = append(devices, res.DeviceInfo{
				DeviceId:   deviceId,
				DeviceName: deviceName,
				Ip:         dev.Ip,
				Pin:        dev.Pin,
			})
		}
	}

	data := res.ClientLoginResponse{
		UserName: user.Name,
		Devices:  devices,
	}

	res.Ok(data, "登录成功", c)
}

func (UsersApi) UserLogout(c *gin.Context) {
	// 1. 从cookie中获取用户ID
	// 2. 删除token表中该用户对应的记录
	userId, err := c.Cookie("userId")
	if err != nil {
		res.FailWithMsg("User.LogoutFailed", c)
		return
	}
	global.DB.Where("user_id = ?", userId).Delete(&models.Token{})
	res.Ok(userId, "退出成功", c)
}

// Sunshine用户注册
func (UsersApi) SunshineRegister(c *gin.Context) {
	var form form.SunshineRegisterRequest
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("sunshine register failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	userName := form.Username
	password := form.Password

	// 检查用户名是否已存在于管理系统
	var existingUser models.User
	if err := global.DB.Where("name = ?", userName).First(&existingUser).Error; err == nil {
		res.FailWithMsg("User.Exists", c)
		return
	}

	// 创建管理系统用户
	global.Logger.Printf("Creating Sunshine user: %s\n", userName)

	var newUser models.User
	newUser.ID = models.NewUUID()
	newUser.Name = userName
	newUser.Password = password
	newUser.Role = "普通用户"
	newUser.Status = "启用"

	if err := global.DB.Create(&newUser).Error; err != nil {
		global.Logger.Printf("create user failed: %v\n", err)
		res.FailWithMsg("User.CreateFailed", c)
		return
	}

	global.Logger.Printf("Sunshine user registered successfully: %s\n", userName)
	res.OkWithData(newUser, c)
}

// Sunshine用户登录
func (UsersApi) SunshineLogin(c *gin.Context) {
	var form form.SunshineLoginRequest
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("sunshine login failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	var user models.User
	userName := form.Username
	password := form.Password

	global.DB.Where("name = ?", userName).First(&user)

	if user.Password != password {
		res.FailWithMsg("User.PasswordIsWrong", c)
		return
	}

	if user.Status != "启用" {
		res.FailWithMsg("User.Disable", c)
		return
	}

	// 生成token
	tokenValue := models.NewUUID()
	utils.UpdateOrCreateToken(user.ID, tokenValue)
	c.SetCookie("accessToken", tokenValue, 86400, "/", "", false, true)
	c.SetCookie("userId", user.ID, 86400, "/", "", false, true)

	data := res.SunshineLoginResponse{
		UserID:   user.ID,
		UserName: user.Name,
		Role:     user.Role,
	}

	global.Logger.Printf("Sunshine user logged in successfully: %s\n", userName)
	res.Ok(data, "登录成功", c)
}

// 用户注册并绑定设备
func (UsersApi) RegisterAndBind(c *gin.Context) {
	var form form.RegisterAndBindRequest
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("register and bind failed:%v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	userName := form.Username
	password := form.Password
	deviceId := form.DeviceId

	// 内置用户名不允许注册
	lowerName := strings.ToLower(userName)
	if lowerName == "admin" || lowerName == "root" {
		res.FailWithMsg("User.BuiltIn", c)
		return
	}

	// 检查设备是否存在
	var device models.Device
	if deviceId != "" {
		global.DB.Where("device_id = ? OR id = ?", deviceId, deviceId).First(&device)
	}
	if device.ID == "" {
		res.FailWithMsg("Device.NotExist", c)
		return
	}

	// 检查用户是否已存在
	var user models.User
	if err := global.DB.Where("name = ?", userName).First(&user).Error; err == nil {
		// 用户已存在
		res.FailWithMsg("User.Exists", c)
		return
	}

	// 创建新用户
	user = models.User{
		ID:       models.NewUUID(),
		Name:     userName,
		Password: password,
		Role:     "普通用户",
		Status:   "启用",
	}
	if err := global.DB.Create(&user).Error; err != nil {
		global.Logger.Printf("create user failed: %v\n", err)
		res.FailWithMsg("User.CreateFailed", c)
		return
	}

	// 建立/更新绑定关系
	var existingBind models.Bind
	global.DB.Where("device_id = ?", device.ID).First(&existingBind)

	switch {
	case existingBind.ID == "":
		// 设备未绑定 → 新建绑定
		bind := models.Bind{
			ID:       models.NewUUID(),
			DeviceId: device.ID,
			UserId:   user.ID,
		}
		if err := global.DB.Create(&bind).Error; err != nil {
			global.Logger.Printf("create bind failed: %v\n", err)
			res.FailWithMsg("Bind.CreateFailed", c)
			return
		}
		global.Logger.Printf("Bind created: user=%s, device=%s\n", userName, deviceId)
	case existingBind.UserId == user.ID:
		// 设备已绑给该用户 → 幂等成功，不重复插入
		global.Logger.Printf("Bind already exists (idempotent): user=%s, device=%s\n", userName, deviceId)
	default:
		// 设备已被他人绑定 → 覆盖所有权
		oldUserId := existingBind.UserId
		existingBind.UserId = user.ID
		if err := global.DB.Save(&existingBind).Error; err != nil {
			global.Logger.Printf("update bind failed: %v\n", err)
			res.FailWithMsg("Bind.UpdateFailed", c)
			return
		}
		global.Logger.Printf("Bind ownership transferred: device=%s from_user=%s to_user=%s\n",
			deviceId, oldUserId, userName)
	}

	global.Logger.Printf("User registered and bound successfully: user=%s, device=%s\n",
		userName, deviceId)
	res.OkWithData(map[string]interface{}{
		"userId":     user.ID,
		"userName":   user.Name,
		"deviceId":   device.DeviceId,
		"deviceName": device.Name,
	}, c)
}

// 绑定已有用户（用于 Sunshine 配置工具的"绑定已有用户"功能）
func (UsersApi) BindExistingUser(c *gin.Context) {
	var form form.BindExistingUserRequest
	if err := c.ShouldBind(&form); err != nil {
		global.Logger.Printf("bind existing user failed: %v\n", err.Error())
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	userName := form.Username
	password := form.Password
	deviceId := form.DeviceId

	// 检查设备是否存在
	var device models.Device
	if deviceId != "" {
		global.DB.Where("device_id = ? OR id = ?", deviceId, deviceId).First(&device)
	}
	if device.ID == "" {
		res.FailWithMsg("Device.NotExist", c)
		return
	}

	// 检查用户是否存在
	var user models.User
	if err := global.DB.Where("name = ?", userName).First(&user).Error; err != nil {
		global.Logger.Printf("bind existing user failed: user not found: %s\n", userName)
		res.FailWithMsg("User.NotExist", c)
		return
	}

	// 验证密码（简单匹配，不使用加密）
	if user.Password != password {
		res.FailWithMsg("User.PasswordIsWrong", c)
		return
	}

	// 建立/更新绑定关系
	var existingBind models.Bind
	global.DB.Where("device_id = ?", device.ID).First(&existingBind)

	switch {
	case existingBind.ID == "":
		// 设备未绑定 → 新建绑定
		bind := models.Bind{
			ID:       models.NewUUID(),
			DeviceId: device.ID,
			UserId:   user.ID,
		}
		if err := global.DB.Create(&bind).Error; err != nil {
			global.Logger.Printf("create bind failed: %v\n", err)
			res.FailWithMsg("Bind.CreateFailed", c)
			return
		}
		global.Logger.Printf("Bind created (existing user): user=%s, device=%s\n", userName, deviceId)
	case existingBind.UserId == user.ID:
		// 设备已绑给该用户 → 幂等成功
		global.Logger.Printf("Bind already exists (idempotent): user=%s, device=%s\n", userName, deviceId)
	default:
		// 设备已被他人绑定 → 覆盖所有权
		oldUserId := existingBind.UserId
		existingBind.UserId = user.ID
		if err := global.DB.Save(&existingBind).Error; err != nil {
			global.Logger.Printf("update bind failed: %v\n", err)
			res.FailWithMsg("Bind.UpdateFailed", c)
			return
		}
		global.Logger.Printf("Bind ownership transferred: device=%s from_user=%s to_user=%s\n",
			deviceId, oldUserId, userName)
	}

	global.Logger.Printf("Existing user bound successfully: user=%s, device=%s\n", userName, deviceId)
	res.OkWithData(map[string]interface{}{
		"userId":      user.ID,
		"userName":    user.Name,
		"deviceId":    device.DeviceId,
		"deviceName":  device.Name,
		"userExisted": true,
	}, c)
}

// 根据设备ID修改用户密码（无需认证）
func (UsersApi) ChangePasswordByDevice(c *gin.Context) {
	var req form.ChangePasswordByDeviceRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		global.Logger.Printf("change password bind failed: %v\n", err)
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// 校验新密码长度
	if len(req.NewPassword) < 4 || len(req.NewPassword) > 32 {
		res.FailWithMsg("Common.InvalidParam", c)
		return
	}

	// 根据 device_id 查询设备
	var device models.Device
	global.DB.Where("device_id = ? OR id = ?", req.DeviceId, req.DeviceId).First(&device)
	if device.ID == "" {
		res.FailWithMsg("Device.NotExist", c)
		return
	}

	// 根据 username 查询用户
	var user models.User
	global.DB.Where("name = ?", req.Username).First(&user)
	if user.ID == "" {
		res.FailWithMsg("User.NotExist", c)
		return
	}

	// 校验用户是否绑定在该设备上
	var bind models.Bind
	global.DB.Where("device_id = ? AND user_id = ?", device.ID, user.ID).First(&bind)
	if bind.ID == "" {
		res.FailWithMsg("User.NoPermission", c)
		return
	}

	// 更新用户密码
	myNewUser := models.User{
		Name:     user.Name,
		Password: req.NewPassword,
	}
	if err := global.DB.Model(user).Updates(&myNewUser).Error; err != nil {
		global.Logger.Printf("change password failed: %v\n", err)
		res.FailWithMsg("Common.InternalError", c)
		return
	}

	global.Logger.Printf("Password changed successfully: user=%s, device=%s\n", req.Username, req.DeviceId)
	res.OkWithData(nil, c)
}