<template>
  <div class="resource-management">
    <div class="content-card">
      <div class="card-header">
        <h3>医疗资源管理</h3>
        <div class="header-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增资源
          </el-button>
        </div>
      </div>
      
      <div class="card-body">
        <!-- 搜索筛选 -->
        <div class="search-bar">
          <el-form :model="searchForm" inline>
            <el-form-item label="关键词">
              <el-input
                v-model="searchForm.keyword"
                placeholder="请输入资源名称或描述"
                clearable
                @clear="handleSearch"
              />
            </el-form-item>
            
            <el-form-item label="资源类型">
              <el-select
                v-model="searchForm.type"
                placeholder="请选择类型"
                clearable
                @clear="handleSearch"
              >
                <el-option label="设备" value="EQUIPMENT" />
                <el-option label="药品" value="MEDICINE" />
                <el-option label="房间" value="ROOM" />
                <el-option label="床位" value="BED" />
                <el-option label="车辆" value="VEHICLE" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="状态">
              <el-select
                v-model="searchForm.status"
                placeholder="请选择状态"
                clearable
                @clear="handleSearch"
              >
                <el-option label="可用" value="AVAILABLE" />
                <el-option label="使用中" value="IN_USE" />
                <el-option label="维护中" value="MAINTENANCE" />
                <el-option label="故障" value="OUT_OF_ORDER" />
              </el-select>
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
          
          <el-table-column prop="name" label="资源名称" min-width="150" />
          
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)">
                {{ getTypeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="location" label="位置" width="120" />
          
          <el-table-column prop="totalQuantity" label="总数量" width="80" />
          
          <el-table-column prop="availableQuantity" label="可用数量" width="80" />
          
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">
              ¥{{ row.unitPrice || 0 }}
            </template>
          </el-table-column>
          
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="btn-group">
                <el-button size="small" @click="handleView(row)">查看</el-button>
                <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
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
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="资源名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入资源名称" />
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入资源描述"
          />
        </el-form-item>
        
        <el-form-item label="资源类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型">
            <el-option label="设备" value="EQUIPMENT" />
            <el-option label="药品" value="MEDICINE" />
            <el-option label="房间" value="ROOM" />
            <el-option label="床位" value="BED" />
            <el-option label="车辆" value="VEHICLE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="故障" value="OUT_OF_ORDER" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="位置" prop="location">
          <el-input v-model="formData.location" placeholder="请输入资源位置" />
        </el-form-item>
        
        <el-form-item label="总数量" prop="totalQuantity">
          <el-input-number
            v-model="formData.totalQuantity"
            :min="1"
            placeholder="请输入总数量"
          />
        </el-form-item>
        
        <el-form-item label="可用数量" prop="availableQuantity">
          <el-input-number
            v-model="formData.availableQuantity"
            :min="0"
            :max="formData.totalQuantity"
            placeholder="请输入可用数量"
          />
        </el-form-item>
        
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number
            v-model="formData.unitPrice"
            :min="0"
            :precision="2"
            placeholder="请输入单价"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getResources, createResource, updateResource, deleteResource } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const searchForm = reactive({
  keyword: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableData = ref([])

const formData = reactive({
  id: null,
  name: '',
  description: '',
  type: '',
  status: 'AVAILABLE',
  location: '',
  totalQuantity: 1,
  availableQuantity: 1,
  unitPrice: 0
})

const formRules = {
  name: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入总数量', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑资源' : '新增资源')

// 获取资源列表
const fetchResources = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...searchForm
    }
    
    const response = await getResources(params)
    const { content, totalElements } = response.data
    
    tableData.value = content || []
    pagination.total = totalElements || 0
  } catch (error) {
    console.error('获取资源列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchResources()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    type: '',
    status: ''
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchResources()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchResources()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看
const handleView = (row) => {
  ElMessage.info('查看功能待实现')
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资源"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteResource(row.id)
    ElMessage.success('删除成功')
    fetchResources()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    if (isEdit.value) {
      await updateResource(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createResource(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    fetchResources()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    description: '',
    type: '',
    status: 'AVAILABLE',
    location: '',
    totalQuantity: 1,
    availableQuantity: 1,
    unitPrice: 0
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 辅助函数
const getTypeLabel = (type) => {
  const typeMap = {
    EQUIPMENT: '设备',
    MEDICINE: '药品',
    ROOM: '房间',
    BED: '床位',
    VEHICLE: '车辆'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type) => {
  const typeMap = {
    EQUIPMENT: '',
    MEDICINE: 'success',
    ROOM: 'info',
    BED: 'warning',
    VEHICLE: 'danger'
  }
  return typeMap[type] || ''
}

const getStatusLabel = (status) => {
  const statusMap = {
    AVAILABLE: '可用',
    IN_USE: '使用中',
    MAINTENANCE: '维护中',
    OUT_OF_ORDER: '故障'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    AVAILABLE: 'success',
    IN_USE: 'info',
    MAINTENANCE: 'warning',
    OUT_OF_ORDER: 'danger'
  }
  return statusMap[status] || ''
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  fetchResources()
})
</script>

<style lang="scss" scoped>
.resource-management {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
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
  
  .btn-group {
    display: flex;
    gap: 8px;
  }
}
</style>