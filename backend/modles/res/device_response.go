package res

type GetDevicesListResponse struct {
	ID              string `json:"id"`
	Name            string `json:"name"`
	Ip              string `json:"ip"`
	Pin             string `json:"pin"`
	UserName        string `json:"username"`
	Status          string `json:"status"`
	CreatedTime     string `json:"createdTime"`
	DeviceId        string `json:"device_id"`          // 8位纯数字设备ID (V4)
	DevicePassword  string `json:"device_password"`   // 6位字母数字密码 (V4)
}
