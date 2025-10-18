import { defineStore } from 'pinia'

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
    user: null as User | null
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
    }
  }
})


