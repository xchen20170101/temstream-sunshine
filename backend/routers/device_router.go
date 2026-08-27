package routers

import (
	"gin-vue/api"

	"github.com/gin-gonic/gin"
)

func DevicesRouter(router *gin.Engine) {
	deviceApi := api.ApiGroupApp.DevicesApi
	// 获取设备列表
	router.GET("/api/stream/v1/devices", deviceApi.GetDevicesList)
	// 获取设备数
	router.GET("/api/stream/v1/device_count", deviceApi.DeviceAllCountGet)
	// 删除sunshine
	router.DELETE("/api/stream/v1/device/:id", deviceApi.DeleteSunshine)
	// 编辑sunshine
	router.PUT("/api/stream/v1/device/:id", deviceApi.UpdateSunshine)
	// 分配
	router.POST("/api/stream/v1/distribute", deviceApi.DistributeDevice)
	// 回收
	router.POST("/api/stream/v1/recycle", deviceApi.RecycleDevice)
	// 新增：获取所有在线设备（供 Client 发现）
	router.GET("/api/stream/v1/devices/online", deviceApi.GetOnlineDevices)
	// 新增：Sunshine 上报设备信息（REST 通道）
	router.POST("/api/stream/v1/devices/announce", deviceApi.AnnounceDevice)
	// 新增：查询设备配对状态
	router.GET("/api/stream/v1/devices/:deviceId/status", deviceApi.GetDeviceStatus)
}
