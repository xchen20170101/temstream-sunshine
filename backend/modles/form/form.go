package form

// 新增用户
type AddUserForm struct {
	Name     string `form:"name" binding:"required"`
	Password string `form:"password" binding:"required"`
	Role     string `form:"role" binding:"required"`
	Status   string `form:"status" binding:"required"`
}

// 编辑用户
type UpdateUserForm struct {
	Password string `form:"password" binding:"required"`
	Role     string `form:"role" binding:"required"`
	Status   string `form:"status" binding:"required"`
}

// 绑定用户
type BindUserForm struct {
	UserId        string `form:"userId" binding:"required"`
	StrDeviceName string `form:"strDeviceName" binding:"required"`
}

// 授权激活
type ActiveLicenseForm struct {
	MachineCode string `form:"machineCode" binding:"required"`
	LicenseCode string `form:"licenseCode" binding:"required"`
}

type AddSunshineForm struct {
	Ip string `form:"ip" binding:"required"`
}

type UpdateSunshineForm struct {
	Ip string `form:"ip" binding:"required"`
}

type DistributeDeviceForm struct {
	DeviceId string `form:"deviceId" binding:"required"`
	UserId   string `form:"userId" binding:"required"`
}

type RecycleDeviceForm struct {
	DeviceId string `form:"deviceId" binding:"required"`
}

type ClientLoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// Sunshine注册请求
type SunshineRegisterRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

// Sunshine登录请求
type SunshineLoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

// Sunshine 上报设备信息请求
type AnnounceDeviceForm struct {
	DeviceId string `form:"device_id" json:"device_id" binding:"required"`
	Hostname string `form:"hostname" json:"hostname"`
	Ip       string `form:"ip" json:"ip"`
	PublicKey string `form:"public_key" json:"public_key"`
}

// 用户注册并绑定设备请求
type RegisterAndBindRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
	DeviceId string `json:"deviceId" binding:"required"`
}

// 根据设备修改密码请求
type ChangePasswordByDeviceRequest struct {
	Username    string `json:"username" binding:"required"`
	NewPassword string `json:"newpassword" binding:"required"`
	DeviceId    string `json:"device_id" binding:"required"`
}

// 绑定已有用户请求（无需注册新用户）
type BindExistingUserRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
	DeviceId string `json:"deviceId" binding:"required"`
}