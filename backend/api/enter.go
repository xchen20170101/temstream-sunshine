package api

import (
	"gin-vue/api/devices_api"
	"gin-vue/api/user_api"
)

type ApiGroup struct {
	UserApi    user_api.UsersApi      //用户管理
	DevicesApi devices_api.DevicesApi //设备管理
}

var ApiGroupApp = new(ApiGroup)
