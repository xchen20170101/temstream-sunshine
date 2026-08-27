package routers

import (
	"gin-vue/api/utils"
	"gin-vue/global"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

func InitRouter() *gin.Engine {
	gin.SetMode(global.Config.System.Env)
	router := gin.Default()
	router.Use(func(c *gin.Context) {
		c.Writer.Header().Set("Content-Type", "application/json; charset=utf-8")
		c.Next()
	})
	cookieCheckMid := func() gin.HandlerFunc {
		return func(c *gin.Context) {
			if c.Request.URL.Path == "/api/stream/v1/login" || c.Request.URL.Path == "/api/stream/v1/reset_password" || c.Request.URL.Path == "/api/stream/v1/client/login" || c.Request.URL.Path == "/api/stream/v1/sunshine/register" || c.Request.URL.Path == "/api/stream/v1/sunshine/login" || c.Request.URL.Path == "/api/stream/v1/devices/online" || c.Request.URL.Path == "/api/stream/v1/devices/announce" || c.Request.URL.Path == "/api/stream/v1/register_and_bind" || c.Request.URL.Path == "/api/stream/v1/bind_user" || c.Request.URL.Path == "/api/stream/v1/change_password_by_device" || strings.HasPrefix(c.Request.URL.Path, "/api/stream/v1/devices/") && strings.HasSuffix(c.Request.URL.Path, "/status") {
				c.Next()
			} else {
				// 校验cookie,若校验不通过，可以使用c.Abort() 或 c.AbortWithStatus()方法
				accessToken, _ := c.Cookie("accessToken")
				userId, _ := c.Cookie("userId")
				checked := utils.CheckAccessToken(accessToken, userId)
				if checked {
					c.Next()
				} else {
					c.AbortWithStatus(http.StatusUnauthorized)
				}

			}

		}
	}
	router.Use(cookieCheckMid())

	router.Use(Cors())
	UsersRouter(router)
	DevicesRouter(router)
	return router
}

func Cors() gin.HandlerFunc {
	return func(context *gin.Context) {
		method := context.Request.Method
		context.Header("Access-Control-Allow-Origin", "*")
		context.Header("Access-Control-Allow-Headers", "Content-Type,AccessToken,X-CSRF-Token, Authorization, Token, x-token")
		context.Header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH, PUT")
		context.Header("Access-Control-Expose-Headers", "Content-Length, Access-Control-Allow-Origin, Access-Control-Allow-Headers, Content-Type")
		context.Header("Access-Control-Allow-Credentials", "true")
		if method == "OPTIONS" {
			context.AbortWithStatus(http.StatusNoContent)
		}
	}
}
