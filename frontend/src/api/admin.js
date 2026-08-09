import request from '@/utils/request'

// 获取管理员概览统计
export function getOverviewStats() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

// 获取系统监控数据
export function getSystemMonitoring() {
  return request({
    url: '/admin/monitoring/realtime',
    method: 'get'
  })
}

// 获取用户列表
export function getUsers(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

// 获取医疗资源列表
export function getResources(params) {
  return request({
    url: '/admin/resources',
    method: 'get',
    params
  })
}

// 添加用户
export function addUser(data) {
  return request({
    url: '/admin/users',
    method: 'post',
    data
  })
}

// 更新用户
export function updateUser(userId, data) {
  return request({
    url: `/admin/users/${userId}`,
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(userId) {
  return request({
    url: `/admin/users/${userId}`,
    method: 'delete'
  })
}

// 添加医疗资源
export function addResource(data) {
  return request({
    url: '/admin/resources',
    method: 'post',
    data
  })
}

// 更新医疗资源
export function updateResource(resourceId, data) {
  return request({
    url: `/admin/resources/${resourceId}`,
    method: 'put',
    data
  })
}

// 删除医疗资源
export function deleteResource(resourceId) {
  return request({
    url: `/admin/resources/${resourceId}`,
    method: 'delete'
  })
}

// 获取系统日志
export function getSystemLogs(params) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params
  })
}

// 获取系统配置
export function getSystemConfig() {
  return request({
    url: '/admin/config',
    method: 'get'
  })
}

// 更新系统配置
export function updateSystemConfig(data) {
  return request({
    url: '/admin/config',
    method: 'put',
    data
  })
}

// 创建资源（别名：createResource）
export const createResource = addResource

// 创建用户（别名：createUser）
export const createUser = addUser

// 获取日志（别名：getLogs）
export const getLogs = getSystemLogs

// 获取实时监控（别名：getRealtimeMonitoring）
export const getRealtimeMonitoring = getSystemMonitoring

// 获取调度规则
export function getSchedulingRules() {
  return request({
    url: '/admin/scheduling-rules',
    method: 'get'
  })
}

// 更新调度规则
export function updateSchedulingRules(data) {
  return request({
    url: '/admin/scheduling-rules',
    method: 'put',
    data
  })
}

// 更新用户状态
export function updateUserStatus(userId, status) {
  return request({
    url: `/admin/users/${userId}/status`,
    method: 'put',
    data: { status }
  })
}

// 获取AI服务状态
export function getAIHealth() {
  return request({
    url: '/admin/ai/health',
    method: 'get'
  })
}

// 获取边缘设备列表
export function getEdgeDevices() {
  return request({
    url: '/admin/edge/devices',
    method: 'get'
  })
}

// 获取Drools状态
export function getDroolsStatus() {
  return request({
    url: '/admin/drools/status',
    method: 'get'
  })
}

// 获取Drools规则执行日志
export function getDroolsLogs(limit = 50) {
  return request({
    url: '/admin/drools/logs',
    method: 'get',
    params: { limit }
  })
}

// 重新加载Drools规则
export function reloadDroolsRules() {
  return request({
    url: '/admin/drools/reload',
    method: 'post'
  })
}

// 获取诊断历史统计
export function getDiagnosisStatistics(params) {
  return request({
    url: '/admin/statistics/diagnosis',
    method: 'get',
    params
  })
}

// 获取系统核心技术栈状态
export function getTechStackStatus() {
  return request({
    url: '/admin/tech-stack/status',
    method: 'get'
  })
}