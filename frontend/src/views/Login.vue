<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="animated-bg">
      <div class="gradient-circle circle-1"></div>
      <div class="gradient-circle circle-2"></div>
      <div class="gradient-circle circle-3"></div>
    </div>

    <!-- 主登录区域 -->
    <div class="login-container">
      <!-- 左侧：系统介绍 -->
      <div class="login-left">
        <div class="brand-section">
          <div class="brand-icon">
            <svg viewBox="0 0 64 64" class="heartbeat-icon">
              <path d="M32 54.4L8.8 31.2c-6.4-6.4-6.4-16.8 0-23.2s16.8-6.4 23.2 0c6.4-6.4 16.8-6.4 23.2 0s6.4 16.8 0 23.2L32 54.4z" fill="currentColor"/>
            </svg>
          </div>
          <h1 class="brand-title">急诊分诊与诊断系统</h1>
          <p class="brand-subtitle">Emergency Triage & Diagnosis System</p>
          <p class="brand-desc">基于边缘-云端协同的多模态AI医疗系统</p>
        </div>

        <div class="features-grid">
          <div class="feature-item" v-for="(feature, index) in features" :key="index">
            <div class="feature-icon">{{ feature.icon }}</div>
            <div class="feature-content">
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.desc }}</p>
            </div>
          </div>
        </div>

        <div class="system-stats">
          <div class="stat-item">
            <div class="stat-value">99.9%</div>
            <div class="stat-label">系统可用性</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">&lt;200ms</div>
            <div class="stat-label">AI响应时间</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">24/7</div>
            <div class="stat-label">全天候服务</div>
          </div>
        </div>
      </div>

      <!-- 右侧：登录表单 -->
      <div class="login-right">
        <div class="login-card">
          <div class="card-header">
            <h2 class="card-title">欢迎登录</h2>
            <p class="card-subtitle">请选择您的身份并登录系统</p>
          </div>

          <!-- 角色选择卡片 -->
          <div class="role-selection">
            <div 
              v-for="role in roles" 
              :key="role.value"
              :class="['role-card', { active: selectedRole === role.value }]"
              @click="selectRole(role.value)"
            >
              <div class="role-icon">{{ role.emoji }}</div>
              <div class="role-text">
                <span class="role-name">{{ role.label }}</span>
                <span class="role-tag">{{ role.tag }}</span>
              </div>
              <div class="role-check" v-if="selectedRole === role.value">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                </svg>
              </div>
            </div>
          </div>

          <!-- 登录表单 -->
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="用户名"
                size="large"
                prefix-icon="User"
                clearable
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="useDemo" @change="handleDemoChange">
                使用演示账号
              </el-checkbox>
              <a href="#" class="forgot-link">忘记密码？</a>
            </div>
            
            <el-button
              :loading="loading"
              type="primary"
              size="large"
              class="login-btn"
              @click="handleLogin"
            >
              <span v-if="!loading">立即登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form>

          <!-- 演示账号提示 -->
          <div class="demo-hint" v-if="useDemo">
            <div class="hint-icon">💡</div>
            <div class="hint-content">
              <strong>{{ getCurrentRole().label }}演示账号</strong>
              <span>{{ getCurrentRole().accounts.username }} / {{ getCurrentRole().accounts.password }}</span>
            </div>
          </div>

          <!-- 快速切换 -->
          <div class="quick-switch">
            <div class="divider">
              <span>快速切换身份</span>
            </div>
            <div class="quick-roles">
              <button 
                v-for="role in roles" 
                :key="role.value"
                :class="['quick-role-btn', { active: selectedRole === role.value }]"
                @click="selectRole(role.value)"
              >
                <span class="emoji">{{ role.emoji }}</span>
                <span class="label">{{ role.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)
const selectedRole = ref('DOCTOR')
const useDemo = ref(true)

// 角色配置
const roles = [
  { 
    value: 'DOCTOR', 
    label: '医生', 
    emoji: '👨‍⚕️',
    tag: 'Doctor',
    description: '诊断患者，制定治疗方案', 
    accounts: { username: 'doctor', password: 'doctor123' }
  },
  { 
    value: 'NURSE', 
    label: '护士', 
    emoji: '👩‍⚕️',
    tag: 'Nurse',
    description: '患者护理，分诊复核', 
    accounts: { username: 'nurse', password: 'nurse123' }
  },
  { 
    value: 'ADMIN', 
    label: '管理员', 
    emoji: '👨‍💼',
    tag: 'Admin',
    description: '系统管理，资源调配', 
    accounts: { username: 'admin', password: 'admin123' }
  }
]

// 系统特性
const features = [
  { icon: '🤖', title: 'AI智能分诊', desc: '多模态AI分析，精准评估病情' },
  { icon: '☁️', title: '边缘云协同', desc: '边缘计算+云端AI双重加速' },
  { icon: '📊', title: '实时监控', desc: '24/7生命体征监测预警' },
  { icon: '🔒', title: '数据安全', desc: '多层加密，符合医疗标准' }
]

const loginForm = reactive({
  username: 'doctor',
  password: 'doctor123'
})

const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
})

const getCurrentRole = () => {
  return roles.find(r => r.value === selectedRole.value) || roles[0]
}

const selectRole = (role) => {
  selectedRole.value = role
  const roleData = roles.find(r => r.value === role)
  if (roleData && useDemo.value) {
    loginForm.username = roleData.accounts.username
    loginForm.password = roleData.accounts.password
  } else if (!useDemo.value) {
    loginForm.username = ''
    loginForm.password = ''
  }
}

const handleDemoChange = (value) => {
  if (value) {
    const roleData = getCurrentRole()
    loginForm.username = roleData.accounts.username
    loginForm.password = roleData.accounts.password
  } else {
    loginForm.username = ''
    loginForm.password = ''
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    console.error('表单引用不存在')
    return
  }
  
  try {
    await loginFormRef.value.validate()
    loading.value = true
    
    const currentRole = getCurrentRole()
    const loginData = { 
      username: loginForm.username,
      password: loginForm.password, 
      role: selectedRole.value 
    }
    
    console.log('提交登录数据:', loginData)
    await userStore.login(loginData)
    console.log('登录成功，准备跳转')
    
    ElMessage.success({
      message: `${currentRole.label}登录成功！欢迎使用急诊分诊系统`,
      type: 'success',
      duration: 2000
    })
    
    // 等待状态更新后再跳转
    await nextTick()
    
    // 根据用户存储中的角色跳转（而不是表单中的角色）
    const routes = {
      'DOCTOR': '/doctor-dashboard',
      'NURSE': '/nurse-dashboard',
      'ADMIN': '/admin-dashboard'
    }
    const userRole = userStore.currentRole || selectedRole.value
    const targetRoute = routes[userRole] || '/dashboard'
    console.log('用户角色:', userRole)
    console.log('跳转到路由:', targetRoute)
    
    router.push(targetRoute)
    
  } catch (error) {
    console.error('登录失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '登录失败，请检查账号密码'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
// 全局重置
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

// 动态背景
.animated-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.gradient-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
  animation: float 20s infinite ease-in-out;
  
  &.circle-1 {
    width: 500px;
    height: 500px;
    top: -200px;
    left: -200px;
    animation-delay: 0s;
  }
  
  &.circle-2 {
    width: 400px;
    height: 400px;
    bottom: -150px;
    right: -150px;
    animation-delay: 7s;
  }
  
  &.circle-3 {
    width: 300px;
    height: 300px;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 14s;
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-30px, 30px) scale(0.9); }
}

// 主容器
.login-container {
  position: relative;
  z-index: 1;
  width: 90%;
  max-width: 1200px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 40px;
  
  @media (max-width: 968px) {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 20px;
  }
}

// 左侧区域
.login-left {
  display: flex;
  flex-direction: column;
  gap: 40px;
  color: white;
  
  @media (max-width: 968px) {
    gap: 25px;
  }
}

.brand-section {
  text-align: left;
  
  .brand-icon {
    width: 80px;
    height: 80px;
    margin-bottom: 20px;
    animation: heartbeat 2s infinite;
    
    .heartbeat-icon {
      width: 100%;
      height: 100%;
      color: white;
      filter: drop-shadow(0 4px 20px rgba(255,255,255,0.3));
    }
  }
  
  .brand-title {
    font-size: 42px;
    font-weight: 800;
    margin-bottom: 10px;
    line-height: 1.2;
    text-shadow: 0 2px 20px rgba(0,0,0,0.2);
    
    @media (max-width: 768px) {
      font-size: 32px;
    }
  }
  
  .brand-subtitle {
    font-size: 18px;
    margin-bottom: 8px;
    opacity: 0.95;
    font-weight: 300;
    letter-spacing: 1px;
  }
  
  .brand-desc {
    font-size: 14px;
    opacity: 0.85;
    font-weight: 300;
  }
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  10%, 30% { transform: scale(1.05); }
  20% { transform: scale(1); }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.feature-item {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  gap: 15px;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.15);
    transform: translateY(-5px);
    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  }
  
  .feature-icon {
    font-size: 32px;
    line-height: 1;
  }
  
  .feature-content {
    flex: 1;
    
    h3 {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 6px;
    }
    
    p {
      font-size: 13px;
      opacity: 0.9;
      line-height: 1.5;
    }
  }
}

.system-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  
  .stat-item {
    text-align: center;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 12px;
    padding: 20px;
    
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 8px;
    }
    
    .stat-label {
      font-size: 12px;
      opacity: 0.9;
    }
  }
}

// 右侧登录卡片
.login-right {
  display: flex;
  align-items: center;
}

.login-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  
  @media (max-width: 768px) {
    padding: 30px 25px;
  }
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
  
  .card-title {
    font-size: 28px;
    font-weight: 700;
    color: #333;
    margin-bottom: 8px;
  }
  
  .card-subtitle {
    font-size: 14px;
    color: #666;
  }
}

// 角色选择
.role-selection {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 25px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.role-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  
  &:hover {
    border-color: #667eea;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
    transform: translateY(-2px);
  }
  
  &.active {
    border-color: #667eea;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
    
    .role-text {
      .role-name {
        color: white;
      }
      
      .role-tag {
        background: rgba(255, 255, 255, 0.2);
        color: white;
      }
    }
  }
  
  .role-icon {
    font-size: 36px;
    line-height: 1;
  }
  
  .role-text {
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .role-name {
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }
    
    .role-tag {
      font-size: 11px;
      color: #999;
      background: #f3f4f6;
      padding: 2px 8px;
      border-radius: 10px;
      display: inline-block;
    }
  }
  
  .role-check {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 24px;
    height: 24px;
    background: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #667eea;
  }
}

// 登录表单
.login-form {
  margin-bottom: 20px;
  
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 15px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    
    &:hover, &.is-focus {
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
    }
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  
  .forgot-link {
    font-size: 13px;
    color: #667eea;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
  }
  
  &:active {
    transform: translateY(0);
  }
}

// 演示账号提示
.demo-hint {
  display: flex;
  gap: 12px;
  padding: 15px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 12px;
  margin-bottom: 20px;
  
  .hint-icon {
    font-size: 20px;
    line-height: 1;
  }
  
  .hint-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    strong {
      font-size: 13px;
      color: #92400e;
    }
    
    span {
      font-size: 12px;
      color: #78350f;
      font-family: 'Courier New', monospace;
      font-weight: 600;
    }
  }
}

// 快速切换
.quick-switch {
  margin-top: 25px;
  padding-top: 25px;
  border-top: 1px solid #e5e7eb;
  
  .divider {
    text-align: center;
    margin-bottom: 15px;
    
    span {
      font-size: 12px;
      color: #9ca3af;
      background: white;
      padding: 0 10px;
    }
  }
  
  .quick-roles {
    display: flex;
    justify-content: center;
    gap: 10px;
  }
  
  .quick-role-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border: 1px solid #e5e7eb;
    border-radius: 20px;
    background: white;
    cursor: pointer;
    transition: all 0.3s ease;
    font-size: 13px;
    color: #6b7280;
    
    &:hover {
      border-color: #667eea;
      color: #667eea;
      background: #f3f4f6;
    }
    
    &.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-color: transparent;
    }
    
    .emoji {
      font-size: 16px;
    }
  }
}

// 响应式优化
@media (max-width: 968px) {
  .login-left {
    display: none;
  }
  
  .login-container {
    grid-template-columns: 1fr;
    max-width: 500px;
  }
}
</style>