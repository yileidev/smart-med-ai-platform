import request from '@/utils/request'

// 用户登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username: data.username,
      password: data.password,
      role: data.role
    }
  })
}

// 用户登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

// 检查token有效性
export function checkToken() {
  return request({
    url: '/auth/check',
    method: 'get'
  })
}