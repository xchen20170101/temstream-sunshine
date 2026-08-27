package global

import (
	"gin-vue/config"
	"log"

	"gorm.io/gorm"
)

// 全局变量
var (
	Config *config.Config
	DB     *gorm.DB
	Logger *log.Logger
)
