<template>
  <div class="patient-history">
    <el-card class="page-header">
      <div class="header-content">
        <h2>已确诊患者查询</h2>
        <p>查看历史确诊患者信息及诊断记录</p>
      </div>
    </el-card>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="filterForm.keyword"
            placeholder="姓名/主诉"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 患者列表 -->
    <el-card class="table-card">
      <el-table
        :data="patientList"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="patientName" label="患者姓名" width="100" />
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">
            {{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="chiefComplaint" label="主诉" min-width="180" show-overflow-tooltip />
        <el-table-column prop="triageLevel" label="分诊等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getTriageLevelType(row.triageLevel)">
              {{ getTriageLevelText(row.triageLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedDepartment" label="就诊科室" width="100" />
        <el-table-column prop="doctorName" label="接诊医生" width="100" />
        <el-table-column prop="dataSource" label="数据来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.dataSource === 'edge-device' ? 'success' : 'info'">
              {{ row.dataSource === 'edge-device' ? '边缘设备' : '手动录入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="arrivalTime" label="到院时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.arrivalTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end;"
        @size-change="loadPatients"
        @current-change="loadPatients"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="患者诊断详情"
      width="700px"
    >
      <div v-if="currentPatient" class="patient-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="患者姓名">{{ currentPatient.patientName }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ currentPatient.age }}岁</el-descriptions-item>
            <el-descriptions-item label="性别">{{ currentPatient.gender === 'MALE' ? '男' : currentPatient.gender === 'FEMALE' ? '女' : '未知' }}</el-descriptions-item>
            <el-descriptions-item label="证件号">{{ currentPatient.idNumber || '未录入' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 分诊信息 -->
        <div class="detail-section">
          <h4>分诊信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="分诊等级">
              <el-tag :type="getTriageLevelType(currentPatient.triageLevel)">
                {{ getTriageLevelText(currentPatient.triageLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="分诊评分">{{ currentPatient.triageScore || '-' }}</el-descriptions-item>
            <el-descriptions-item label="就诊科室">{{ currentPatient.assignedDepartment }}</el-descriptions-item>
            <el-descriptions-item label="数据来源">{{ currentPatient.dataSource === 'edge-device' ? '边缘设备' : '手动录入' }}</el-descriptions-item>
            <el-descriptions-item label="边缘设备ID" v-if="currentPatient.edgeDeviceId">{{ currentPatient.edgeDeviceId }}</el-descriptions-item>
            <el-descriptions-item label="到院时间">{{ formatTime(currentPatient.arrivalTime) }}</el-descriptions-item>
            <el-descriptions-item label="确认时间">{{ formatTime(currentPatient.confirmedTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 主诉症状 -->
        <div class="detail-section">
          <h4>主诉症状</h4>
          <el-card shadow="never" class="complaint-card">
            {{ currentPatient.chiefComplaint || '未记录' }}
          </el-card>
        </div>

        <!-- 生命体征 -->
        <div class="detail-section" v-if="vitalSigns">
          <h4>生命体征</h4>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="体温">{{ vitalSigns.temperature }}°C</el-descriptions-item>
            <el-descriptions-item label="心率">{{ vitalSigns.heartRate }} bpm</el-descriptions-item>
            <el-descriptions-item label="血氧">{{ vitalSigns.bloodOxygen }}%</el-descriptions-item>
            <el-descriptions-item label="收缩压">{{ vitalSigns.systolicBP }} mmHg</el-descriptions-item>
            <el-descriptions-item label="舒张压">{{ vitalSigns.diastolicBP }} mmHg</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- AI诊断建议 -->
        <div class="detail-section" v-if="currentPatient.aiDiagnosis">
          <h4>AI诊断建议</h4>
          <el-card shadow="never" class="ai-diagnosis-card">
            <div class="ai-confidence" v-if="currentPatient.aiConfidence">
              <span>置信度:</span>
              <el-progress 
                :percentage="Math.round(currentPatient.aiConfidence * 100)" 
                :color="currentPatient.aiConfidence > 0.8 ? '#67c23a' : '#e6a23c'"
                style="width: 200px; display: inline-block; margin-left: 10px;"
              />
            </div>
            <div class="diagnosis-content">{{ currentPatient.aiDiagnosis }}</div>
          </el-card>
        </div>

        <!-- 诊疗信息 -->
        <div class="detail-section" v-if="currentPatient.doctor">
          <h4>诊疗信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="接诊医生">{{ currentPatient.doctor.name }}</el-descriptions-item>
            <el-descriptions-item label="医生科室">{{ currentPatient.doctor.department }}</el-descriptions-item>
            <el-descriptions-item label="分诊护士" v-if="currentPatient.nurse">{{ currentPatient.nurse.name }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 护士备注 -->
        <div class="detail-section" v-if="currentPatient.nurseComments">
          <h4>护士备注</h4>
          <el-card shadow="never">{{ currentPatient.nurseComments }}</el-card>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const patientList = ref([])
const detailDialogVisible = ref(false)
const currentPatient = ref(null)

const filterForm = reactive({
  keyword: '',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 计算生命体征
const vitalSigns = computed(() => {
  if (!currentPatient.value?.vitalSigns) return null
  try {
    return typeof currentPatient.value.vitalSigns === 'string' 
      ? JSON.parse(currentPatient.value.vitalSigns)
      : currentPatient.value.vitalSigns
  } catch {
    return null
  }
})

// 加载患者列表
const loadPatients = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (filterForm.keyword) {
      params.keyword = filterForm.keyword
    }
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startTime = filterForm.dateRange[0]
      params.endTime = filterForm.dateRange[1]
    }
    
    const res = await request.get('/admin/patients/diagnosed', { params })
    if (res.data) {
      patientList.value = res.data.content || []
      pagination.total = res.data.totalElements || 0
    }
  } catch (error) {
    ElMessage.error('加载患者列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadPatients()
}

// 重置筛选
const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.dateRange = null
  pagination.page = 1
  loadPatients()
}

// 查看详情
const viewDetail = async (row) => {
  try {
    const res = await request.get(`/admin/patients/diagnosed/${row.id}`)
    if (res.data) {
      currentPatient.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载患者详情失败')
  }
}

// 分诊等级样式
const getTriageLevelType = (level) => {
  const types = { 1: 'danger', 2: 'warning', 3: '', 4: 'success', 5: 'info' }
  return types[level] || 'info'
}

const getTriageLevelText = (level) => {
  const texts = { 1: 'I级-濒危', 2: 'II级-危重', 3: 'III级-急症', 4: 'IV级-次急', 5: 'V级-非急' }
  return texts[level] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { 
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(() => {
  loadPatients()
})
</script>

<style scoped>
.patient-history {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding: 20px;
  padding-bottom: 40px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.header-content p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 400px;
}

.patient-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.complaint-card {
  background: #fafafa;
}

.ai-diagnosis-card {
  background: #f0f9eb;
}

.ai-confidence {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.diagnosis-content {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
