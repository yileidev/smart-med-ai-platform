<template>
  <div class="triage-confirmation">
    <div class="page-header">
      <h2>分诊确认</h2>
      <p>请确认边缘设备的分诊结果</p>
    </div>

    <!-- 实时通知 -->
    <div v-if="newTriageAlert" class="alert alert-warning">
      <i class="fas fa-exclamation-triangle"></i>
      有新的分诊数据需要确认！
      <button @click="refreshData" class="btn btn-sm btn-primary ml-2">刷新</button>
    </div>

    <!-- 待确认列表 -->
    <div class="pending-list">
      <div class="list-header">
        <h3>待确认分诊 ({{ pendingRecords.length }})</h3>
        <div class="filters">
          <select v-model="levelFilter" @change="filterRecords">
            <option value="">所有等级</option>
            <option value="1">I级 (红色)</option>
            <option value="2">II级 (橙色)</option>
            <option value="3">III级 (黄色)</option>
            <option value="4">IV级 (绿色)</option>
            <option value="5">V级 (蓝色)</option>
          </select>
        </div>
      </div>

      <div class="records-grid">
        <div v-for="record in filteredRecords" :key="record.id" 
             class="record-card" :class="getTriageLevelClass(record.triageLevel)">
          
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="patient-info">
              <h4>{{ record.patient?.patientName || '临时患者' }}</h4>
              <span class="patient-id">ID: {{ record.patient?.id || record.patientTempId }}</span>
            </div>
            <div class="triage-badge" :class="getTriageBadgeClass(record.triageLevel)">
              {{ getTriageLevelText(record.triageLevel) }}
            </div>
          </div>

          <!-- 基本信息 -->
          <div class="card-content">
            <div class="info-row">
              <label>主诉：</label>
              <span>{{ record.chiefComplaint }}</span>
            </div>
            
            <div class="info-row">
              <label>到院时间：</label>
              <span>{{ formatTime(record.arrivalTime) }}</span>
            </div>

            <div class="info-row">
              <label>边缘AI诊断：</label>
              <span>{{ record.aiDiagnosis }}</span>
            </div>

            <div class="info-row">
              <label>AI置信度：</label>
              <span>{{ (record.aiConfidence * 100).toFixed(1) }}%</span>
            </div>

            <!-- 生命体征 -->
            <div class="vital-signs" v-if="record.vitalSigns">
              <h5>生命体征</h5>
              <div class="signs-grid">
                <div class="sign-item">
                  <label>体温</label>
                  <span>{{ getVitalSign(record.vitalSigns, 'temperature') }}°C</span>
                </div>
                <div class="sign-item">
                  <label>心率</label>
                  <span>{{ getVitalSign(record.vitalSigns, 'heartRate') }}/分</span>
                </div>
                <div class="sign-item">
                  <label>血氧</label>
                  <span>{{ getVitalSign(record.vitalSigns, 'bloodOxygen') }}%</span>
                </div>
                <div class="sign-item">
                  <label>血压</label>
                  <span>{{ getVitalSign(record.vitalSigns, 'systolicBP') }}/{{ getVitalSign(record.vitalSigns, 'diastolicBP') }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="card-actions">
            <button @click="viewDetails(record)" class="btn btn-info">
              <i class="fas fa-eye"></i> 详情
            </button>
            <button @click="confirmTriage(record)" class="btn btn-success">
              <i class="fas fa-check"></i> 确认
            </button>
            <button @click="rejectTriage(record)" class="btn btn-warning">
              <i class="fas fa-times"></i> 重新分诊
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认对话框 -->
    <div v-if="showConfirmDialog" class="modal-overlay" @click="closeConfirmDialog">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>确认分诊等级</h3>
          <button @click="closeConfirmDialog" class="close-btn">&times;</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>确认分诊等级：</label>
            <select v-model="confirmForm.confirmedTriageLevel">
              <option value="1">I级 (红色) - 濒危</option>
              <option value="2">II级 (橙色) - 危急</option>
              <option value="3">III级 (黄色) - 急症</option>
              <option value="4">IV级 (绿色) - 次急症</option>
              <option value="5">V级 (蓝色) - 非急症</option>
            </select>
          </div>

          <div class="form-group">
            <label>护士备注：</label>
            <textarea v-model="confirmForm.nurseComments" 
                     placeholder="请输入确认备注或调整原因"></textarea>
          </div>

          <!-- 患者信息补充 -->
          <div class="patient-update-section">
            <h4>补充患者信息</h4>
            <div class="form-row">
              <div class="form-group">
                <label>姓名：</label>
                <input v-model="confirmForm.patientInfo.name" type="text" />
              </div>
              <div class="form-group">
                <label>年龄：</label>
                <input v-model="confirmForm.patientInfo.age" type="number" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>性别：</label>
                <select v-model="confirmForm.patientInfo.gender">
                  <option value="男">男</option>
                  <option value="女">女</option>
                </select>
              </div>
              <div class="form-group">
                <label>电话：</label>
                <input v-model="confirmForm.patientInfo.phoneNumber" type="tel" />
              </div>
            </div>
          </div>

          <!-- 生命体征补充 -->
          <div class="vitals-update-section">
            <h4>补充生命体征</h4>
            <div class="vitals-grid">
              <div class="form-group">
                <label>收缩压：</label>
                <input v-model="vitalSignsUpdate.systolicBP" type="number" />
              </div>
              <div class="form-group">
                <label>舒张压：</label>
                <input v-model="vitalSignsUpdate.diastolicBP" type="number" />
              </div>
              <div class="form-group">
                <label>呼吸频率：</label>
                <input v-model="vitalSignsUpdate.respiratoryRate" type="number" />
              </div>
              <div class="form-group">
                <label>意识状态：</label>
                <select v-model="vitalSignsUpdate.consciousness">
                  <option value="清醒">清醒</option>
                  <option value="嗜睡">嗜睡</option>
                  <option value="昏迷">昏迷</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="closeConfirmDialog" class="btn btn-secondary">取消</button>
          <button @click="submitConfirmation" class="btn btn-success" :disabled="submitting">
            {{ submitting ? '处理中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 拒绝对话框 -->
    <div v-if="showRejectDialog" class="modal-overlay" @click="closeRejectDialog">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>重新分诊</h3>
          <button @click="closeRejectDialog" class="close-btn">&times;</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>拒绝原因：</label>
            <textarea v-model="rejectForm.reason" 
                     placeholder="请说明需要重新分诊的原因"></textarea>
          </div>

          <div class="form-group">
            <label>更新主诉：</label>
            <textarea v-model="rejectForm.updatedChiefComplaint" 
                     placeholder="如需要，请更新患者的主诉信息"></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="closeRejectDialog" class="btn btn-secondary">取消</button>
          <button @click="submitRejection" class="btn btn-warning" :disabled="submitting">
            {{ submitting ? '处理中...' : '重新分诊' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { triageConfirmationAPI } from '@/api/triage-confirmation'
import { useWebSocket } from '@/composables/useWebSocket'

export default {
  name: 'TriageConfirmation',
  setup() {
    const pendingRecords = ref([])
    const filteredRecords = ref([])
    const levelFilter = ref('')
    const newTriageAlert = ref(false)
    const submitting = ref(false)

    // 对话框状态
    const showConfirmDialog = ref(false)
    const showRejectDialog = ref(false)
    const selectedRecord = ref(null)

    // 表单数据
    const confirmForm = ref({
      confirmedTriageLevel: null,
      nurseComments: '',
      patientInfo: {
        name: '',
        age: null,
        gender: '',
        phoneNumber: '',
        idCard: ''
      }
    })

    const vitalSignsUpdate = ref({
      systolicBP: null,
      diastolicBP: null,
      respiratoryRate: null,
      consciousness: '清醒'
    })

    const rejectForm = ref({
      reason: '',
      updatedChiefComplaint: ''
    })

    // WebSocket连接
    const { connect, disconnect, subscribe } = useWebSocket()

    // 获取待确认数据
    const fetchPendingRecords = async () => {
      try {
        const response = await triageConfirmationAPI.getPendingRecords()
        pendingRecords.value = response.records || []
        filterRecords()
      } catch (error) {
        console.error('获取待确认记录失败:', error)
      }
    }

    // 过滤记录
    const filterRecords = () => {
      if (levelFilter.value) {
        filteredRecords.value = pendingRecords.value.filter(
          record => record.triageLevel == levelFilter.value
        )
      } else {
        filteredRecords.value = [...pendingRecords.value]
      }
    }

    // 刷新数据
    const refreshData = () => {
      fetchPendingRecords()
      newTriageAlert.value = false
    }

    // 查看详情
    const viewDetails = (record) => {
      // 实现详情查看逻辑
      console.log('查看详情:', record)
    }

    // 确认分诊
    const confirmTriage = (record) => {
      selectedRecord.value = record
      confirmForm.value.confirmedTriageLevel = record.triageLevel
      
      // 预填充患者信息
      if (record.patient) {
        confirmForm.value.patientInfo = {
          name: record.patient.patientName || '',
          age: record.patient.age || null,
          gender: record.patient.gender || '',
          phoneNumber: record.patient.phoneNumber || '',
          idCard: record.patient.idCard || ''
        }
      }
      
      showConfirmDialog.value = true
    }

    // 拒绝分诊
    const rejectTriage = (record) => {
      selectedRecord.value = record
      rejectForm.value.updatedChiefComplaint = record.chiefComplaint
      showRejectDialog.value = true
    }

    // 提交确认
    const submitConfirmation = async () => {
      submitting.value = true
      try {
        // 构建生命体征数据
        const currentVitals = JSON.parse(selectedRecord.value.vitalSigns || '{}')
        const updatedVitals = {
          ...currentVitals,
          ...vitalSignsUpdate.value
        }
        confirmForm.value.updatedVitalSigns = JSON.stringify(updatedVitals)

        await triageConfirmationAPI.confirmTriage(selectedRecord.value.id, confirmForm.value)
        
        closeConfirmDialog()
        refreshData()
        
        // 显示成功消息
        alert('分诊确认成功！')
      } catch (error) {
        console.error('确认失败:', error)
        alert('确认失败: ' + error.message)
      } finally {
        submitting.value = false
      }
    }

    // 提交拒绝
    const submitRejection = async () => {
      submitting.value = true
      try {
        await triageConfirmationAPI.rejectTriage(selectedRecord.value.id, rejectForm.value)
        
        closeRejectDialog()
        refreshData()
        
        alert('已提交重新分诊请求！')
      } catch (error) {
        console.error('拒绝失败:', error)
        alert('拒绝失败: ' + error.message)
      } finally {
        submitting.value = false
      }
    }

    // 关闭对话框
    const closeConfirmDialog = () => {
      showConfirmDialog.value = false
      selectedRecord.value = null
      // 重置表单
      confirmForm.value = {
        confirmedTriageLevel: null,
        nurseComments: '',
        patientInfo: { name: '', age: null, gender: '', phoneNumber: '', idCard: '' }
      }
      vitalSignsUpdate.value = {
        systolicBP: null, diastolicBP: null, respiratoryRate: null, consciousness: '清醒'
      }
    }

    const closeRejectDialog = () => {
      showRejectDialog.value = false
      selectedRecord.value = null
      rejectForm.value = { reason: '', updatedChiefComplaint: '' }
    }

    // 工具函数
    const getTriageLevelClass = (level) => {
      const classes = {
        1: 'level-1', 2: 'level-2', 3: 'level-3', 4: 'level-4', 5: 'level-5'
      }
      return classes[level] || ''
    }

    const getTriageBadgeClass = (level) => {
      const classes = {
        1: 'badge-red', 2: 'badge-orange', 3: 'badge-yellow', 4: 'badge-green', 5: 'badge-blue'
      }
      return classes[level] || ''
    }

    const getTriageLevelText = (level) => {
      // 严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)
      // 四级分诊标准：Ⅰ级-急危、Ⅱ级-急重、Ⅲ级-急症、Ⅳ级-亚急症/非急症
      const texts = {
        1: 'Ⅰ级-急危(红色)',    // 即刻 - 复苏区/抢救区
        2: 'Ⅱ级-急重(橙色)',    // 10分钟内 - 抢救区
        3: 'Ⅲ级-急症(黄色)',    // 30分钟内 - 优先诊疗区
        4: 'Ⅳ级-亚急症(绿色)'   // 60分钟-2小时 - 普通诊疗区
      }
      return texts[level] || '未知等级'
    }

    const formatTime = (timeStr) => {
      return new Date(timeStr).toLocaleString()
    }

    const getVitalSign = (vitalSignsStr, key) => {
      try {
        const vitals = JSON.parse(vitalSignsStr)
        return vitals[key] || '-'
      } catch {
        return '-'
      }
    }

    // 生命周期
    onMounted(async () => {
      await fetchPendingRecords()
      
      // 建立WebSocket连接
      connect()
      
      // 订阅护士确认主题
      subscribe('/topic/nurse-confirmation', (message) => {
        newTriageAlert.value = true
        // 可以直接将新数据添加到列表中
        if (message.type === 'triage_confirmation') {
          fetchPendingRecords()
        }
      })
    })

    onUnmounted(() => {
      disconnect()
    })

    return {
      pendingRecords,
      filteredRecords,
      levelFilter,
      newTriageAlert,
      submitting,
      showConfirmDialog,
      showRejectDialog,
      selectedRecord,
      confirmForm,
      vitalSignsUpdate,
      rejectForm,
      fetchPendingRecords,
      filterRecords,
      refreshData,
      viewDetails,
      confirmTriage,
      rejectTriage,
      submitConfirmation,
      submitRejection,
      closeConfirmDialog,
      closeRejectDialog,
      getTriageLevelClass,
      getTriageBadgeClass,
      getTriageLevelText,
      formatTime,
      getVitalSign
    }
  }
}
</script>

<style scoped>
.triage-confirmation {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #2c3e50;
  margin-bottom: 5px;
}

.alert {
  padding: 15px;
  margin-bottom: 20px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.alert-warning {
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  color: #856404;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.record-card {
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.record-card.level-1 { border-color: #e74c3c; }
.record-card.level-2 { border-color: #f39c12; }
.record-card.level-3 { border-color: #f1c40f; }
.record-card.level-4 { border-color: #27ae60; }
.record-card.level-5 { border-color: #3498db; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.patient-info h4 {
  margin: 0;
  color: #2c3e50;
}

.patient-id {
  color: #7f8c8d;
  font-size: 0.9em;
}

.triage-badge {
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 0.9em;
  font-weight: bold;
}

.badge-red { background: #e74c3c; color: white; }
.badge-orange { background: #f39c12; color: white; }
.badge-yellow { background: #f1c40f; color: black; }
.badge-green { background: #27ae60; color: white; }
.badge-blue { background: #3498db; color: white; }

.card-content {
  padding: 15px;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
}

.info-row label {
  font-weight: bold;
  min-width: 80px;
  color: #2c3e50;
}

.vital-signs {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
}

.signs-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.sign-item {
  display: flex;
  justify-content: space-between;
  padding: 5px;
  background: #f8f9fa;
  border-radius: 5px;
}

.card-actions {
  padding: 15px;
  display: flex;
  gap: 10px;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9em;
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn-info { background: #17a2b8; color: white; }
.btn-success { background: #28a745; color: white; }
.btn-warning { background: #ffc107; color: black; }
.btn-secondary { background: #6c757d; color: white; }

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: white;
  border-radius: 10px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #2c3e50;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
}

.form-group textarea {
  height: 80px;
  resize: vertical;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.patient-update-section,
.vitals-update-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.vitals-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}
</style>