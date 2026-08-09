<template>
  <div class="system-logs">
    <div class="content-card">
      <div class="card-header">
        <h3>系统日志</h3>
      </div>
      
      <div class="card-body">
        <!-- 搜索筛选 -->
        <div class="search-bar">
          <el-form :model="searchForm" inline>
            <el-form-item label="日志级别">
              <el-select
                v-model="searchForm.level"
                placeholder="请选择级别"
                clearable
              >
                <el-option label="调试" value="DEBUG" />
                <el-option label="信息" value="INFO" />
                <el-option label="警告" value="WARN" />
                <el-option label="错误" value="ERROR" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="操作">
              <el-input
                v-model="searchForm.action"
                placeholder="请输入操作名称"
                clearable
              />
            </el-form-item>
            
            <el-form-item label="用户名">
              <el-input
                v-model="searchForm.userName"
                placeholder="请输入用户名"
                clearable
              />
            </el-form-item>
            
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 数据表格 -->
        <el-table
          v-loading="loading"
          :data="tableData"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="id" label="ID" width="80" />
          
          <el-table-column prop="level" label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="getLevelTagType(row.level)" size="small">
                {{ getLevelLabel(row.level) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="userName" label="用户" width="120" />
          
          <el-table-column prop="action" label="操作" width="150" />
          
          <el-table-column prop="resourceType" label="资源类型" width="100" />
          
          <el-table-column prop="details" label="详情" min-width="200">
            <template #default="{ row }">
              <el-tooltip :content="row.details" placement="top" v-if="row.details">
                <div class="text-ellipsis">{{ row.details }}</div>
              </el-tooltip>
            </template>
          </el-table-column>
          
          <el-table-column prop="ipAddress" label="IP地址" width="120" />
          
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
    
    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="800px"
    >
      <div v-if="currentLog">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelTagType(currentLog.level)">
              {{ getLevelLabel(currentLog.level) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentLog.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
          <el-descriptions-item label="资源类型">{{ currentLog.resourceType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资源ID">{{ currentLog.resourceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentLog.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDate(currentLog.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="详情" :span="2">
            <div class="log-details">{{ currentLog.details || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="User Agent" :span="2">
            <div class="log-details">{{ currentLog.userAgent || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getLogs } from '@/api/admin'

const loading = ref(false)
const detailVisible = ref(false)

const searchForm = reactive({
  level: '',
  action: '',
  userName: ''
})

const dateRange = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableData = ref([])
const currentLog = ref(null)

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...searchForm
    }
    
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    
    const response = await getLogs(params)
    const { content, totalElements } = response.data
    
    tableData.value = content || []
    pagination.total = totalElements || 0
  } catch (error) {
    console.error('获取日志列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchLogs()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    level: '',
    action: '',
    userName: ''
  })
  dateRange.value = []
  handleSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchLogs()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchLogs()
}

// 查看详情
const handleView = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

// 辅助函数
const getLevelLabel = (level) => {
  const levelMap = {
    DEBUG: '调试',
    INFO: '信息',
    WARN: '警告',
    ERROR: '错误'
  }
  return levelMap[level] || level
}

const getLevelTagType = (level) => {
  const levelMap = {
    DEBUG: 'info',
    INFO: 'success',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return levelMap[level] || ''
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  fetchLogs()
})
</script>

<style lang="scss" scoped>
.system-logs {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .search-bar {
    margin-bottom: 20px;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 6px;
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .text-ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 200px;
  }
  
  .log-details {
    word-break: break-all;
    white-space: pre-wrap;
    max-height: 200px;
    overflow-y: auto;
  }
}
</style>