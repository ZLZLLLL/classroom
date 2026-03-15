import { defineStore } from 'pinia'
import { login, register } from '../api/auth'
import { getCurrentUser } from '../api/user'
import type { UserInfo } from '../api/auth'

interface AuthState {
  token: string | null
  user: UserInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isTeacher: (state) => state.user?.role === 1,
    isStudent: (state) => state.user?.role === 2
  },

  actions: {
    async login(username: string, password: string) {
      const data = await login({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      return data
    },

    async register(data: { username: string; password: string; realName: string; role: number; classId?: number }) {
      const result = await register(data)
      return result
    },

    async fetchUser() {
      if (!this.token) return
      try {
        const user = await getCurrentUser()
        this.user = user
        localStorage.setItem('user', JSON.stringify(user))
      } catch (error) {
        this.logout()
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
