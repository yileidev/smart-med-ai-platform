import request from '@/utils/request'

// 获取医生工作台统计
export function getDoctorStats() {
  return request({
    url: '/doctor/stats',
    method: 'get'
  })
}

// 获取患者队列
export function getPatientQueue() {
  return request({
    url: '/doctor/patient-queue',
    method: 'get'
  })
}

// 获取患者详情
export function getPatientDetail(patientId) {
  return request({
    url: `/doctor/patient/${patientId}`,
    method: 'get'
  })
}

// 提交诊断
export function submitDiagnosis(data) {
  return request({
    url: '/doctor/diagnosis',
    method: 'post',
    data
  })
}

// 获取AI诊断建议
export function getAIDiagnosis(patientId) {
  return request({
    url: `/doctor/ai-diagnosis/${patientId}`,
    method: 'get'
  })
}

// 申请会诊
export function requestConsultation(data) {
  return request({
    url: '/doctor/consultation',
    method: 'post',
    data
  })
}