<template>
  <div class="diagnosed-patients">
    <el-card class="header-card">
      <div class="page-header">
        <h2>已确诊患者查询</h2>
        <p>查看历史确诊患者信息、诊断记录和治疗方案</p>
      </div>
    </el-card>

    <!-- 搜索筛选 -->
    <el-card style="margin-top: 20px;">
      <el-form :model="searchForm" inline>
        <el-form-item label="患者姓名">
          <el-input v-model="searchForm.patientName" placeholder="输入姓名" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idCard" placeholder="输入身份证号" clearable />
        </el-form-item>
        <el-form-item label="诊断科室">
          <el-select v-model="searchForm.department" placeholder="选择科室" clearable>
            <el-option label="急诊科" value="急诊科" />
            <el-option label="内科" value="内科" />
            <el-option label="外科" value="外科" />
            <el-option label="心内科" value="心内科" />
            <el-option label="神经内科" value="神经内科" />
            <el-option label="呼吸内科" value="呼吸内科" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
          <el-button type="success" @click="exportData">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.total }}</div>
              <div class="stat-label">总确诊数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completed }}</div>
              <div class="stat-label">已完成治疗</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.treating }}</div>
              <div class="stat-label">治疗中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.critical }}</div>
              <div class="stat-label">重症患者</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 患者列表 -->
    <el-card style="margin-top: 20px;">
      <el-table :data="patientList" v-loading="loading" stripe border>
        <el-table-column prop="patientName" label="患者姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">
            {{ formatGender(row.gender) }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idCard" label="身份证号" width="180">
          <template #default="{ row }">
            {{ maskIdCard(row.idCard || row.idNumber) }}
          </template>
        </el-table-column>
        <el-table-column prop="triageLevel" label="分诊等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.triageLevel)" size="small">
              {{ getLevelText(row.triageLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="诊断科室" width="100" />
        <el-table-column prop="diagnosis" label="诊断结果" min-width="200" show-overflow-tooltip />
        <el-table-column prop="doctorName" label="主治医生" width="100" />
        <el-table-column prop="diagnosisTime" label="诊断时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.diagnosisTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchPatients"
          @current-change="fetchPatients"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="患者详情" width="700px">
      <div v-if="currentPatient" class="patient-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="患者姓名">{{ currentPatient.patientName }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ formatGender(currentPatient.gender) }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{ currentPatient.age }}岁</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ maskIdCard(currentPatient.idCard || currentPatient.idNumber) }}</el-descriptions-item>
          <el-descriptions-item label="分诊等级" :span="2">
            <el-tag :type="getLevelType(currentPatient.triageLevel)">
              {{ getLevelText(currentPatient.triageLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="主诉症状" :span="2">
            {{ currentPatient.chiefComplaint || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">诊断信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="诊断科室">{{ currentPatient.department }}</el-descriptions-item>
          <el-descriptions-item label="主治医生">{{ currentPatient.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="诊断时间" :span="2">
            {{ formatTime(currentPatient.diagnosisTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="诊断结果" :span="2">
            {{ currentPatient.diagnosis }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案" :span="2">
            {{ currentPatient.treatmentPlan || '暂无' }}
          </el-descriptions-item>
          <el-descriptions-item label="医嘱" :span="2">
            {{ currentPatient.doctorAdvice || '暂无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left" v-if="currentPatient.aiDiagnosis">AI辅助诊断</el-divider>
        <div v-if="currentPatient.aiDiagnosis" class="ai-diagnosis">
          <el-alert type="info" :closable="false">
            {{ currentPatient.aiDiagnosis }}
          </el-alert>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, User, CircleCheck, Clock, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const detailVisible = ref(false)
const currentPatient = ref(null)

const searchForm = reactive({
  patientName: '',
  idCard: '',
  department: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statistics = reactive({
  total: 0,
  completed: 0,
  treating: 0,
  critical: 0
})

const patientList = ref([])

// 获取患者列表
const fetchPatients = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...searchForm
    }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res = await request.get('/admin/patients/diagnosed', { params })
    if (res.data) {
      patientList.value = res.data.content || res.data.list || []
      pagination.total = res.data.totalElements || res.data.total || 0
    }
  } catch (error) {
    console.error('获取患者列表失败:', error)
    ElMessage.error('获取患者列表失败')
    patientList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await request.get('/admin/patients/diagnosed/statistics')
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    // 保持默认值0
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchPatients()
}

// 重置搜索
const resetSearch = () => {
  searchForm.patientName = ''
  searchForm.idCard = ''
  searchForm.department = ''
  searchForm.dateRange = []
  handleSearch()
}

// 导出数据为Excel
const exportData = async () => {
  try {
    loading.value = true
    ElMessage.info('正在导出数据...')
    
    // 获取所有数据（不分页）
    const params = { ...searchForm, page: 0, size: 10000 }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    
    const res = await request.get('/admin/patients/diagnosed', { params })
    const data = res.data?.content || res.data?.list || patientList.value
    
    if (!data || data.length === 0) {
      ElMessage.warning('没有可导出的数据')
      return
    }
    
    // 转换为CSV格式
    const headers = ['患者姓名', '性别', '年龄', '身份证号', '分诊等级', '诊断科室', '诊断结果', '主治医生', '诊断时间']
    const rows = data.map(p => [
      p.patientName || '',
      formatGender(p.gender),
      p.age || '',
      maskIdCard(p.idCard || p.idNumber),
      getLevelText(p.triageLevel),
      p.department || p.assignedDepartment || '',
      (p.diagnosis || p.aiDiagnosis || '').replace(/,/g, '，'),
      p.doctorName || '',
      formatTime(p.diagnosisTime || p.confirmedTime)
    ])
    
    // 生成CSV内容
    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    
    // 下载文件
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `已确诊患者_${new Date().toLocaleDateString()}.csv`
    link.click()
    URL.revokeObjectURL(url)
    
    ElMessage.success(`成功导出 ${data.length} 条记录`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = async (patient) => {
  try {
    // 调用详情接口获取完整信息
    const res = await request.get(`/admin/patients/diagnosed/${patient.id}`)
    const detail = res.data || patient
    
    // 解析生命体征数据 - 处理可能的双重编码
    let vitalSigns = {}
    if (detail.vitalSigns) {
      try {
        let signsData = detail.vitalSigns
        // 处理双重编码：如果是字符串并且以引号开头，先解析一次
        if (typeof signsData === 'string') {
          if (signsData.startsWith('"') || signsData.startsWith("'")) {
            signsData = JSON.parse(signsData)
          }
          // 再次解析
          if (typeof signsData === 'string') {
            signsData = JSON.parse(signsData)
          }
        }
        vitalSigns = signsData || {}
        console.log('解析后的生命体征:', vitalSigns)
      } catch (e) {
        console.warn('解析生命体征失败:', e, detail.vitalSigns)
      }
    }
    
    // 设置当前患者详情
    currentPatient.value = {
      ...detail,
      // 身份证号兼容
      idCard: detail.idCard || detail.idNumber || '-',
      idNumber: detail.idNumber || detail.idCard || '-',
      // 生命体征
      heartRate: vitalSigns.heartRate || vitalSigns.heart_rate || '-',
      bloodOxygen: vitalSigns.bloodOxygen || vitalSigns.blood_oxygen || vitalSigns.spo2 || '-',
      temperature: vitalSigns.temperature || '-',
      bloodPressure: vitalSigns.bloodPressure || 
        (vitalSigns.systolicBP && vitalSigns.diastolicBP ? `${vitalSigns.systolicBP}/${vitalSigns.diastolicBP}` : '-'),
      department: detail.assignedDepartment || detail.department || '-',
      doctorName: detail.doctor?.name || detail.doctorName || '-',
      diagnosisTime: detail.confirmedTime || detail.diagnosisTime || '-',
      diagnosis: detail.diagnosis || detail.aiDiagnosis || '-',
      treatmentPlan: detail.treatmentPlan || '暂无',
      doctorAdvice: detail.doctorAdvice || detail.nurseComments || '暂无'
    }
    detailVisible.value = true
  } catch (error) {
    console.error('获取患者详情失败:', error)
    // 如果接口失败，使用列表数据
    currentPatient.value = patient
    detailVisible.value = true
  }
}

// 身份证脱敏
const maskIdCard = (idCard) => {
  if (!idCard) return '-'
  if (idCard.length >= 15) {
    return idCard.substring(0, 6) + '********' + idCard.substring(14)
  }
  return idCard
}

// 格式化性别显示
const formatGender = (gender) => {
  if (!gender) return '-'
  const genderMap = {
    'MALE': '男',
    'FEMALE': '女',
    'OTHER': '其他',
    'M': '男',
    'F': '女',
    '男': '男',
    '女': '女'
  }
  return genderMap[gender] || gender
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取等级样式
const getLevelType = (level) => {
  const types = { 1: 'danger', 2: 'warning', 3: 'info', 4: 'success' }
  return types[level] || 'info'
}

// 获取等级文本
const getLevelText = (level) => {
  const texts = { 1: '一级危急', 2: '二级紧急', 3: '三级较急', 4: '四级一般' }
  return texts[level] || '未知'
}

// 获取状态样式
const getStatusType = (status) => {
  const types = { '已完成': 'success', '治疗中': 'warning', '待治疗': 'info' }
  return types[status] || 'info'
}

onMounted(() => {
  fetchPatients()
  fetchStatistics()
})
</script>

<style lang="scss" scoped>
.diagnosed-patients {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .page-header {
    h2 { margin: 0 0 8px; color: #303133; }
    p { margin: 0; color: #909399; font-size: 14px; }
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 24px;
    }
    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #303133;
      }
      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .patient-detail {
    .el-divider {
      margin: 20px 0 16px;
    }
    .ai-diagnosis {
      margin-top: 12px;
    }
  }
}
</style>
