/**
 * API工具函数集合
 */

const API_BASE_URL = 'http://localhost:8080/api'

/**
 * 获取认证头
 */
const getAuthHeaders = () => {
  const token = localStorage.getItem('token')
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

/**
 * 诊断历史API
 */
export const diagnosisHistoryAPI = {
  // 获取患者诊断历史
  getPatientHistory: async (patientId) => {
    const response = await fetch(`${API_BASE_URL}/history/diagnosis/patient/${patientId}`, {
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 获取医生诊断历史
  getDoctorHistory: async (doctorId, page = 0, size = 10) => {
    const response = await fetch(
      `${API_BASE_URL}/history/diagnosis/doctor/${doctorId}?page=${page}&size=${size}`,
      { headers: getAuthHeaders() }
    )
    return await response.json()
  },

  // 根据时间范围查询
  getHistoryByRange: async (startTime, endTime, page = 0, size = 10) => {
    const response = await fetch(
      `${API_BASE_URL}/history/diagnosis/range?startTime=${startTime}&endTime=${endTime}&page=${page}&size=${size}`,
      { headers: getAuthHeaders() }
    )
    return await response.json()
  }
}

/**
 * 操作日志API
 */
export const operationLogAPI = {
  // 获取用户操作日志
  getUserLogs: async (userId, page = 0, size = 20) => {
    const response = await fetch(
      `${API_BASE_URL}/logs/user/${userId}?page=${page}&size=${size}`,
      { headers: getAuthHeaders() }
    )
    return await response.json()
  },

  // 根据时间范围查询
  getLogsByRange: async (startTime, endTime, page = 0, size = 20) => {
    const response = await fetch(
      `${API_BASE_URL}/logs/range?startTime=${startTime}&endTime=${endTime}&page=${page}&size=${size}`,
      { headers: getAuthHeaders() }
    )
    return await response.json()
  },

  // 根据操作类型查询
  getLogsByType: async (type, page = 0, size = 20) => {
    const response = await fetch(
      `${API_BASE_URL}/logs/type/${type}?page=${page}&size=${size}`,
      { headers: getAuthHeaders() }
    )
    return await response.json()
  }
}

/**
 * 通知API
 */
export const notificationAPI = {
  // 获取未读通知
  getUnreadNotifications: async () => {
    const response = await fetch(`${API_BASE_URL}/notifications/unread`, {
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 获取未读数量
  getUnreadCount: async () => {
    const response = await fetch(`${API_BASE_URL}/notifications/count`, {
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 标记为已读
  markAsRead: async (id) => {
    const response = await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 全部标记为已读
  markAllAsRead: async () => {
    const response = await fetch(`${API_BASE_URL}/notifications/read-all`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    return await response.json()
  }
}

/**
 * 会诊API
 */
export const consultationAPI = {
  // 创建会诊申请
  createConsultation: async (consultation) => {
    const response = await fetch(`${API_BASE_URL}/consultation`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(consultation)
    })
    return await response.json()
  },

  // 接受会诊
  acceptConsultation: async (id) => {
    const response = await fetch(`${API_BASE_URL}/consultation/${id}/accept`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 完成会诊
  completeConsultation: async (id, opinion) => {
    const response = await fetch(
      `${API_BASE_URL}/consultation/${id}/complete?opinion=${encodeURIComponent(opinion)}`,
      {
        method: 'POST',
        headers: getAuthHeaders()
      }
    )
    return await response.json()
  },

  // 获取医生的会诊记录
  getDoctorConsultations: async (doctorId) => {
    const response = await fetch(`${API_BASE_URL}/consultation/doctor/${doctorId}`, {
      headers: getAuthHeaders()
    })
    return await response.json()
  },

  // 获取待处理会诊
  getPendingConsultations: async () => {
    const response = await fetch(`${API_BASE_URL}/consultation/pending`, {
      headers: getAuthHeaders()
    })
    return await response.json()
  }
}

/**
 * 导出API
 */
export const exportAPI = {
  // 导出诊断历史
  exportDiagnosisHistory: (startTime, endTime) => {
    window.open(
      `${API_BASE_URL}/export/diagnosis-history?startTime=${startTime}&endTime=${endTime}`,
      '_blank'
    )
  },

  // 导出操作日志
  exportOperationLogs: (startTime, endTime) => {
    window.open(
      `${API_BASE_URL}/export/operation-logs?startTime=${startTime}&endTime=${endTime}`,
      '_blank'
    )
  },

  // 导出分诊记录
  exportTriageRecords: (startTime, endTime) => {
    window.open(
      `${API_BASE_URL}/export/triage-records?startTime=${startTime}&endTime=${endTime}`,
      '_blank'
    )
  }
}
