import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建一个 axios 实例
const request = axios.create({
    baseURL: 'http://localhost:8080', //  Spring Boot 后端地址
    timeout: 5000 // 请求超时时间
})

// 请求拦截器：发请求前做点什么
request.interceptors.request.use(
    config => {
        // 从浏览器的本地存储中取出用户在登录时存进去的 Token
        const token = localStorage.getItem('token')
        if (token) {
            // 给所有的请求头都加上 JWT 通行证
            config.headers['Authorization'] = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：收到后端回复后做点什么
request.interceptors.response.use(
    response => {
        let res = response.data
        // 如果后端返回的 code  ( 1 是成功，0 是失败，根据 R.java 调整)
        if (res.code === 0) {
            ElMessage.error(res.msg || '操作失败')
            return Promise.reject(res.msg)
        }
        return res
    },
    error => {
        ElMessage.error('网络异常或服务器错误')
        return Promise.reject(error)
    }
)

export default request