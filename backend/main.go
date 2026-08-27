package main

import (
	"gin-vue/core"
	"gin-vue/global"
	"gin-vue/routers"
)

func main() {
	//读取配置文件
	core.InitLogger()
	core.InitConf()
	//fmt.Println(global.Config.Vm.GetTemplate())
	//连接数据库
	global.DB = core.InitSqliteGorm()
	//
	router := routers.InitRouter()
	addr := global.Config.System.Addr()
	go core.CheckSunshineStatus()
	go core.DeviceMonitor()
	//core.InitTemplateInfo()
	// 暂时屏蔽该机制
	//go core.TriggerLicenseDateTask()
	//fmt.Printf("gvb server运行在：%s", addr)
	global.Logger.Printf("gvb server运行在：%s", addr)
	router.Run(addr)
}
