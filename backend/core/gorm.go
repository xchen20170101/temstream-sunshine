package core

import (
	"gin-vue/global"
	"time"

	"gin-vue/modles/models"

	"github.com/glebarez/sqlite"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	"gorm.io/gorm/schema"
)

// InitGorm 初始化gorm
func InitGorm() *gorm.DB {
	if global.Config.Mysql.Host == "" {
		global.Logger.Println("未配置mysql，取消gorm连接")
		return nil
	}
	dsn := global.Config.Mysql.Dsn()
	global.Logger.Println("DSN:", dsn)
	var mysqlLogger logger.Interface
	if global.Config.System.Env == "debug" { //判断环境
		//开发环境显示所有的sq1
		mysqlLogger = logger.Default.LogMode(logger.Info)
	} else {
		mysqlLogger = logger.Default.LogMode(logger.Error) // 只打印错误的sql
	}
	//global.MysqlLog = logger .Default .LogMode(logger.Info)
	db, err := gorm.Open(mysql.Open(dsn), &gorm.Config{
		Logger: mysqlLogger,
		NamingStrategy: schema.NamingStrategy{
			SingularTable: true, // 单数表名
		},
	})
	if err != nil {
		global.Logger.Println("MySql连接失败...")
	}
	db.AutoMigrate(&models.User{})
	sqlDB, _ := db.DB()
	sqlDB.SetMaxIdleConns(10)
	// 最大空闲连接数
	sqlDB.SetMaxOpenConns(100)
	// 最多可容纳
	sqlDB.SetConnMaxLifetime(time.Hour * 4) // 连接最大复用时间，不能超过mysql的wait_timeout
	return db
}

func InitSqliteGorm() *gorm.DB {

	db, err := gorm.Open(sqlite.Open("cloud_server.db"), &gorm.Config{})
	if err != nil {
		global.Logger.Println("sqlite connect failed")
	}
	//global.MysqlLog = logger .Default .LogMode(logger.Info)
	db.AutoMigrate(&models.User{})
	db.AutoMigrate(&models.Device{})
	db.AutoMigrate(&models.Bind{})
	db.AutoMigrate(&models.Token{})
	db.AutoMigrate(&models.License{})
	db.AutoMigrate(&models.LicenseDate{})

	// V4: 为 device_id 字段创建唯一索引（SQLite 和 MySQL 兼容）
	db.Exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_device_id ON devices(device_id)")
	// 保证同一设备在 binds 表中最多一条记录，应用层逻辑要求唯一归属
	db.Exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_bind_device_id ON binds(device_id)")

	InsertDefaultUser(db)
	return db
}

func InsertDefaultUser(db *gorm.DB) {
	var myUser models.User
	result := db.First(&myUser, "name=?", "admin")
	if myUser.Name == "admin" || result.RowsAffected > 1 {
		global.Logger.Println("default admin exists")
		return
	}
	myNewUser := models.User{ID: models.NewUUID(), Name: "admin", Password: "admin", Role: "管理员", Status: "启用"}
	res := db.Create(&myNewUser)
	if res.Error != nil {
		global.Logger.Panicln("init user failed")
	}
	global.Logger.Println("insert default admin user successfully")
}
