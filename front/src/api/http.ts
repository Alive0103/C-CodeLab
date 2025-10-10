import axios from 'axios'
import { useAuthStore } from '../stores/auth'

export const http = axios.create({ baseURL: '/api' })

http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.clear()
      // 避免在登录页面重复重定向
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)


