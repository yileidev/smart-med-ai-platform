<template>
  <div class="doctor-dashboard">
    <!-- 顶部导航 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>👨‍⚕️ 医生工作台</h1>
        <p>急诊分诊与诊断系统 - 医生端</p>
      </div>
      <div class="header-right">
        <el-badge :value="pendingCount" class="notification-badge">
          <el-button type="primary" @click="refreshQueue" :loading="loading">
            <i class="el-icon-refresh"></i> 刷新队列
          </el-button>
        </el-badge>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32">👨‍⚕️</el-avatar>
            <span>{{ currentUser }}</span>
            <i class="el-icon-arrow-down"></i>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧：患者队列 -->
      <div class="patient-queue">
        <div class="queue-header">
          <h3>📋 待诊患者队列</h3>
        </div>
        <div class="queue-filters">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索患者姓名"
            clearable
            class="search-input"
          ></el-input>
          <el-select v-model="selectedDepartment" placeholder="选择科室" @change="filterPatients" class="dept-select">
            <el-option label="全部" value=""></el-option>
            <el-option label="心内科" value="心内科"></el-option>
            <el-option label="呼吸科" value="呼吸科"></el-option>
            <el-option label="消化科" value="消化科"></el-option>
            <el-option label="急诊科" value="急诊科"></el-option>
          </el-select>
        </div>

        <div class="patient-list">
          <div 
            v-for="patient in filteredPatients" 
            :key="patient.id"
            :class="['patient-card', { selected: selectedPatient?.id === patient.id }]"
            @click="selectPatient(patient)"
          >
            <div class="patient-header">
              <div class="patient-info">
                <span class="patient-name">{{ patient.patient?.patientName || patient.patientName || '未知' }}</span>
                <span :class="['triage-badge', `level-${patient.triageLevel}`]">
                  {{ getTriageLevelText(patient.triageLevel) }}
                </span>
              </div>
              <div class="patient-time">
                {{ formatTime(patient.arrivalTime) }}
              </div>
            </div>
            
            <div class="patient-details">
              <div class="complaint">
                <strong>主诉：</strong>{{ patient.chiefComplaint || '无' }}
              </div>
              <div class="vital-signs">
                <span class="vital-item">🌡️ {{ getVitalSign(patient.vitalSigns, 'temperature') }}°C</span>
                <span class="vital-item">💓 {{ getVitalSign(patient.vitalSigns, 'heartRate') }}次/分</span>
                <span class="vital-item">🫁 {{ getVitalSign(patient.vitalSigns, 'bloodOxygen') }}%</span>
                <span class="vital-item">🩸 {{ getVitalSign(patient.vitalSigns, 'systolicBP') }}/{{ getVitalSign(patient.vitalSigns, 'diastolicBP') }}</span>
              </div>
              <div class="department">
                <strong>建议科室：</strong>{{ patient.assignedDepartment }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：诊断工作区 -->
      <div class="diagnosis-workspace">
        <div v-if="!selectedPatient" class="no-selection">
          <div class="empty-state">
            <i class="el-icon-document"></i>
            <h3>请选择患者进行诊断</h3>
            <p>从左侧患者队列中选择一个患者开始诊断工作</p>
          </div>
        </div>

        <div v-else class="patient-diagnosis">
          <!-- 上半部分：患者信息 + 生命体征 -->
          <div class="upper-section">
            <!-- 患者基本信息 -->
            <div class="info-section">
              <h4>👤 患者信息</h4>
              <div class="info-grid">
                <div class="info-item">
                  <label>姓名：</label>
                  <span>{{ selectedPatient.patient?.patientName || selectedPatient.patientName || '未知' }}</span>
                </div>
                <div class="info-item">
                  <label>证件号：</label>
                  <span>{{ selectedPatient.patient?.idCard || selectedPatient.patient?.idNumber || '未登记' }}</span>
                </div>
                <div class="info-item">
                  <label>到院时间：</label>
                  <span>{{ formatTime(selectedPatient.arrivalTime) }}</span>
                </div>
                <div class="info-item">
                  <label>分诊等级：</label>
                  <span :class="['triage-badge', `level-${selectedPatient.triageLevel}`]">
                    {{ getTriageLevelText(selectedPatient.triageLevel) }}
                  </span>
                </div>
                <div class="info-item">
                  <label>建议科室：</label>
                  <span>{{ selectedPatient.assignedDepartment }}</span>
                </div>
                <div class="info-item info-item-full">
                  <label>患者主诉：</label>
                  <span class="chief-complaint-text">{{ selectedPatient.chiefComplaint || '无' }}</span>
                </div>
              </div>
            </div>

            <!-- 生命体征详情 -->
            <div class="vitals-section">
              <h4>📊 生命体征详情</h4>
              <div class="vitals-chart">
                <div class="vital-detail">
                  <div class="vital-label">体温</div>
                  <div class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'temperature') }}°C</div>
                  <div class="vital-status" :class="getVitalStatus(selectedPatient.vitalSigns, 'temperature')">{{ getVitalStatusText(selectedPatient.vitalSigns, 'temperature') }}</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">心率</div>
                  <div class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'heartRate') }}</div>
                  <div class="vital-status" :class="getVitalStatus(selectedPatient.vitalSigns, 'heartRate')">{{ getVitalStatusText(selectedPatient.vitalSigns, 'heartRate') }}</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">血压</div>
                  <div class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'systolicBP') }}/{{ getVitalSign(selectedPatient.vitalSigns, 'diastolicBP') }}</div>
                  <div class="vital-status" :class="getVitalStatus(selectedPatient.vitalSigns, 'bloodPressure')">{{ getVitalStatusText(selectedPatient.vitalSigns, 'bloodPressure') }}</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">血氧</div>
                  <div class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'bloodOxygen') }}%</div>
                  <div class="vital-status" :class="getVitalStatus(selectedPatient.vitalSigns, 'bloodOxygen')">{{ getVitalStatusText(selectedPatient.vitalSigns, 'bloodOxygen') }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 下半部分：AI诊断建议 + 医生诊断 -->
          <div class="lower-section">
            <!-- AI分析结果 -->
            <div class="ai-analysis">
              <div class="ai-header">
                <h4>🤖 AI诊断建议</h4>
                <el-button type="primary" size="small" @click="getDetailedAIDiagnosis" :loading="loading">
                  获取AI分析
                </el-button>
              </div>
              
              <!-- 加载进度条 -->
              <div v-if="loading" class="ai-loading">
                <el-progress :percentage="aiLoadingProgress" :stroke-width="6" status="success"></el-progress>
                <p class="loading-text">AI正在分析中，请稍候...</p>
              </div>
              
              <!-- AI诊断内容区域 -->
              <div v-else class="ai-content">
                <div v-if="selectedPatient.aiDiagnosis || aiDetailedResult.symptomAnalysis" class="diagnosis-text-area">
                  <!-- 主诊断 -->
                  <div class="ai-section">
                    <div class="section-title">📊 初步诊断</div>
                    <div class="section-content">{{ selectedPatient.aiDiagnosis || aiDetailedResult.symptomAnalysis || '暂无' }}</div>
                  </div>
                  
                  <!-- 建议检查 -->
                  <div v-if="aiDetailedResult.recommendedExams && aiDetailedResult.recommendedExams.length > 0" class="ai-section">
                    <div class="section-title">🩺 建议检查</div>
                    <ul class="section-list">
                      <li v-for="exam in aiDetailedResult.recommendedExams" :key="exam">{{ exam }}</li>
                    </ul>
                  </div>
                  
                  <!-- 可能诊断 -->
                  <div v-if="aiDetailedResult.possibleDiagnoses && aiDetailedResult.possibleDiagnoses.length > 0" class="ai-section">
                    <div class="section-title">📋 可能诊断</div>
                    <ul class="section-list">
                      <li v-for="diagnosis in aiDetailedResult.possibleDiagnoses" :key="diagnosis">{{ diagnosis }}</li>
                    </ul>
                  </div>
                </div>
                <div v-else class="no-diagnosis-hint">
                  <span>💡 点击上方按钮获取AI智能分析</span>
                </div>
              </div>
              
              <!-- 底部操作栏 -->
              <div class="ai-footer">
                <div class="confidence-bar">
                  <span class="label">置信度</span>
                  <el-progress 
                    :percentage="Math.round((selectedPatient.triageScore || 0.8) * 100)" 
                    :color="confidenceColor"
                    :stroke-width="8"
                  ></el-progress>
                </div>
                <el-button 
                  v-if="selectedPatient.aiDiagnosis" 
                  type="success" 
                  size="small" 
                  @click="adoptAISuggestion"
                >
                  采纳AI建议
                </el-button>
              </div>
            </div>

            <!-- 医生诊断 -->
            <div class="doctor-diagnosis">
              <h4>✍️ 医生诊断</h4>
              <el-form :model="diagnosisForm" ref="diagnosisFormRef" label-width="80px">
                <el-form-item label="诊断结果" prop="diagnosis">
                  <el-input
                    v-model="diagnosisForm.diagnosis"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入详细诊断结果..."
                  ></el-input>
                </el-form-item>
                
                <el-form-item label="治疗方案" prop="treatment">
                  <el-input
                    v-model="diagnosisForm.treatment"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入治疗方案和建议..."
                  ></el-input>
                </el-form-item>

                <el-form-item label="优先级">
                  <el-radio-group v-model="diagnosisForm.priority" size="small">
                    <el-radio label="1" border><span class="level-1">急危重症</span></el-radio>
                    <el-radio label="2" border><span class="level-2">急症</span></el-radio>
                    <el-radio label="3" border><span class="level-3">次急症</span></el-radio>
                    <el-radio label="4" border><span class="level-4">非急症</span></el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="submitDiagnosis" :loading="submitting" size="small">
                    提交诊断
                  </el-button>
                  <el-button @click="saveDraft" size="small">保存草稿</el-button>
                  <el-button type="info" @click="requestConsultation" size="small">申请会诊</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI详细分析对话框 -->
    <el-dialog v-model="aiDialogVisible" title="🤖 AI详细诊断分析" width="60%">
      <div class="ai-detailed-analysis">
        <div class="analysis-section">
          <h4>症状分析</h4>
          <p>{{ aiDetailedResult.symptomAnalysis }}</p>
        </div>
        <div class="analysis-section">
          <h4>可能诊断</h4>
          <ul>
            <li v-for="diagnosis in aiDetailedResult.possibleDiagnoses" :key="diagnosis">
              {{ diagnosis }}
            </li>
          </ul>
        </div>
        <div class="analysis-section">
          <h4>建议检查</h4>
          <ul>
            <li v-for="exam in aiDetailedResult.recommendedExams" :key="exam">
              {{ exam }}
            </li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="aiDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="adoptAISuggestion">采纳AI建议</el-button>
      </template>
    </el-dialog>

    <!-- 个人信息对话框 -->
    <el-dialog v-model="profileDialogVisible" title="👨‍⚕️ 个人信息" width="600px">
      <el-form :model="profileForm" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.fullName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="profileForm.department" placeholder="请选择科室" style="width: 100%">
            <el-option label="心内科" value="心内科"></el-option>
            <el-option label="呼吸科" value="呼吸科"></el-option>
            <el-option label="消化科" value="消化科"></el-option>
            <el-option label="急诊科" value="急诊科"></el-option>
            <el-option label="神经内科" value="神经内科"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="profileForm.title" placeholder="请选择职称" style="width: 100%">
            <el-option label="住院医师" value="住院医师"></el-option>
            <el-option label="主治医师" value="主治医师"></el-option>
            <el-option label="副主任医师" value="副主任医师"></el-option>
            <el-option label="主任医师" value="主任医师"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="执业证号">
          <el-input v-model="profileForm.licenseNumber" placeholder="请输入执业证号"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="changePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置对话框 -->
    <el-dialog v-model="settingsDialogVisible" title="⚙️ 系统设置" width="600px">
      <el-form :model="settingsForm" label-width="120px">
        <el-divider content-position="left">通知设置</el-divider>
        <el-form-item label="消息通知">
          <el-switch v-model="settingsForm.notifications"></el-switch>
          <span class="setting-hint">接收系统通知消息</span>
        </el-form-item>
        <el-form-item label="声音提醒">
          <el-switch v-model="settingsForm.soundAlerts"></el-switch>
          <span class="setting-hint">紧急患者声音提醒</span>
        </el-form-item>
        
        <el-divider content-position="left">界面设置</el-divider>
        <el-form-item label="自动刷新">
          <el-switch v-model="settingsForm.autoRefresh"></el-switch>
          <span class="setting-hint">自动刷新患者队列</span>
        </el-form-item>
        <el-form-item label="刷新间隔" v-if="settingsForm.autoRefresh">
          <el-slider v-model="settingsForm.refreshInterval" :min="10" :max="120" :step="10" show-stops></el-slider>
          <span class="setting-hint">{{ settingsForm.refreshInterval }}秒</span>
        </el-form-item>
        <el-form-item label="主题">
          <el-radio-group v-model="settingsForm.theme">
            <el-radio label="light">浅色</el-radio>
            <el-radio label="dark">深色</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="settingsForm.language" placeholder="请选择语言" style="width: 200px">
            <el-option label="简体中文" value="zh-CN"></el-option>
            <el-option label="English" value="en-US"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'
import { getAIDiagnosis, submitDiagnosis as submitDiagnosisAPI, getPatientQueue, getDoctorStats } from '@/api/doctor'

const router = useRouter()
const userStore = useUserStore()
const currentUser = ref('张医生')

// WebSocket连接
const { connect, disconnect, subscribe, connected } = useWebSocket()

// 数据状态
const selectedDepartment = ref('')
const searchKeyword = ref('')
const selectedPatient = ref(null)
const submitting = ref(false)
const loading = ref(false)
const aiLoadingProgress = ref(0)  // AI加载进度
const aiDialogVisible = ref(false)
const aiExpanded = ref(false)
const profileDialogVisible = ref(false)
const settingsDialogVisible = ref(false)

// 个人信息表单
const profileForm = reactive({
  fullName: userStore.userInfo?.fullName || '',
  username: userStore.userInfo?.username || '',
  email: userStore.userInfo?.email || '',
  phone: '',
  department: '心内科',
  title: '主治医师',
  licenseNumber: ''
})

// 系统设置表单
const settingsForm = reactive({
  notifications: true,
  soundAlerts: true,
  autoRefresh: true,
  refreshInterval: 30,
  theme: 'light',
  language: 'zh-CN'
})

// 统计数据 - 从数据库加载真实数据
const stats = reactive({
  urgentPatients: 0,
  pendingPatients: 0,
  completedToday: 0,
  avgDiagnosisTime: '0分钟'
})

// 患者队列数据 - 从数据库加载真实数据
const patients = ref([])

// 诊断表单
const diagnosisForm = reactive({
  diagnosis: '',
  treatment: '',
  priority: '',
  department: ''
})

// AI详细分析结果
const aiDetailedResult = reactive({
  symptomAnalysis: '',
  possibleDiagnoses: [],
  recommendedExams: []
})

// 计算属性
const pendingCount = computed(() => patients.value.length)
const filteredPatients = computed(() => {
  let result = patients.value
  
  // 按科室过滤
  if (selectedDepartment.value) {
    result = result.filter(p => p.assignedDepartment === selectedDepartment.value)
  }
  
  // 按关键词搜索
  if (searchKeyword.value && searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    result = result.filter(p => {
      const patientName = (p.patient?.patientName || p.patientName || '').toLowerCase()
      const chiefComplaint = (p.chiefComplaint || '').toLowerCase()
      const department = (p.assignedDepartment || '').toLowerCase()
      
      return patientName.includes(keyword) || 
             chiefComplaint.includes(keyword) ||
             department.includes(keyword)
    })
  }
  
  return result
})

const confidenceColor = computed(() => {
  const score = selectedPatient.value?.triageScore || 0
  if (score > 0.8) return '#67c23a'
  if (score > 0.6) return '#e6a23c'
  return '#f56c6c'
})

// 方法
const refreshQueue = async () => {
  try {
    loading.value = true
    ElMessage.info('正在刷新队列...')
    await loadInitialData()
    ElMessage.success(`队列已刷新，当前有 ${patients.value.length} 位患者`)
  } catch (error) {
    console.error('刷新失败:', error)
    ElMessage.error('刷新失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const selectPatient = (patient) => {
  selectedPatient.value = patient
  aiExpanded.value = false  // 重置展开状态
  // 重置表单
  diagnosisForm.diagnosis = ''
  diagnosisForm.treatment = ''
  diagnosisForm.priority = patient.triageLevel.toString()
}

const toggleAIExpand = () => {
  aiExpanded.value = !aiExpanded.value
}

const filterPatients = () => {
  // 过滤逻辑已在computed中处理
}

const getTriageLevelText = (level) => {
  // 严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)
  // 四级分诊标准：Ⅰ级-急危、Ⅱ级-急重、Ⅲ级-急症、Ⅳ级-亚急症/非急症
  const levels = {
    1: 'Ⅰ级-急危',    // 红色 - 即刻
    2: 'Ⅱ级-急重',    // 橙色 - 10分钟内
    3: 'Ⅲ级-急症',    // 黄色 - 30分钟内
    4: 'Ⅳ级-亚急症'   // 绿色 - 60分钟-2小时
  }
  return levels[level] || '未分诊'
}

const getVitalSign = (vitalSigns, key) => {
  try {
    if (!vitalSigns) return '--'
    
    let signData = vitalSigns
    
    // 持续解析直到得到对象
    while (typeof signData === 'string') {
      try {
        const parsed = JSON.parse(signData)
        signData = parsed
      } catch (e) {
        break
      }
    }
    
    // 确保最终是对象
    if (typeof signData !== 'object') {
      return '--'
    }
    
    return signData[key] || '--'
  } catch (e) {
    return '--'
  }
}

const formatTime = (time) => {
  if (!time) return '--'
  try {
    const date = new Date(time)
    if (isNaN(date.getTime())) return '--'
    return date.toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  } catch (e) {
    return '--'
  }
}

// 获取生命体征状态样式类 - 根据国家卫健委专家共识客观评估指标
const getVitalStatus = (vitalSigns, key) => {
  try {
    const value = parseFloat(getVitalSign(vitalSigns, key))
    if (isNaN(value)) return 'status-normal'
    
    // 严格遵循《急诊预检分诊专家共识》客观评估指标
    switch(key) {
      case 'temperature': // 体温(腋温) - 专家共识表1
        if (value > 41.0) return 'status-critical'  // Ⅰ级: >41℃
        if (value > 39.0 && value <= 41.0) return 'status-high'  // Ⅱ级: 39-41℃
        if (value >= 38.5 && value <= 39.0) return 'status-warning'  // Ⅲ级: 38.5-39℃
        if (value < 35.0) return 'status-critical'  // Ⅰ级: <35℃
        return 'status-normal'  // Ⅳ级: 35-38.5℃
        
      case 'heartRate': // 心率(次/min) - 专家共识表1
        if (value > 180 || value < 40) return 'status-critical'  // Ⅰ级: >180或<40
        if ((value >= 150 && value <= 180) || (value >= 40 && value <= 50)) return 'status-high'  // Ⅱ级
        if ((value >= 100 && value < 150) || (value > 50 && value <= 55)) return 'status-warning'  // Ⅲ级
        return 'status-normal'  // Ⅳ级: 55-100
        
      case 'bloodPressure': // 收缩压(mmHg) - 专家共识表1
        const systolic = parseFloat(getVitalSign(vitalSigns, 'systolicBP'))
        if (isNaN(systolic)) return 'status-normal'
        if (systolic < 70) return 'status-critical'  // Ⅰ级: <70mmHg
        if (systolic > 200) return 'status-critical'  // Ⅰ级: >200mmHg
        if ((systolic >= 70 && systolic <= 80) || systolic > 200) return 'status-high'  // Ⅱ级
        if ((systolic > 80 && systolic <= 90) || (systolic >= 180 && systolic <= 200)) return 'status-warning'  // Ⅲ级
        return 'status-normal'  // Ⅳ级: 90-180mmHg
        
      case 'bloodOxygen': // SpO2(%) - 专家共识表1
        if (value < 80) return 'status-critical'  // Ⅰ级: <80%
        if (value >= 80 && value < 90) return 'status-high'  // Ⅱ级: 80-90%
        if (value >= 90 && value < 94) return 'status-warning'  // Ⅲ级: 90-94%
        return 'status-normal'  // Ⅳ级: ≥94%
        
      default:
        return 'status-normal'
    }
  } catch (e) {
    return 'status-normal'
  }
}

// 获取生命体征状态文字 - 根据国家卫健委专家共识客观评估指标
const getVitalStatusText = (vitalSigns, key) => {
  try {
    const value = parseFloat(getVitalSign(vitalSigns, key))
    if (isNaN(value)) return '正常'
    
    // 严格遵循《急诊预检分诊专家共识》客观评估指标
    switch(key) {
      case 'temperature':  // 腋温
        if (value > 41.0) return 'Ⅰ级-极高热(>41℃)'  // 急危
        if (value > 39.0 && value <= 41.0) return 'Ⅱ级-高热(39-41℃)'  // 急重
        if (value >= 38.5 && value <= 39.0) return 'Ⅲ级-中等发热(38.5-39℃)'  // 急症
        if (value < 35.0) return 'Ⅰ级-低体温(<35℃)'  // 急危
        return '正常(35-38.5℃)'
        
      case 'heartRate':  // 心率(次/min)
        if (value > 180) return 'Ⅰ级-极度心动过速(>180)'  // 急危
        if (value < 40) return 'Ⅰ级-极度心动过缓(<40)'  // 急危
        if (value >= 150 && value <= 180) return 'Ⅱ级-重度心动过速(150-180)'  // 急重
        if (value >= 40 && value <= 50) return 'Ⅱ级-重度心动过缓(40-50)'  // 急重
        if (value >= 100 && value < 150) return 'Ⅲ级-轻度心动过速(100-150)'  // 急症
        if (value > 55 && value < 60) return 'Ⅲ级-轻度心动过缓(55-60)'  // 急症
        return '正常(60-100)'
        
      case 'bloodPressure':  // 收缩压(mmHg)
        const systolic = parseFloat(getVitalSign(vitalSigns, 'systolicBP'))
        if (isNaN(systolic)) return '正常'
        if (systolic < 70) return 'Ⅰ级-休克血压(<70)'  // 急危
        if (systolic > 200) return 'Ⅰ级-高血压危象(>200)'  // 急危
        if (systolic >= 70 && systolic <= 80) return 'Ⅱ级-低血压(70-80)'  // 急重
        if (systolic >= 180 && systolic <= 200) return 'Ⅲ级-高血压(180-200)'  // 急症
        if (systolic > 80 && systolic <= 90) return 'Ⅲ级-偏低(80-90)'  // 急症
        return '正常(90-180)'
        
      case 'bloodOxygen':  // SpO2(%)
        if (value < 80) return 'Ⅰ级-严重缺氧(<80%)'  // 急危
        if (value >= 80 && value < 90) return 'Ⅱ级-中度缺氧(80-90%)'  // 急重
        if (value >= 90 && value < 94) return 'Ⅲ级-轻度缺氧(90-94%)'  // 急症
        return '正常(≥94%)'
        
      default:
        return '正常'
    }
  } catch (e) {
    return '正常'
  }
}

const getDetailedAIDiagnosis = async () => {
  if (!selectedPatient.value) return
  
  try {
    loading.value = true
    aiLoadingProgress.value = 0
    
    // 模拟进度条动画
    const progressInterval = setInterval(() => {
      if (aiLoadingProgress.value < 90) {
        aiLoadingProgress.value += Math.random() * 15
      }
    }, 300)
    
    // 调用真实API获取AI详细诊断
    const response = await getAIDiagnosis(selectedPatient.value.id)
    
    clearInterval(progressInterval)
    aiLoadingProgress.value = 100
    
    console.log('AI诊断响应:', response)
    
    // axios拦截器已经解包，直接使用response
    if (response.success) {
      const aiData = response.data || {}
      aiDetailedResult.symptomAnalysis = aiData.symptomAnalysis || ''
      aiDetailedResult.possibleDiagnoses = aiData.possibleDiagnoses || []
      aiDetailedResult.recommendedExams = aiData.recommendedExams || []
      
      // 直接在当前区域显示AI诊断结果
      if (aiData.primaryDiagnosis) {
        selectedPatient.value.aiDiagnosis = aiData.primaryDiagnosis
      } else if (aiData.symptomAnalysis) {
        selectedPatient.value.aiDiagnosis = aiData.symptomAnalysis
      }
      // 更新置信度
      if (aiData.confidence) {
        selectedPatient.value.triageScore = aiData.confidence
      }
      
      ElMessage.success('AI分析完成')
    } else {
      ElMessage.error(response.message || '获取AI分析失败')
    }
  } catch (error) {
    console.error('AI诊断失败:', error)
    ElMessage.error('获取AI分析失败，请稍后重试')
  } finally {
    setTimeout(() => {
      loading.value = false
      aiLoadingProgress.value = 0
    }, 300)
  }
}

const adoptAISuggestion = () => {
  // 使用当前显示的AI诊断结果
  if (selectedPatient.value?.aiDiagnosis || aiDetailedResult.symptomAnalysis) {
    // 拼接诊断结果
    let diagnosis = selectedPatient.value?.aiDiagnosis || aiDetailedResult.symptomAnalysis || ''
    if (aiDetailedResult.possibleDiagnoses && aiDetailedResult.possibleDiagnoses.length > 0) {
      diagnosis += '\n\n可能诊断：\n' + aiDetailedResult.possibleDiagnoses.join('\n')
    }
    diagnosisForm.diagnosis = diagnosis
    
    // 拼接治疗方案
    let treatment = '根据AI分析建议：'
    if (aiDetailedResult.recommendedExams && aiDetailedResult.recommendedExams.length > 0) {
      treatment += '\n建议检查：' + aiDetailedResult.recommendedExams.join('、')
    }
    diagnosisForm.treatment = treatment
    
    ElMessage.success('已采纳AI建议')
  } else {
    ElMessage.warning('请先获取AI分析结果')
  }
}

const submitDiagnosis = async () => {
  if (!diagnosisForm.diagnosis.trim()) {
    ElMessage.warning('请输入诊断结果')
    return
  }
  
  if (!selectedPatient.value) {
    ElMessage.warning('请选择患者')
    return
  }
  
  try {
    submitting.value = true
    
    // 添加调试日志
    console.log('准备提交诊断, selectedPatient:', selectedPatient.value)
    console.log('患者ID:', selectedPatient.value.patient?.id || selectedPatient.value.patientId)
    console.log('分诊记录ID:', selectedPatient.value.id)
    
    // 调用真实API提交诊断
    const response = await submitDiagnosisAPI({
      patientId: selectedPatient.value.patient?.id || selectedPatient.value.patientId,
      triageRecordId: selectedPatient.value.id,
      diagnosis: diagnosisForm.diagnosis,
      treatment: diagnosisForm.treatment,
      priority: diagnosisForm.priority
    })
    
    console.log('API完整响应:', response)
    console.log('response.data:', response.data)
    
    // 处理多种响应格式
    const isSuccess = 
      response.data === '诊断提交成功' || 
      response.data?.success === true ||
      response.success === true ||
      (typeof response.data === 'object' && response.data?.message?.includes('成功'))
    
    if (isSuccess) {
      ElMessage.success('诊断提交成功')
      
      // 移除已诊断的患者
      const index = patients.value.findIndex(p => p.id === selectedPatient.value.id)
      if (index > -1) {
        patients.value.splice(index, 1)
      }
      
      selectedPatient.value = null
      stats.completedToday++
      stats.pendingPatients = patients.value.length
      stats.urgentPatients = patients.value.filter(p => p.triageLevel <= 2).length
      
      // 重置表单
      diagnosisForm.diagnosis = ''
      diagnosisForm.treatment = ''
      diagnosisForm.priority = ''
    } else {
      const errorMsg = response.data?.message || response.message || '提交失败'
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    console.error('提交诊断失败:', error)
    console.error('错误详情:', error.response || error.message)
    ElMessage.error('提交失败: ' + (error.response?.data?.message || error.message || '请检查网络连接'))
  } finally {
    submitting.value = false
  }
}

const saveDraft = () => {
  ElMessage.success('草稿已保存')
}

const requestConsultation = () => {
  ElMessage.success('会诊申请已发送')
}

// 保存个人信息
const saveProfile = () => {
  ElMessage.success('个人信息已保存')
  profileDialogVisible.value = false
}

// 保存设置
const saveSettings = () => {
  // 保存到 localStorage
  localStorage.setItem('userSettings', JSON.stringify(settingsForm))
  ElMessage.success('设置已保存')
  settingsDialogVisible.value = false
}

// 修改密码
const changePassword = () => {
  ElMessageBox.prompt('请输入新密码', '修改密码', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /.{6,}/,
    inputErrorMessage: '密码至少需要6位'
  }).then(({ value }) => {
    ElMessage.success('密码修改成功，请重新登录')
  }).catch(() => {
    // 用户取消
  })
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      profileDialogVisible.value = true
      break
    case 'settings':
      settingsDialogVisible.value = true
      break
    case 'logout':
      // 退出登录：清除用户信息
      userStore.logout()
      ElMessage.success('退出登录成功')
      router.push('/login')
      break
  }
}

onMounted(async () => {
  console.log('医生工作台组件已挂载,准备初始化...')
  
  // 初始化WebSocket连接
  connect()
  
  // 使用轮询等待WebSocket连接成功后再订阅
  const waitForConnection = () => {
    return new Promise((resolve) => {
      const checkConnection = () => {
        if (connected.value) {
          resolve(true)
        } else {
          setTimeout(checkConnection, 200)
        }
      }
      // 最多等待10秒
      setTimeout(() => resolve(false), 10000)
      checkConnection()
    })
  }
  
  waitForConnection().then((isConnected) => {
    console.log('WebSocket连接状态:', isConnected)
    
    if (!isConnected) {
      console.warn('WebSocket连接超时，尝试重新连接...')
      connect()
    }
    
    // 订阅医生端消息 - 护士复核后的新患者
    subscribe('/topic/new-patients', (message) => {
      console.log('🔔 [医生端] 收到 /topic/new-patients 消息:', message)
      console.log('消息类型:', message.type)
      console.log('完整消息内容:', JSON.stringify(message, null, 2))
      
      if (message.type === 'NEW_DIAGNOSIS') {
        console.log('✅ 确认是 NEW_DIAGNOSIS 类型消息,准备添加患者到队列')
        
        // 护士复核后推送的新患者 - 使用完整数据
        const newPatient = {
          id: message.triageRecordId || message.patientId,
          patient: {
            id: message.patientId,
            patientName: message.patientName || '待登记'
          },
          patientId: message.patientId,
          patientName: message.patientName || '待登记',
          triageLevel: message.triageLevel,
          chiefComplaint: message.chiefComplaint || '',
          vitalSigns: message.vitalSigns || '{}',
          aiDiagnosis: message.aiDiagnosis || '',
          nurseNotes: message.nurseNotes || '',
          assignedDepartment: message.assignedDepartment || '',
          arrivalTime: message.arrivalTime || new Date(),
          status: 'CONFIRMED'
        }
        
        console.log('构建的新患者对象:', newPatient)
        console.log('新患者vitalSigns字段:', newPatient.vitalSigns)
        console.log('新患者vitalSigns类型:', typeof newPatient.vitalSigns)
        
        // 检查是否已存在
        const exists = patients.value.some(p => p.id === newPatient.id)
        if (exists) {
          console.log('⚠️ 患者已存在于队列中,跳过添加')
          return
        }
        
        patients.value.unshift(newPatient)
        stats.pendingPatients = patients.value.length
        stats.urgentPatients = patients.value.filter(p => p.triageLevel <= 2).length
        
        console.log('✅ 患者已添加到队列,当前队列长度:', patients.value.length)
        
        ElMessage({
          message: `新患者：${newPatient.patientName} - ${getTriageLevelText(newPatient.triageLevel)}`,
          type: newPatient.triageLevel === 1 ? 'error' : 'warning',
          duration: 5000
        })
      } else {
        console.log('⚠️ 消息类型不匹配,当前类型:', message.type)
      }
    })
    
    // 订阅医生诊断主题 - 分诊确认通知
    subscribe('/topic/doctor-diagnosis', (message) => {
      console.log('🔔 [医生端] 收到 /topic/doctor-diagnosis 消息:', message)
      console.log('消息类型:', message.type)
      
      if (message.type === 'NEW_DIAGNOSIS') {
        console.log('✅ doctor-diagnosis 主题收到 NEW_DIAGNOSIS,刷新队列')
        // 如果已经通过 /topic/new-patients 处理了,这里就不重复处理
        // 可以选择刷新整个队列以确保数据一致性
        setTimeout(() => {
          loadInitialData()
          console.log('队列已刷新')
        }, 1000)
      }
      
      if (message.type === 'triage_confirmation' || message.type === 'new_diagnosis') {
        // 刷新患者队列
        loadInitialData()
        ElMessage.info('有新的分诊确认，队列已更新')
      }
    })
  }) // waitForConnection.then 结束
  
  // 加载初始数据
  await loadInitialData()
  
  console.log('医生工作台初始化完成')
})

const loadInitialData = async () => {
  try {
    console.log('开始加载医生工作台数据...')
    
    // 获取统计数据
    const statsRes = await getDoctorStats()
    console.log('统计数据API响应:', statsRes)
    if (statsRes.data && statsRes.data.success) {
      Object.assign(stats, statsRes.data.data)
      console.log('统计数据加载成功:', statsRes.data.data)
    }
    
    // 获取患者队列 - 从数据库加载真实数据
    const queueRes = await getPatientQueue()
    console.log('患者队列API完整响应:', queueRes)
    console.log('queueRes.data:', queueRes.data)
    console.log('queueRes.data 类型:', typeof queueRes.data)
    console.log('queueRes.data 是否为数组:', Array.isArray(queueRes.data))
    
    // 判断响应数据结构
    if (queueRes.data) {
      // 如果 queueRes.data 是对象且有 success 属性
      if (typeof queueRes.data === 'object' && 'success' in queueRes.data && queueRes.data.success) {
        patients.value = queueRes.data.data || []
        console.log('✅ 患者队列加载成功 (对象格式)，共 ' + patients.value.length + ' 个患者')
      } 
      // 如果 queueRes.data 直接是数组
      else if (Array.isArray(queueRes.data)) {
        patients.value = queueRes.data
        console.log('✅ 患者队列加载成功 (数组格式)，共 ' + patients.value.length + ' 个患者')
      }
      else {
        console.warn('⚠️ 未知的响应格式')
        patients.value = []
      }
      
      console.log('患者列表详情:', patients.value)
      
      // 打印第一个患者的完整数据
      if (patients.value.length > 0) {
        console.log('第一个患者完整数据:', JSON.stringify(patients.value[0], null, 2))
        console.log('患者姓名:', patients.value[0].patientName || patients.value[0].patient?.patientName)
        console.log('到院时间:', patients.value[0].arrivalTime)
        console.log('生命体征:', patients.value[0].vitalSigns)
      }
      
      // 根据患者队列实时计算统计数据
      stats.pendingPatients = patients.value.length
      stats.urgentPatients = patients.value.filter(p => p.triageLevel <= 2).length
    } else {
      console.warn('⚠️ API返回数据为空')
      patients.value = []
    }
  } catch (error) {
    console.error('加载初始数据失败:', error)
    console.error('错误详情:', error.response)
    // 即使失败也使用空数组，不使用假数据
    patients.value = []
    ElMessage.warning('加载患者数据失败，请稍后刷新')
  }
}
</script>

<style lang="scss" scoped>
.doctor-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .dashboard-header {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 24px 32px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 4px 20px rgba(0,0,0,0.08);
    border-bottom: 2px solid rgba(102, 126, 234, 0.1);
    
    .header-left {
      h1 {
        margin: 0 0 8px 0;
        color: #2c3e50;
        font-size: 28px;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
      
      p {
        margin: 0;
        color: #7f8c8d;
        font-size: 14px;
        font-weight: 500;
      }
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 24px;
      
      .notification-badge {
        .el-button {
          border-radius: 24px;
          padding: 10px 24px;
          font-weight: 600;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
          transition: all 0.3s ease;
          
          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
          }
        }
      }
      
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        padding: 8px 12px;
        border-radius: 20px;
        transition: background 0.3s;
        
        &:hover {
          background: #f0f0f0;
        }
      }
    }
  }
  
  .main-content {
    display: grid;
    grid-template-columns: 380px 1fr;
    gap: 16px;
    padding: 16px;
    height: calc(100vh - 90px);
    overflow: hidden;
    
    .patient-queue {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-radius: 16px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 8px 32px rgba(0,0,0,0.1);
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 40px rgba(0,0,0,0.15);
      }
      
      .queue-header {
        padding: 20px 24px;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
        border-bottom: 2px solid rgba(102, 126, 234, 0.1);
        
        h3 {
          margin: 0;
          color: #2c3e50;
          font-size: 18px;
          font-weight: 700;
        }
      }
      
      .queue-filters {
        padding: 20px;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
        backdrop-filter: blur(20px);
        border-bottom: 1px solid rgba(102, 126, 234, 0.15);
        display: flex;
        gap: 16px;
        align-items: center;
        
        .search-input {
          flex: 1;
          
          :deep(.el-input__wrapper) {
            border-radius: 24px;
            background: white;
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.12);
            transition: all 0.3s ease;
            padding-left: 15px !important;
            padding-right: 15px !important;
            padding-top: 0 !important;
            padding-bottom: 0 !important;
            border: 2px solid transparent;
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            
            &:hover {
              box-shadow: 0 6px 20px rgba(102, 126, 234, 0.2);
              border-color: rgba(102, 126, 234, 0.1);
              transform: translateY(-1px);
            }
            
            &.is-focus {
              box-shadow: 0 6px 24px rgba(102, 126, 234, 0.25);
              border-color: rgba(102, 126, 234, 0.3);
              transform: translateY(-2px);
            }
          }
          
          :deep(.el-input__inner) {
            border: none !important;
            background: transparent !important;
            height: 100% !important;
            line-height: 40px !important;
            font-size: 14px !important;
            color: #2c3e50 !important;
            font-weight: 500 !important;
            padding: 0 !important;
            margin: 0 !important;
            text-align: left !important;
            
            &::placeholder {
              color: #95a5a6;
              font-weight: 400;
            }
          }
          
          :deep(.el-input__prefix) {
            color: #667eea;
            display: flex;
            align-items: center;
            margin-right: 6px !important;
            margin-left: 0 !important;
            flex-shrink: 0;
          }
          
          :deep(.el-input__suffix) {
            color: #667eea;
            display: flex;
            align-items: center;
            margin-left: 8px;
            flex-shrink: 0;
          }
        }
        
        .dept-select {
          width: 120px;
          
          :deep(.el-input__wrapper) {
            border-radius: 24px;
            background: white;
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.12);
            transition: all 0.3s ease;
            padding: 0 20px;
            border: 2px solid transparent;
            height: 40px;
            display: flex;
            align-items: center;
            
            &:hover {
              box-shadow: 0 6px 20px rgba(102, 126, 234, 0.2);
              border-color: rgba(102, 126, 234, 0.1);
              transform: translateY(-1px);
            }
            
            &.is-focus {
              box-shadow: 0 6px 24px rgba(102, 126, 234, 0.25);
              border-color: rgba(102, 126, 234, 0.3);
              transform: translateY(-2px);
            }
          }
          
          :deep(.el-input__inner) {
            border: none;
            background: transparent;
            height: 100%;
            line-height: 40px;
            font-size: 14px;
            color: #2c3e50;
            font-weight: 500;
            
            &::placeholder {
              color: #95a5a6;
              font-weight: 400;
            }
          }
          
          :deep(.el-input__suffix) {
            color: #667eea;
            display: flex;
            align-items: center;
          }
        }
      }
      
      .patient-list {
        flex: 1;
        overflow-y: auto;
        overflow-x: hidden;
        
        .patient-card {
          padding: 16px;
          margin-bottom: 12px;
          border-radius: 12px;
          cursor: pointer;
          transition: all 0.3s ease;
          background: rgba(255, 255, 255, 0.6);
          border: 2px solid transparent;
          
          &:hover {
            background: rgba(255, 255, 255, 0.9);
            transform: translateX(4px);
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
          }
          
          &.selected {
            background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 126, 234, 0.1) 100%);
            border-color: rgba(64, 158, 255, 0.3);
            box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
            transform: translateX(8px);
          }
          
          .patient-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            
            .patient-info {
              display: flex;
              align-items: center;
              gap: 10px;
              
              .patient-name {
                font-weight: bold;
                color: #333;
              }
              
              .triage-badge {
                padding: 4px 12px;
                border-radius: 14px;
                font-size: 12px;
                color: white;
                font-weight: 600;
                box-shadow: 0 2px 8px rgba(0,0,0,0.15);
                
                &.level-1 { 
                  background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%);
                }
                &.level-2 { 
                  background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%);
                }
                &.level-3 { 
                  background: linear-gradient(135deg, #ffeb3b 0%, #f9a825 100%);
                  color: #333;
                }
                &.level-4 { 
                  background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
                }
              }
            }
            
            .patient-time {
              font-size: 12px;
              color: #999;
            }
          }
          
          .patient-details {
            .complaint {
              font-size: 13px;
              color: #666;
              margin-bottom: 8px;
              white-space: nowrap; // 禁止换行
            }
            
            .vital-signs {
              display: flex;
              gap: 15px;
              margin-bottom: 8px;
              flex-wrap: nowrap; // 禁止换行
              overflow-x: auto; // 如果内容过多，水平滚动
              
              &::-webkit-scrollbar {
                height: 4px;
              }
              
              &::-webkit-scrollbar-thumb {
                background: rgba(102, 126, 234, 0.3);
                border-radius: 2px;
              }
              
              .vital-item {
                font-size: 12px;
                color: #666;
                white-space: nowrap; // 禁止文字换行
                flex-shrink: 0; // 不压缩
              }
            }
            
            .department {
              font-size: 12px;
              color: #409eff;
              white-space: nowrap; // 禁止换行
            }
          }
        }
      }
    }
    
    .diagnosis-workspace {
      background: white;
      border-radius: 16px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      box-shadow: 0 8px 24px rgba(0,0,0,0.08);
      border: 1px solid #eef2f7;
      overflow: hidden;
      height: 100%;
      
      .no-selection {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .empty-state {
          text-align: center;
          color: #999;
          
          i {
            font-size: 64px;
            margin-bottom: 20px;
          }
          
          h3 {
            margin: 0 0 10px 0;
          }
        }
      }
      
      .patient-diagnosis {
        display: flex;
        flex-direction: column;
        height: 100%;
        overflow: hidden;
        gap: 10px;
        
        /* 上半部分：患者信息 + 生命体征 */
        .upper-section {
          display: grid;
          grid-template-columns: 1fr 1fr; /* 等宽 */
          gap: 10px;
          flex: 0 0 auto; /* 不拉伸不压缩 */
        }
        
        /* 下半部分：AI分析 + 医生诊断 */
        .lower-section {
          display: grid;
          grid-template-columns: 1fr 1fr; /* 等宽，与上半部分一致 */
          gap: 10px;
          flex: 1; /* 填充剩余空间 */
          min-height: 0; /* 允许压缩 */
        }
        
        /* 四个section统一样式 */
        .info-section, .vitals-section, .ai-analysis, .doctor-diagnosis {
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
          padding: 12px;
          border-radius: 10px;
          border: 1px solid rgba(102, 126, 234, 0.1);
          transition: all 0.3s ease;
          
          &:hover {
            border-color: rgba(102, 126, 234, 0.2);
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
          }
          
          h4 {
            margin: 0 0 8px 0;
            color: #2c3e50;
            font-size: 14px;
            font-weight: 700;
            padding-bottom: 6px;
            border-bottom: 1px solid rgba(102, 126, 234, 0.2);
          }
        }
          
        .info-section,
        .vitals-section {
          .info-grid {
            display: flex;
            flex-direction: column;
            gap: 4px;
            
            .info-item {
              display: flex;
              font-size: 12px;
              
              &.info-item-full {
                flex-direction: column;
                gap: 4px;
              }
              
              label {
                min-width: 70px;
                color: #666;
                font-weight: 600;
              }
              
              span {
                color: #333;
              }
              
              .chief-complaint-text {
                color: #666;
                line-height: 1.5;
              }
              
              .triage-badge {
                padding: 2px 8px;
                border-radius: 10px;
                font-size: 11px;
                color: white;
                font-weight: 600;
                
                &.level-1 { background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%); }
                &.level-2 { background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%); }
                &.level-3 { background: linear-gradient(135deg, #ffeb3b 0%, #f9a825 100%); color: #333; }
                &.level-4 { background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%); }
              }
            }
          }
          
          .vitals-chart {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 8px;
            
            .vital-detail {
              text-align: center;
              padding: 10px;
              background: rgba(255, 255, 255, 0.8);
              border-radius: 8px;
              border: 1px solid rgba(102, 126, 234, 0.1);
              transition: all 0.3s ease;
              
              &:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
                border-color: rgba(102, 126, 234, 0.3);
              }
              
              .vital-label {
                font-size: 11px;
                color: #7f8c8d;
                margin-bottom: 4px;
                font-weight: 600;
              }
              
              .vital-value {
                font-size: 16px;
                font-weight: 700;
                color: #2c3e50;
                margin-bottom: 4px;
              }
              
              .vital-status {
                font-size: 10px;
                padding: 2px 6px;
                border-radius: 8px;
                display: inline-block;
                font-weight: 600;
                
                &.status-normal {
                  background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
                  color: white;
                }
                
                &.status-critical {
                  background: #c0392b;
                  color: white;
                  animation: pulse 2s infinite;
                }
                
                &.status-high {
                  background: #f56c6c;
                  color: white;
                }
                
                &.status-warning {
                  background: #e6a23c;
                  color: white;
                }
                
                &.status-low {
                  background: #409eff;
                  color: white;
                }
              }
            }
          }
        }
        
        /* Ⅰ级急危状态脉冲动画 */
        @keyframes pulse {
          0%, 100% {
            opacity: 1;
            box-shadow: 0 0 0 0 rgba(192, 57, 43, 0.7);
          }
          50% {
            opacity: 0.8;
            box-shadow: 0 0 0 10px rgba(192, 57, 43, 0);
          }
        }
        
        .ai-analysis,
        .doctor-diagnosis {
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }
        
        .ai-analysis {
          .ai-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            flex-shrink: 0;
            
            h4 {
              margin: 0;
              font-size: 14px;
              font-weight: 700;
              color: #2c3e50;
            }
          }
          
          .ai-loading {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 20px;
            
            .loading-text {
              text-align: center;
              color: #909399;
              font-size: 13px;
              margin-top: 12px;
            }
          }
          
          .ai-content {
            flex: 1;
            min-height: 0;
            overflow-y: auto;
            margin-bottom: 10px;
            
            /* 细滚动条 */
            &::-webkit-scrollbar {
              width: 4px;
            }
            &::-webkit-scrollbar-track {
              background: rgba(0,0,0,0.05);
              border-radius: 2px;
            }
            &::-webkit-scrollbar-thumb {
              background: rgba(102, 126, 234, 0.3);
              border-radius: 2px;
            }
            
            .diagnosis-text-area {
              padding: 12px;
              background: #fafafa;
              border-radius: 8px;
              border: 1px solid #eee;
              
              .ai-section {
                margin-bottom: 14px;
                
                &:last-child {
                  margin-bottom: 0;
                }
                
                .section-title {
                  font-size: 13px;
                  font-weight: 700;
                  color: #5e35b1;
                  margin-bottom: 8px;
                  padding-bottom: 4px;
                  border-bottom: 1px dashed rgba(94, 53, 177, 0.2);
                }
                
                .section-content {
                  font-size: 13px;
                  line-height: 1.8;
                  color: #34495e;
                  white-space: pre-line;
                  word-wrap: break-word;
                }
                
                .section-list {
                  margin: 0;
                  padding-left: 20px;
                  
                  li {
                    font-size: 13px;
                    line-height: 1.8;
                    color: #34495e;
                    margin-bottom: 4px;
                    
                    &:last-child {
                      margin-bottom: 0;
                    }
                  }
                }
              }
            }
            
            .no-diagnosis-hint {
              display: flex;
              align-items: center;
              justify-content: center;
              height: 100%;
              min-height: 80px;
              color: #909399;
              font-size: 13px;
            }
          }
          
          .ai-footer {
            flex-shrink: 0;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            padding-top: 10px;
            border-top: 1px solid #eee;
            
            .confidence-bar {
              flex: 1;
              display: flex;
              align-items: center;
              gap: 10px;
              
              .label {
                font-size: 12px;
                color: #606266;
                font-weight: 600;
                white-space: nowrap;
              }
              
              .el-progress {
                flex: 1;
              }
            }
          }
        }
        
        .doctor-diagnosis {
          overflow: hidden;
          
          h4 {
            margin: 0 0 10px 0;
            font-size: 14px;
            font-weight: 700;
            color: #2c3e50;
            flex-shrink: 0;
          }
          
          :deep(.el-form) {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            
            .el-form-item {
              margin-bottom: 10px;
              
              &:nth-child(1),
              &:nth-child(2) {
                flex: 1;
                display: flex;
                flex-direction: column;
                min-height: 0;
                
                .el-form-item__content {
                  flex: 1;
                  display: flex;
                  flex-direction: column;
                  
                  .el-textarea {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    
                    .el-textarea__inner {
                      flex: 1;
                      resize: none;
                    }
                  }
                }
              }
              
              &:last-child {
                margin-bottom: 0;
                margin-top: auto;
                flex-shrink: 0;
              }
              
              .el-form-item__label {
                font-weight: 600;
                color: #2c3e50;
                font-size: 13px;
              }
              
              .el-textarea__inner {
                border-radius: 8px;
                border: 1px solid #e8e8e8;
                font-size: 13px;
                
                &:focus {
                  border-color: #667eea;
                }
              }
              
              .el-radio-group {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                
                .el-radio {
                  margin-right: 0;
                  
                  .level-1 { color: #f56c6c; font-weight: 600; }
                  .level-2 { color: #e6a23c; font-weight: 600; }
                  .level-3 { color: #f9a825; font-weight: 600; }
                  .level-4 { color: #67c23a; font-weight: 600; }
                }
              }
            }
          }
        }
        
      }
    }
  }
  
  .ai-detailed-analysis {
    .analysis-section {
      margin-bottom: 20px;
      
      h4 {
        color: #333;
        margin-bottom: 10px;
      }
      
      p {
        color: #666;
        line-height: 1.6;
      }
      
      ul {
        margin: 0;
        padding-left: 20px;
        
        li {
          color: #666;
          margin-bottom: 5px;
        }
      }
    }
  }
  
  // 设置提示样式
  .setting-hint {
    margin-left: 12px;
    font-size: 13px;
    color: #95a5a6;
  }
}
</style>