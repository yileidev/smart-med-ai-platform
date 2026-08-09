import request from '@/utils/request'

/**
 * 边缘设备API
 */
export const edgeDeviceApi = {
  /**
   * 获取边缘设备状态列表
   */
  getDeviceStatus() {
    return request.get('/edge/devices/status')
  },

  /**
   * 获取边缘数据统计
   */
  getDataStatistics() {
    return request.get('/edge/data/statistics')
  },

  /**
   * 获取未处理的边缘数据
   */
  getUnprocessedData(page = 0, size = 20) {
    return request.get('/edge/data/unprocessed', {
      params: { page, size }
    })
  },

  /**
   * 根据设备ID获取数据
   */
  getDataByDevice(deviceId, page = 0, size = 20) {
    return request.get(`/edge/data/device/${deviceId}`, {
      params: { page, size }
    })
  },

  /**
   * 获取数据质量报告
   */
  getDataQualityReport() {
    return request.get('/edge/data/quality-report')
  },

  /**
   * 重新处理失败数据
   */
  reprocessFailedData() {
    return request.post('/edge/data/reprocess-failed')
  },

  /**
   * 清理过期数据
   */
  cleanupExpiredData(daysToKeep = 30) {
    return request.delete('/edge/data/cleanup', {
      params: { daysToKeep }
    })
  },

  /**
   * 更新设备状态
   */
  updateDeviceStatus(deviceId, status, errorMessage = null) {
    return request.post(`/edge/devices/${deviceId}/status`, null, {
      params: { status, errorMessage }
    })
  },

  /**
   * 标记数据已处理
   */
  markDataProcessed(dataId) {
    return request.post(`/edge/data/${dataId}/mark-processed`)
  }
}

/**
 * 分诊相关API（从边缘设备接收的数据）
 */
export const triageApi = {
  /**
   * 接收边缘设备分诊数据（通常由MQTT处理，这里作为HTTP备份）
   */
  receiveEdgeData(edgeData) {
    return request.post('/triage/edge-data', edgeData)
  },

  /**
   * 获取待分诊患者列表
   */
  getPendingPatients() {
    return request.get('/triage/pending-patients')
  },

  /**
   * 护士确认分诊结果
   */
  confirmTriage(patientId, triageLevel, nurseNotes) {
    return request.post('/triage/confirm-triage', {
      patientId,
      triageLevel,
      nurseNotes
    })
  },

  /**
   * 获取待诊断患者列表（医生端）
   */
  getDiagnosisQueue(department = null) {
    return request.get('/triage/diagnosis-queue', {
      params: { department }
    })
  },

  /**
   * 提交诊断
   */
  submitDiagnosis(patientId, diagnosis, treatment, doctorId) {
    return request.post('/triage/submit-diagnosis', {
      patientId,
      diagnosis,
      treatment,
      doctorId
    })
  },

  /**
   * 获取AI诊断建议
   */
  getAIDiagnosis(patientId) {
    return request.post('/triage/ai-diagnosis', { patientId })
  },

  /**
   * 获取资源调度建议
   */
  getResourceAllocation(patientId) {
    return request.get(`/triage/resource-allocation/${patientId}`)
  },

  /**
   * 获取实时分诊统计
   */
  getTriageStatistics() {
    return request.get('/triage/statistics')
  },

  /**
   * 获取患者详细信息
   */
  getPatientDetail(patientId) {
    return request.get(`/triage/patient/${patientId}`)
  },

  /**
   * 更新患者状态
   */
  updatePatientStatus(patientId, status) {
    return request.post('/triage/update-status', {
      patientId,
      status
    })
  }
}

export default {
  edgeDeviceApi,
  triageApi
}