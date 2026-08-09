<template>
  <div class="user-management">
    <div class="content-card">
      <div class="card-header">
        <h3>用户管理</h3>
        <div class="header-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
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
                placeholder="请输入用户名、姓名或邮箱"
                clearable
                @clear="handleSearch"
              />
            </el-form-item>
            
            <el-form-item label="角色">
              <el-select
                v-model="searchForm.role"
                placeholder="请选择角色"
                clearable
                @clear="handleSearch"
              >
                <el-option label="管理员" value="ADMIN" />
                <el-option label="医生" value="DOCTOR" />
                <el-option label="护士" value="NURSE" />
                <el-option label="普通用户" value="USER" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="状态">
              <el-select
                v-model="searchForm.status"
                placeholder="请选择状态"
                clearable
                @clear="handleSearch"
              >
                <el-option label="正常" value="ACTIVE" />
                <el-option label="禁用" value="INACTIVE" />
                <el-option label="暂停" value="SUSPENDED" />
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
          
          <el-table-column prop="username" label="用户名" min-width="120" />
          
          <el-table-column prop="fullName" label="姓名" min-width="100" />
          
          <el-table-column prop="email" label="邮箱" min-width="180" />
          
          <el-table-column prop="phoneNumber" label="手机号" width="120" />
          
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="getRoleTagType(row.role)">
                {{ getRoleLabel(row.role) }}
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
          
          <el-table-column prop="lastLoginAt" label="最后登录" width="180">
            <template #default="{ row }">
              {{ formatDate(row.lastLoginAt) || '从未登录' }}
            </template>
          </el-table-column>
          
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="btn-group">
                <el-button size="small" @click="handleView(row)">查看</el-button>
                <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-dropdown @command="(command) => handleStatusChange(row, command)">
                  <el-button size="small" type="warning">
                    状态<el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="ACTIVE" :disabled="row.status === 'ACTIVE'">
                        启用
                      </el-dropdown-item>
                      <el-dropdown-item command="INACTIVE" :disabled="row.status === 'INACTIVE'">
                        禁用
                      </el-dropdown-item>
                      <el-dropdown-item command="SUSPENDED" :disabled="row.status === 'SUSPENDED'">
                        暂停
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="formData.username" 
            placeholder="请输入用户名"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="formData.fullName" placeholder="请输入姓名" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phoneNumber">
          <el-input v-model="formData.phoneNumber" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="护士" value="NURSE" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="ACTIVE">正常</el-radio>
            <el-radio label="INACTIVE">禁用</el-radio>
            <el-radio label="SUSPENDED">暂停</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const searchForm = reactive({
  keyword: '',
  role: '',
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
  username: '',
  password: '',
  fullName: '',
  email: '',
  phoneNumber: '',
  role: 'USER',
  status: 'ACTIVE'
})

const formRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...searchForm
    }
    
    const response = await getUsers(params)
    const { content, totalElements } = response.data
    
    tableData.value = content || []
    pagination.total = totalElements || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    role: '',
    status: ''
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchUsers()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchUsers()
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
  Object.assign(formData, { ...row, password: '' })
  dialogVisible.value = true
}

// 查看
const handleView = (row) => {
  ElMessage.info('查看功能待实现')
}

// 状态变更
const handleStatusChange = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      `确定要将用户"${row.username}"的状态改为"${getStatusLabel(status)}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(row.id, status)
    ElMessage.success('状态更新成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('状态更新失败:', error)
    }
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户"${row.username}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
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
      await updateUser(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createUser(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    fetchUsers()
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
    username: '',
    password: '',
    fullName: '',
    email: '',
    phoneNumber: '',
    role: 'USER',
    status: 'ACTIVE'
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 辅助函数
const getRoleLabel = (role) => {
  const roleMap = {
    ADMIN: '管理员',
    DOCTOR: '医生',
    NURSE: '护士',
    USER: '普通用户'
  }
  return roleMap[role] || role
}

const getRoleTagType = (role) => {
  const roleMap = {
    ADMIN: 'danger',
    DOCTOR: 'success',
    NURSE: 'info',
    USER: ''
  }
  return roleMap[role] || ''
}

const getStatusLabel = (status) => {
  const statusMap = {
    ACTIVE: '正常',
    INACTIVE: '禁用',
    SUSPENDED: '暂停'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    ACTIVE: 'success',
    INACTIVE: 'danger',
    SUSPENDED: 'warning'
  }
  return statusMap[status] || ''
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  fetchUsers()
})
</script>

<style lang="scss" scoped>
.user-management {
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
    gap: 6px;
    flex-wrap: nowrap; // 禁止换行
    white-space: nowrap; // 禁止文本换行
  }
}
</style>