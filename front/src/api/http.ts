import axios from 'axios'
import { useAuthStore } from '../stores/auth'

export const http = axios.create({ 
  baseURL: '/api',
  withCredentials: true
})

// 请求拦截器：添加JWT token
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理401错误
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.clear()
      localStorage.removeItem('token')
      // 避免在登录页面重复重定向
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)


