import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || ''
  }),
  actions: {
    setToken(token: string) {
      this.accessToken = token
      localStorage.setItem('accessToken', token)
    },
    clear() {
      this.accessToken = ''
      localStorage.removeItem('accessToken')
    }
  }
})


