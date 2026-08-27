import Vue from 'vue'
import VueRouter from 'vue-router'
import store from "@/store";

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'login',
    component: () => import("../views/login.vue")
  },
  {
    path: '/login',
    name: 'login',
    component: () => import("../views/login.vue")
  },
  {
    path: '/main',
    name: 'main',
    component: () => import("../views/main"),
    redirect: "/home",
    children: [
      {
        path: '/home',
        name: '主页',
        component: () => import("../views/Home.vue")
      },
      {
        path: '/user',
        name: '用户管理',
        component: () => import("../views/User.vue")
      },
      {
        path: '/device',
        name: '设备管理',
        component: () => import("../views/Device.vue")
      },
    ]
  },

]

const router = new VueRouter({
  mode: 'history',
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  localStorage.setItem("currentPathName", to.name)  // 设置当前的路由名称，为了在Header组件中去使用
  store.commit("setPath")  // 触发store的数据更新
  next()  // 放行路由
})

export default router
