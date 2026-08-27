package res

type GetUserProfileReponse struct {
	UserName string `json:"username"`
	Password string `json:"password"`
}

type GetUserLicenseResponse struct {
	MachineCode string `json:"machineCode"`
	IsChecked   bool   `json:"isChecked"`
}

type GetLicensesResponse struct {
	MachineCode string `json:"machineCode"`
	LicenseCode string `json:"licenseCode"`
	LicenseType string `json:"licenseType"`
	ExpireTime  string `json:"expireTime"`
}

type DeviceInfo struct {
	DeviceId   string `json:"deviceId"`
	DeviceName string `json:"deviceName"`
	Ip         string `json:"ip"`
	Pin        string `json:"pin"`
}

type ClientLoginResponse struct {
	UserName string       `json:"userName"`
	Ip       string       `json:"ip"`
	Pin      string       `json:"pin"`
	Devices  []DeviceInfo `json:"devices"`
}

type SunshineLoginResponse struct {
	UserID   string `json:"userId"`
	UserName string `json:"username"`
	Role     string `json:"role"`
}