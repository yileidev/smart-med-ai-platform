<template>
  <div class="dashboard-overview">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stats-card">
        <div class="stats-number">{{ overview.totalUsers || 0 }}</div>
        <div class="stats-label">总用户数</div>
      </div>
      
      <div class="stats-card success">
        <div class="stats-number">{{ overview.activeUsers || 0 }}</div>
        <div class="stats-label">活跃用户</div>
      </div>
      
      <div class="stats-card warning">
        <div class="stats-number">{{ overview.totalResources || 0 }}</div>
        <div class="stats-label">医疗资源</div>
      </div>
      
      <div class="stats-card info">
        <div class="stats-number">{{ overview.availableResources || 0 }}</div>
        <div class="stats-label">可用资源</div>
      </div>
    </div>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>系统监控</h3>
          </div>
          <div class="card-body">
            <div class="monitor-item">
              <span>CPU使用率</span>
              <el-progress 
                :percentage="Math.round(monitoring.cpuUsage || 0)" 
                :color="getProgressColor(monitoring.cpuUsage)"
              />
            </div>
            
            <div class="monitor-item">
              <span>内存使用率</span>
              <el-progress 
                :percentage="Math.round(monitoring.memoryUsage || 0)"
                :color="getProgressColor(monitoring.memoryUsage)"
              />
            </div>
            
            <div class="monitor-item">
              <span>系统负载</span>
              <el-progress 
                :percentage="Math.round((monitoring.systemLoad || 0) * 50)"
                :color="getProgressColor((monitoring.systemLoad || 0) * 50)"
              />
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>快速操作</h3>
          </div>
          <div class="card-body">
            <div class="quick-actions">
              <el-button 
                v-if="userStore.isAdmin"
                type="primary" 
                @click="$router.push('/admin/resources')"
              >
                <el-icon><Box /></el-icon>
                资源管理
              </el-button>
              
              <el-button 
                v-if="userStore.isAdmin"
                type="success" 
                @click="$router.push('/admin/users')"
              >
                <el-icon><User /></el-icon>
                用户管理
              </el-button>
              
              <el-button 
                v-if="userStore.isAdmin"
                type="warning" 
                @click="$router.push('/admin/logs')"
              >
                <el-icon><Document /></el-icon>
                查看日志
              </el-button>
              
              <el-button 
                v-if="userStore.isAdmin"
                type="info" 
                @click="$router.push('/admin/monitoring')"
              >
                <el-icon><Monitor /></el-icon>
                系统监控
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 最近活动 -->
    <div class="content-card">
      <div class="card-header">
        <h3>系统状态</h3>
      </div>
      <div class="card-body">
        <div class="system-status">
          <el-tag type="success" size="large">
            <el-icon><CircleCheck /></el-icon>
            系统运行正常
          </el-tag>
          
          <div class="status-info">
            <p>在线用户：{{ monitoring.onlineUsers || 0 }} 人</p>
            <p>网络流量：入 {{ Math.round(monitoring.networkIn || 0) }}KB/s，出 {{ Math.round(monitoring.networkOut || 0) }}KB/s</p>
            <p>最后更新：{{ new Date().toLocaleString() }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getOverviewStats, getSystemMonitoring } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { Box, User, Document, Monitor, CircleCheck } from '@element-plus/icons-vue'

const userStore = useUserStore()

const overview = ref({})
const monitoring = ref({})
let timer = null

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const fetchOverview = async () => {
  try {
    const response = await getOverviewStats()
    console.log('系统概览数据:', response)
    if (response.data && response.data.success) {
      overview.value = response.data.data || {}
    } else {
      overview.value = response.data || {}
    }
    console.log('概览数据加载成功:', overview.value)
  } catch (error) {
    console.error('获取概览数据失败:', error)
    // 失败时设置默认值
    overview.value = {
      totalUsers: 0,
      activeUsers: 0,
      totalResources: 0,
      availableResources: 0
    }
  }
}

const fetchMonitoring = async () => {
  try {
    const response = await getSystemMonitoring()
    console.log('系统监控数据:', response)
    if (response.data && response.data.success) {
      monitoring.value = response.data.data || {}
    } else {
      monitoring.value = response.data || {}
    }
    console.log('监控数据加载成功:', monitoring.value)
  } catch (error) {
    console.error('获取监控数据失败:', error)
    // 失败时设置默认值
    monitoring.value = {
      cpuUsage: 0,
      memoryUsage: 0,
      systemLoad: 0,
      onlineUsers: 0,
      networkIn: 0,
      networkOut: 0
    }
  }
}

const startPolling = () => {
  // 定时刷新监控数据
  timer = setInterval(() => {
    fetchMonitoring()
  }, 5000)
}

onMounted(async () => {
  console.log('系统概览页面开始加载...')
  console.log('用户信息:', userStore.userInfo)
  console.log('是否是管理员:', userStore.isAdmin)
  await fetchOverview()
  await fetchMonitoring()
  startPolling()
  console.log('系统概览页面加载完成')
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.dashboard-overview {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }
  
  .stats-card {
    background: white;
    padding: 24px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    
    .stats-number {
      font-size: 32px;
      font-weight: bold;
      color: #333;
      margin-bottom: 8px;
    }
    
    .stats-label {
      font-size: 14px;
      color: #666;
    }
    
    &.success .stats-number {
      color: #67c23a;
    }
    
    &.warning .stats-number {
      color: #e6a23c;
    }
    
    &.info .stats-number {
      color: #409eff;
    }
  }
  
  .content-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    overflow: hidden;
    
    .card-header {
      padding: 16px 20px;
      border-bottom: 1px solid #e4e7ed;
      
      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #333;
      }
    }
    
    .card-body {
      padding: 20px;
    }
  }
  
  .chart-row {
    margin-bottom: 20px;
  }
  
  .monitor-item {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    
    span {
      width: 100px;
      margin-right: 16px;
      font-size: 14px;
    }
    
    .el-progress {
      flex: 1;
    }
  }
  
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    gap: 12px;
    
    .el-button {
      height: 50px;
      
      .el-icon {
        margin-right: 6px;
      }
    }
  }
  
  .system-status {
    .el-tag {
      margin-bottom: 16px;
      
      .el-icon {
        margin-right: 6px;
      }
    }
    
    .status-info {
      p {
        margin: 8px 0;
        color: #666;
        font-size: 14px;
      }
    }
  }
}
</style>