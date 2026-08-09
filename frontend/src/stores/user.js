import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: [],
    currentRole: localStorage.getItem('currentRole') || ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.currentRole === 'ADMIN' || state.userInfo?.role === 'ADMIN',
    isDoctor: (state) => state.currentRole === 'DOCTOR' || state.userInfo?.role === 'DOCTOR',
    isNurse: (state) => state.currentRole === 'NURSE' || state.userInfo?.role === 'NURSE',
    getUserRole: (state) => {
      return state.currentRole || state.userInfo?.role || 'USER'
    },
    getRoleDisplayName: (state) => {
      const role = state.currentRole || 'USER'
      const roleNames = {
        'DOCTOR': '医生',
        'NURSE': '护士', 
        'ADMIN': '管理员',
        'USER': '用户'
      }
      return roleNames[role] || '用户'
    }
  },

  actions: {
    // 登录
    async login(loginForm) {
      try {
        console.log('UserStore - 开始登录:', loginForm)
        const response = await login(loginForm)
        console.log('UserStore - 登录响应:', response)
        
        const { token, userId, username, fullName, email, role, status } = response.data
        console.log('UserStore - 解析数据:', { token, userId, username, fullName, email, role, status })
        
        this.token = token
        this.userInfo = { 
          userId, 
          username, 
          fullName, 
          email, 
          role, 
          status,
          authorities: [{ authority: `ROLE_${role}` }]
        }
        this.currentRole = role
        
        console.log('UserStore - 设置状态:', {
          token: this.token,
          userInfo: this.userInfo,
          currentRole: this.currentRole
        })
        
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('currentRole', role)
        
        return response
      } catch (error) {
        console.error('UserStore - 登录失败:', error)
        this.logout()
        throw error
      }
    },

    // 登出
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.currentRole = ''
      
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('currentRole')
    },

    // 初始化用户信息
    initUser() {
      const token = localStorage.getItem('token')
      const userInfo = localStorage.getItem('userInfo')
      const currentRole = localStorage.getItem('currentRole')
      
      if (token && userInfo) {
        this.token = token
        this.userInfo = JSON.parse(userInfo)
        this.currentRole = currentRole || ''
      }
    },

    // 检查权限
    hasPermission(permission) {
      return this.permissions.includes(permission)
    }
  }
})