import axios from 'axios'
import ElementUI from "element-ui";
import router from './router';

const request = axios.create({
    //baseURL: 'http://127.0.0.1:8090',
    baseURL: '',
    timeout: 30000,
    withCredentials: true
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {

    config.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';
    return config
}, error => {
    return Promise.reject(error)
});

request.interceptors.response.use(response => {
    // 如果请求成功，则直接返回响应
    return response;
}, error => {
    // 处理401错误（未授权/Token失效）
    if (error.response && error.response.status === 401) {
        // 清除用户信息（如果有存储的话）
        localStorage.removeItem('user');
        localStorage.removeItem('token');

        // 获取当前路由路径（兼容不同版本的Vue Router）
        const currentPath = router.currentRoute ?
            (router.currentRoute.path || router.currentRoute.value?.path) :
            window.location.pathname;

        const currentFullPath = router.currentRoute ?
            (router.currentRoute.fullPath || router.currentRoute.value?.fullPath) :
            window.location.href;

        // 跳转到登录页面（如果当前不在登录页面）
        if (currentPath !== '/login' && currentPath !== '/') {
            router.push({
                path: '/login',
                query: { redirect: currentFullPath }
            });
        }

        // 显示提示信息
        if (ElementUI && ElementUI.Message) {
            ElementUI.Message.error('登录已过期，请重新登录');
        }
    }
    return Promise.reject(error);
});


export default request

