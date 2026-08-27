package models

// Device 设备表
type Device struct {
	ID              string `json:"id"`
	Name            string `json:"name"`
	Ip              string `json:"ip"`
	Pin             string `json:"pin"`
	DevicePassword  string `json:"device_password"` // 6位字母数字密码，非局域网模式必填
	Status          string `json:"status"`
	PublicKey       string `json:"public_key"`   // 保留字段，兼容旧 Sunshine（V4 不使用）
	DeviceId        string `json:"device_id"`    // 8位纯数字设备ID (V4)，唯一索引
	CreatedTime     int64  `json:"created_time"`
	LastActiveTime  int64  `json:"last_active_time"`
}
