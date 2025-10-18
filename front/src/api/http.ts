import axios from 'axios'
import { useAuthStore } from '../stores/auth'

export const http = axios.create({ 
  baseURL: '/api',
  withCredentials: true
})

// 是否正在刷新token的标志
let isRefreshing = false
// 存储等待刷新完成的请求
let failedQueue: Array<{
  resolve: (value?: any) => void
  reject: (error?: any) => void
}> = []

// 处理等待队列
function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  
  failedQueue = []
}

// 请求拦截器：添加JWT token
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      // 确保token有Bearer前缀
      const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
      config.headers.Authorization = authToken
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理401/403错误和自动刷新token
http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const originalRequest = err.config

    // 处理401和403错误
    if ((err.response?.status === 401 || err.response?.status === 403) && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新token，将请求加入等待队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = token
          return http(originalRequest)
        }).catch(err => {
          return Promise.reject(err)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 获取当前token（可能是过期的）
        const currentToken = localStorage.getItem('token')
        if (!currentToken) {
          throw new Error('No token available for refresh')
        }

        // 尝试刷新token，发送当前token（即使过期）
        const refreshResponse = await http.post('/auth/refresh', {}, {
          headers: {
            Authorization: currentToken
          }
        })
        const newToken = refreshResponse.data.data
        
        if (newToken) {
          // 更新localStorage中的token
          localStorage.setItem('token', newToken)
          
          // 处理等待队列
          processQueue(null, newToken)
          
          // 重试原始请求
          originalRequest.headers.Authorization = newToken
          return http(originalRequest)
        }
      } catch (refreshError) {
        // 刷新失败，清除认证信息并显示错误提示
        processQueue(refreshError, null)
        const auth = useAuthStore()
        auth.clear()
        localStorage.removeItem('token')
        
        // 触发认证错误事件
        const authErrorEvent = new CustomEvent('auth-error', {
          detail: {
            message: '登录已过期，请重新登录',
            error: refreshError
          }
        })
        window.dispatchEvent(authErrorEvent)
        
        // 避免在登录页面重复重定向
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(err)
  }
)


