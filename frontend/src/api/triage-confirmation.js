import request from '@/utils/request'

/**
 * 分诊确认相关API
 */
export const triageConfirmationAPI = {
  /**
   * 获取待确认的分诊记录
   */
  getPendingRecords() {
    return request({
      url: '/triage-confirmation/pending',
      method: 'get'
    })
  },

  /**
   * 确认分诊结果
   * @param {number} recordId - 分诊记录ID
   * @param {object} confirmData - 确认数据
   */
  confirmTriage(recordId, confirmData) {
    return request({
      url: `/triage-confirmation/${recordId}/confirm`,
      method: 'post',
      data: confirmData
    })
  },

  /**
   * 拒绝分诊结果，要求重新分诊
   * @param {number} recordId - 分诊记录ID
   * @param {object} rejectData - 拒绝数据
   */
  rejectTriage(recordId, rejectData) {
    return request({
      url: `/triage-confirmation/${recordId}/reject`,
      method: 'post',
      data: rejectData
    })
  },

  /**
   * 获取分诊记录详情
   * @param {number} recordId - 分诊记录ID
   */
  getTriageDetail(recordId) {
    return request({
      url: `/triage-confirmation/${recordId}/detail`,
      method: 'get'
    })
  },

  /**
   * 获取分诊统计信息
   */
  getTriageStats() {
    return request({
      url: '/triage-confirmation/stats',
      method: 'get'
    })
  },

  /**
   * 批量确认分诊
   * @param {array} recordIds - 记录ID数组
   * @param {object} batchData - 批量操作数据
   */
  batchConfirm(recordIds, batchData) {
    return request({
      url: '/triage-confirmation/batch-confirm',
      method: 'post',
      data: {
        recordIds,
        ...batchData
      }
    })
  }
}