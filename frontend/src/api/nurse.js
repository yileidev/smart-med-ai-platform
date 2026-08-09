import request from '@/utils/request'

// 获取护士工作台统计
export function getNurseStats() {
  return request({
    url: '/nurse/stats',
    method: 'get'
  })
}

// 获取待分诊患者列表
export function getTriageQueue() {
  return request({
    url: '/nurse/triage-queue',
    method: 'get'
  })
}

// 患者分诊
export function submitTriage(data) {
  return request({
    url: '/nurse/triage',
    method: 'post',
    data
  })
}

// 更新患者生命体征
export function updateVitalSigns(patientId, vitalSigns) {
  return request({
    url: `/nurse/vital-signs/${patientId}`,
    method: 'put',
    data: vitalSigns
  })
}

// 获取分诊历史
export function getTriageHistory(params) {
  return request({
    url: '/nurse/triage-history',
    method: 'get',
    params
  })
}

// 患者分配科室
export function assignDepartment(patientId, department) {
  return request({
    url: `/nurse/assign-department/${patientId}`,
    method: 'put',
    data: { department }
  })
}

// 护士复核分诊（提交复核）
export function submitTriageReview(data) {
  // 将前端参数转换为后端期望的格式
  const requestData = {
    patientId: data.patientId, // 患者ID
    triageRecordId: data.triageRecordId || data.id, // 分诊记录ID - 后端优先使用这个
    triageLevel: data.confirmedLevel,
    nurseNotes: data.nurseNotes,
    // AI诊断结果 - 传递到医生端
    aiDiagnosis: data.aiDiagnosis || '',
    aiConfidence: data.aiConfidence || 0,
    recommendedDepartment: data.recommendedDepartment || '',
    // 生命体征数据 - 新增
    vitalSigns: data.vitalSigns || null
  }
  
  return request({
    url: '/nurse/triage/confirm',  // 使用NurseController的路径
    method: 'post',
    data: requestData
  })
}