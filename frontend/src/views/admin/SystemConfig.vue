<template>
  <div class="system-config">
    <el-row :gutter="20">
      <!-- 系统配置 -->
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>系统配置</h3>
          </div>
          <div class="card-body">
            <el-form
              ref="systemFormRef"
              :model="systemConfig"
              label-width="120px"
            >
              <el-form-item label="系统名称">
                <el-input v-model="systemConfig.systemName" />
              </el-form-item>
              
              <el-form-item label="系统版本">
                <el-input v-model="systemConfig.version" readonly />
              </el-form-item>
              
              <el-form-item label="最大上传大小">
                <el-input v-model="systemConfig.maxUploadSize" />
              </el-form-item>
              
              <el-form-item label="会话超时时间">
                <el-input v-model="systemConfig.sessionTimeout" />
              </el-form-item>
              
              <el-form-item label="系统维护模式">
                <el-switch
                  v-model="systemConfig.maintenanceMode"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>
              
              <el-form-item label="用户注册">
                <el-switch
                  v-model="systemConfig.allowRegister"
                  active-text="允许"
                  inactive-text="禁止"
                />
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="saveSystemConfig" :loading="systemLoading">
                  保存配置
                </el-button>
                <el-button @click="resetSystemConfig">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>
      
      <!-- 调度规则配置 -->
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>资源调度规则</h3>
          </div>
          <div class="card-body">
            <el-form
              ref="rulesFormRef"
              :model="schedulingRules"
              label-width="140px"
            >
              <el-form-item label="自动调度">
                <el-switch
                  v-model="schedulingRules.autoScheduling"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>
              
              <el-form-item label="优先级">
                <el-select v-model="schedulingRules.priorityLevel">
                  <el-option label="低" value="LOW" />
                  <el-option label="中" value="MEDIUM" />
                  <el-option label="高" value="HIGH" />
                  <el-option label="紧急" value="URGENT" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="最大并发预约">
                <el-input-number
                  v-model="schedulingRules.maxConcurrentBookings"
                  :min="1"
                  :max="100"
                />
              </el-form-item>
              
              <el-form-item label="预约提前时间">
                <el-input-number
                  v-model="schedulingRules.advanceBookingHours"
                  :min="1"
                  :max="168"
                />
                <span style="margin-left: 8px; color: #999;">小时</span>
              </el-form-item>
              
              <el-form-item label="自动取消时间">
                <el-input-number
                  v-model="schedulingRules.autoCancelMinutes"
                  :min="5"
                  :max="1440"
                />
                <span style="margin-left: 8px; color: #999;">分钟</span>
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="saveSchedulingRules" :loading="rulesLoading">
                  保存规则
                </el-button>
                <el-button @click="resetSchedulingRules">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 邮件配置 -->
    <div class="content-card">
      <div class="card-header">
        <h3>邮件配置</h3>
      </div>
      <div class="card-body">
        <el-form
          ref="emailFormRef"
          :model="emailConfig"
          :rules="emailRules"
          label-width="120px"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="SMTP服务器" prop="smtpHost">
                <el-input v-model="emailConfig.smtpHost" />
              </el-form-item>
              
              <el-form-item label="端口" prop="smtpPort">
                <el-input-number v-model="emailConfig.smtpPort" :min="1" :max="65535" />
              </el-form-item>
              
              <el-form-item label="用户名" prop="username">
                <el-input v-model="emailConfig.username" />
              </el-form-item>
              
              <el-form-item label="密码" prop="password">
                <el-input v-model="emailConfig.password" type="password" show-password />
              </el-form-item>
            </el-col>
            
            <el-col :span="12">
              <el-form-item label="发件人邮箱">
                <el-input v-model="emailConfig.fromEmail" />
              </el-form-item>
              
              <el-form-item label="发件人名称">
                <el-input v-model="emailConfig.fromName" />
              </el-form-item>
              
              <el-form-item label="启用SSL">
                <el-switch
                  v-model="emailConfig.ssl"
                  active-text="是"
                  inactive-text="否"
                />
              </el-form-item>
              
              <el-form-item label="启用TLS">
                <el-switch
                  v-model="emailConfig.tls"
                  active-text="是"
                  inactive-text="否"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item>
            <el-button type="primary" @click="saveEmailConfig" :loading="emailLoading">
              保存配置
            </el-button>
            <el-button @click="testEmail" :loading="testLoading">
              测试连接
            </el-button>
            <el-button @click="resetEmailConfig">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 备份与恢复 -->
    <div class="content-card">
      <div class="card-header">
        <h3>数据备份与恢复</h3>
      </div>
      <div class="card-body">
        <el-alert
          title="重要提示"
          type="warning"
          description="备份和恢复操作可能会影响系统性能，建议在系统空闲时进行。"
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="backup-section">
              <h4>数据备份</h4>
              <p>创建系统数据的完整备份，包括用户数据、配置信息等。</p>
              <el-button type="primary" @click="createBackup" :loading="backupLoading">
                <el-icon><Download /></el-icon>
                创建备份
              </el-button>
            </div>
          </el-col>
          
          <el-col :span="12">
            <div class="restore-section">
              <h4>数据恢复</h4>
              <p>从备份文件恢复系统数据，此操作将覆盖现有数据。</p>
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :show-file-list="false"
                accept=".sql,.backup"
                :on-change="handleFileChange"
              >
                <el-button type="warning">
                  <el-icon><Upload /></el-icon>
                  选择备份文件
                </el-button>
              </el-upload>
              <el-button 
                type="danger" 
                @click="restoreBackup" 
                :loading="restoreLoading"
                :disabled="!backupFile"
                style="margin-left: 10px;"
              >
                执行恢复
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import { getSystemConfig, updateSystemConfig, getSchedulingRules, updateSchedulingRules } from '@/api/admin'

const systemFormRef = ref()
const rulesFormRef = ref()
const emailFormRef = ref()
const uploadRef = ref()

const systemLoading = ref(false)
const rulesLoading = ref(false)
const emailLoading = ref(false)
const testLoading = ref(false)
const backupLoading = ref(false)
const restoreLoading = ref(false)

const backupFile = ref(null)

const systemConfig = reactive({
  systemName: '医疗管理系统',
  version: '1.0.0',
  maxUploadSize: '10MB',
  sessionTimeout: '30分钟',
  maintenanceMode: false,
  allowRegister: true
})

const schedulingRules = reactive({
  autoScheduling: true,
  priorityLevel: 'HIGH',
  maxConcurrentBookings: 5,
  advanceBookingHours: 24,
  autoCancelMinutes: 30
})

const emailConfig = reactive({
  smtpHost: '',
  smtpPort: 587,
  username: '',
  password: '',
  fromEmail: '',
  fromName: '医疗管理系统',
  ssl: false,
  tls: true
})

const emailRules = {
  smtpHost: [{ required: true, message: '请输入SMTP服务器地址', trigger: 'blur' }],
  smtpPort: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 获取系统配置
const fetchSystemConfig = async () => {
  try {
    const response = await getSystemConfig()
    Object.assign(systemConfig, response.data)
  } catch (error) {
    console.error('获取系统配置失败:', error)
  }
}

// 保存系统配置
const saveSystemConfig = async () => {
  systemLoading.value = true
  try {
    await updateSystemConfig(systemConfig)
    ElMessage.success('系统配置保存成功')
  } catch (error) {
    console.error('保存系统配置失败:', error)
  } finally {
    systemLoading.value = false
  }
}

// 重置系统配置
const resetSystemConfig = () => {
  fetchSystemConfig()
}

// 获取调度规则
const fetchSchedulingRules = async () => {
  try {
    const response = await getSchedulingRules()
    Object.assign(schedulingRules, response.data)
  } catch (error) {
    console.error('获取调度规则失败:', error)
  }
}

// 保存调度规则
const saveSchedulingRules = async () => {
  rulesLoading.value = true
  try {
    await updateSchedulingRules(schedulingRules)
    ElMessage.success('调度规则保存成功')
  } catch (error) {
    console.error('保存调度规则失败:', error)
  } finally {
    rulesLoading.value = false
  }
}

// 重置调度规则
const resetSchedulingRules = () => {
  fetchSchedulingRules()
}

// 保存邮件配置
const saveEmailConfig = async () => {
  if (!emailFormRef.value) return
  
  try {
    await emailFormRef.value.validate()
    emailLoading.value = true
    
    // 模拟保存邮件配置
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('邮件配置保存成功')
  } catch (error) {
    console.error('保存邮件配置失败:', error)
  } finally {
    emailLoading.value = false
  }
}

// 测试邮件连接
const testEmail = async () => {
  if (!emailFormRef.value) return
  
  try {
    await emailFormRef.value.validate()
    testLoading.value = true
    
    // 模拟测试邮件连接
    await new Promise(resolve => setTimeout(resolve, 2000))
    ElMessage.success('邮件服务器连接测试成功')
  } catch (error) {
    ElMessage.error('邮件服务器连接测试失败')
  } finally {
    testLoading.value = false
  }
}

// 重置邮件配置
const resetEmailConfig = () => {
  Object.assign(emailConfig, {
    smtpHost: '',
    smtpPort: 587,
    username: '',
    password: '',
    fromEmail: '',
    fromName: '医疗管理系统',
    ssl: false,
    tls: true
  })
  
  if (emailFormRef.value) {
    emailFormRef.value.clearValidate()
  }
}

// 创建备份
const createBackup = async () => {
  try {
    await ElMessageBox.confirm(
      '创建备份可能需要一些时间，确定要继续吗？',
      '确认备份',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    backupLoading.value = true
    
    // 模拟创建备份
    await new Promise(resolve => setTimeout(resolve, 3000))
    ElMessage.success('数据备份创建成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建备份失败:', error)
    }
  } finally {
    backupLoading.value = false
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  backupFile.value = file
  ElMessage.info(`已选择文件: ${file.name}`)
}

// 恢复备份
const restoreBackup = async () => {
  if (!backupFile.value) {
    ElMessage.warning('请先选择备份文件')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '恢复备份将覆盖现有数据，此操作不可逆，确定要继续吗？',
      '危险操作确认',
      {
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    restoreLoading.value = true
    
    // 模拟恢复备份
    await new Promise(resolve => setTimeout(resolve, 5000))
    ElMessage.success('数据恢复完成')
    backupFile.value = null
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复备份失败:', error)
    }
  } finally {
    restoreLoading.value = false
  }
}

onMounted(async () => {
  await fetchSystemConfig()
  await fetchSchedulingRules()
})
</script>

<style lang="scss" scoped>
.system-config {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .backup-section,
  .restore-section {
    h4 {
      margin-bottom: 8px;
      color: #333;
    }
    
    p {
      margin-bottom: 16px;
      color: #666;
      font-size: 14px;
      line-height: 1.5;
    }
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>