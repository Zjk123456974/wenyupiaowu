import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    avatar: localStorage.getItem('avatar') || '',
    role: parseInt(localStorage.getItem('role') || '0'),
    operatorType: parseInt(localStorage.getItem('operatorType') || '0'),
    unreadCount: 0
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === 2,
    isOperator: (state) => state.role === 1 || state.role === 2,
    // 活动运营：可进入电子票核销端，仅能核销本人负责的活动
    isActivityOperator: (state) => state.role === 1 && state.operatorType === 1,
    // 可核销身份：管理员（全部活动）或活动运营（本人活动）
    canVerifyTicket: (state) => state.role === 2 || (state.role === 1 && state.operatorType === 1)
  },
  actions: {
    setLoginInfo(data) {
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.nickname = data.nickname || ''
      this.avatar = data.avatar || ''
      this.role = data.role
      this.operatorType = data.operatorType || 0
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', data.userId)
      localStorage.setItem('username', data.username)
      localStorage.setItem('nickname', data.nickname || '')
      localStorage.setItem('avatar', data.avatar || '')
      localStorage.setItem('role', data.role)
      localStorage.setItem('operatorType', data.operatorType || 0)
    },
    updateProfile(data) {
      this.nickname = data.nickname || this.nickname
      this.avatar = data.avatar || this.avatar
      localStorage.setItem('nickname', data.nickname || this.nickname)
      localStorage.setItem('avatar', data.avatar || this.avatar)
    },
    setUnreadCount(count) {
      this.unreadCount = count
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      this.avatar = ''
      this.role = 0
      this.operatorType = 0
      this.unreadCount = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('avatar')
      localStorage.removeItem('role')
      localStorage.removeItem('operatorType')
    }
  }
})
