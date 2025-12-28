import { defineStore } from 'pinia'
import { logout as apiLogout } from '../api/auth'

interface User {
  id: number
  username: string
  email: string
  role: string
  enabled: boolean
  createdAt: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    isRefreshing: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.user
  },
  actions: {
    setUser(user: User) {
      this.user = user
    },
    clear() {
      this.user = null
      this.isRefreshing = false
    },
    setRefreshing(refreshing: boolean) {
      this.isRefreshing = refreshing
    },
    async logout() {
      try {
        await apiLogout()
      } catch (error) {
        console.error('登出请求失败:', error)
        // 即使请求失败，也清除本地状态
      } finally {
        // 清除本地存储的 token 和用户信息
        localStorage.removeItem('token')
        this.clear()
      }
    }
  }
})


