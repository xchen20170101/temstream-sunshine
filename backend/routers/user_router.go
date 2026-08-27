package routers

import (
	"gin-vue/api"

	"github.com/gin-gonic/gin"
)

func UsersRouter(router *gin.Engine) {
	userApi := api.ApiGroupApp.UserApi
	// 登录接口
	router.POST("/api/stream/v1/login", userApi.UserLogin)
	// 客户端登录接口
	router.POST("/api/stream/v1/client/login", userApi.UserClientLogin)
	// Sunshine登录接口
	router.POST("/api/stream/v1/sunshine/login", userApi.SunshineLogin)
	// Sunshine注册接口
	router.POST("/api/stream/v1/sunshine/register", userApi.SunshineRegister)
	// 安卓客户端修改密码
	router.POST("/api/stream/v1/reset_password", userApi.AndroidResetPassword)
	// 登出接口
	router.DELETE("/api/stream/v1/logout", userApi.UserLogout)
	// 新增用户
	router.POST("/api/stream/v1/users", userApi.UserAdd)
	// 获取用户列表
	router.GET("/api/stream/v1/users", userApi.UserGet)
	// 获取全部用户列表（含已绑定的）
	router.GET("/api/stream/v1/users_all", userApi.UsersAllGet)
	// 删除用户
	router.DELETE("/api/stream/v1/user/:id", userApi.UserDel)
	// 修改用户信息（包含用户的启用、禁用）
	router.PUT("/api/stream/v1/user/:id", userApi.UserUpdate)
	// 用户数获取
	router.GET("/api/stream/v1/user_count", userApi.UserAllCountGet)
	// 用户绑定云桌面
	router.POST("/api/stream/v1/user_bind", userApi.UserBindDevices)
	// 用户个人信息获取
	router.GET("/api/stream/v1/user_profile", userApi.UserProfileGet)
	// 用户密码修改（修改自身密码）
	router.PUT("/api/stream/v1/user", userApi.UserUpdateSelfPassword)
	// 获取授权信息
	router.GET("/api/stream/v1/licenses", userApi.UserLicenseGet)
	// 授权激活
	router.POST("/api/stream/v1/licenses", userApi.LicenseActive)
	// 授权查询
	router.GET("/api/stream/v1/licenses_all", userApi.GetLicenses)
	// 用户注册并绑定设备
	router.POST("/api/stream/v1/register_and_bind", userApi.RegisterAndBind)
	// 绑定已有用户（用于 Sunshine 配置工具）
	router.POST("/api/stream/v1/bind_user", userApi.BindExistingUser)
	// 根据设备ID修改用户密码（无需认证）
	router.POST("/api/stream/v1/change_password_by_device", userApi.ChangePasswordByDevice)
}
