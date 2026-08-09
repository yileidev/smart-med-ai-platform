<template>
  <div class="system-monitoring">
    <!-- 核心服务状态 -->
    <el-row :gutter="20" class="service-status-row">
      <el-col :span="6">
        <div class="status-card" :class="{'status-online': serviceStatus.backend}">
          <div class="status-icon">
            <el-icon :size="32"><Monitor /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-name">Spring Boot 后端</div>
            <div class="status-value">{{ serviceStatus.backend ? '运行中' : '离线' }}</div>
          </div>
          <div class="status-indicator" :class="serviceStatus.backend ? 'online' : 'offline'"></div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="status-card" :class="{'status-online': serviceStatus.mqtt}">
          <div class="status-icon">
            <el-icon :size="32"><Connection /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-name">MQTT Broker</div>
            <div class="status-value">{{ serviceStatus.mqtt ? '已连接' : '断开' }}</div>
          </div>
          <div class="status-indicator" :class="serviceStatus.mqtt ? 'online' : 'offline'"></div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="status-card" :class="{'status-online': serviceStatus.baichuanAI}">
          <div class="status-icon">
            <el-icon :size="32"><Cpu /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-name">百川AI大模型</div>
            <div class="status-value">{{ serviceStatus.baichuanAI ? '在线' : '离线' }}</div>
          </div>
          <div class="status-indicator" :class="serviceStatus.baichuanAI ? 'online' : 'offline'"></div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="status-card" :class="{'status-online': serviceStatus.chromaDB}">
          <div class="status-icon">
            <el-icon :size="32"><Files /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-name">Chroma 向量库</div>
            <div class="status-value">{{ serviceStatus.chromaDB ? '正常' : '异常' }}</div>
          </div>
          <div class="status-indicator" :class="serviceStatus.chromaDB ? 'online' : 'offline'"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 实时监控数据 -->
    <el-row :gutter="20" class="monitor-cards">
      <el-col :span="6">
        <div class="content-card monitor-card">
          <div class="card-body">
            <div class="monitor-item">
              <div class="monitor-label">CPU使用率</div>
              <div class="monitor-value">{{ Math.round(monitoring.cpuUsage || 0) }}%</div>
              <el-progress 
                :percentage="Math.round(monitoring.cpuUsage || 0)" 
                :color="getProgressColor(monitoring.cpuUsage)"
                :show-text="false"
              />
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="content-card monitor-card">
          <div class="card-body">
            <div class="monitor-item">
              <div class="monitor-label">内存使用率</div>
              <div class="monitor-value">{{ Math.round(monitoring.memoryUsage || 0) }}%</div>
              <el-progress 
                :percentage="Math.round(monitoring.memoryUsage || 0)"
                :color="getProgressColor(monitoring.memoryUsage)"
                :show-text="false"
              />
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="content-card monitor-card">
          <div class="card-body">
            <div class="monitor-item">
              <div class="monitor-label">系统负载</div>
              <div class="monitor-value">{{ (monitoring.systemLoad || 0).toFixed(2) }}</div>
              <el-progress 
                :percentage="Math.round((monitoring.systemLoad || 0) * 50)"
                :color="getProgressColor((monitoring.systemLoad || 0) * 50)"
                :show-text="false"
              />
            </div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="content-card monitor-card">
          <div class="card-body">
            <div class="monitor-item">
              <div class="monitor-label">在线用户</div>
              <div class="monitor-value">{{ monitoring.onlineUsers || 0 }}</div>
              <div class="monitor-unit">人</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 边缘设备监控 -->
    <div class="content-card">
      <div class="card-header">
        <h3>
          <el-icon><VideoCamera /></el-icon>
          边缘设备状态 (Jetson Orin Nano)
        </h3>
        <div class="header-actions">
          <el-button size="small" @click="refreshEdgeDevices">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
      <div class="card-body">
        <el-table :data="edgeDevices" stripe v-loading="edgeLoading">
          <el-table-column prop="deviceId" label="设备ID" width="180" />
          <el-table-column prop="deviceName" label="设备名称" width="150" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'" size="small">
                {{ row.status === 'ONLINE' ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastHeartbeat" label="最后心跳" width="180">
            <template #default="{ row }">
              {{ formatDate(row.lastHeartbeat) || '从未连接' }}
            </template>
          </el-table-column>
          <el-table-column prop="triageCount" label="今日分诊" width="100" />
          <el-table-column prop="cpuUsage" label="CPU" width="80">
            <template #default="{ row }">
              {{ row.cpuUsage ? row.cpuUsage + '%' : '--' }}
            </template>
          </el-table-column>
          <el-table-column prop="memoryUsage" label="内存" width="80">
            <template #default="{ row }">
              {{ row.memoryUsage ? row.memoryUsage + '%' : '--' }}
            </template>
          </el-table-column>
          <el-table-column prop="sensors" label="传感器状态" min-width="200">
            <template #default="{ row }">
              <div class="sensor-tags" v-if="row.sensors">
                <el-tag size="small" :type="row.sensors.temperature ? 'success' : 'danger'">
                  体温{{ row.sensors.temperature ? '√' : '×' }}
                </el-tag>
                <el-tag size="small" :type="row.sensors.heartRate ? 'success' : 'danger'">
                  心率{{ row.sensors.heartRate ? '√' : '×' }}
                </el-tag>
                <el-tag size="small" :type="row.sensors.bloodOxygen ? 'success' : 'danger'">
                  血氧{{ row.sensors.bloodOxygen ? '√' : '×' }}
                </el-tag>
                <el-tag size="small" :type="row.sensors.microphone ? 'success' : 'danger'">
                  麦克风{{ row.sensors.microphone ? '√' : '×' }}
                </el-tag>
              </div>
              <span v-else>--</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="edgeDevices.length === 0 && !edgeLoading" description="暂无边缘设备连接" />
      </div>
    </div>

    <!-- AI服务详情 & Drools规则引擎 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>
              <el-icon><Cpu /></el-icon>
              AI服务详情
            </h3>
          </div>
          <div class="card-body">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="百川模型">
                <el-tag :type="aiStatus.baichuan?.status === 'online' ? 'success' : 'danger'" size="small">
                  {{ aiStatus.baichuan?.status === 'online' ? '在线' : '离线' }}
                </el-tag>
                <span class="ml-2">{{ aiStatus.baichuan?.model || 'Baichuan2-Turbo-192k' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="RAG增强">
                <el-tag :type="aiStatus.rag?.enabled ? 'success' : 'info'" size="small">
                  {{ aiStatus.rag?.enabled ? '已启用' : '未启用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="向量知识库">
                <span>{{ aiStatus.vectorDB?.documentCount || 0 }} 条知识</span>
              </el-descriptions-item>
              <el-descriptions-item label="今日调用次数">
                {{ aiStatus.todayCallCount || 0 }} 次
              </el-descriptions-item>
              <el-descriptions-item label="平均响应时间">
                {{ aiStatus.avgResponseTime || '--' }} ms
              </el-descriptions-item>
              <el-descriptions-item label="API密钥状态">
                <el-tag :type="aiStatus.apiKeyValid ? 'success' : 'danger'" size="small">
                  {{ aiStatus.apiKeyValid ? '有效' : '无效/未配置' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>
      
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>
              <el-icon><Setting /></el-icon>
              Drools 规则引擎
            </h3>
          </div>
          <div class="card-body">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="引擎状态">
                <el-tag type="success" size="small">运行中</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="分诊优先级规则">
                <el-tag type="info" size="small">triage-priority.drl</el-tag>
                <span class="ml-2 rule-count">4 条规则</span>
              </el-descriptions-item>
              <el-descriptions-item label="医生分配规则">
                <el-tag type="info" size="small">doctor-assignment.drl</el-tag>
                <span class="ml-2 rule-count">3 条规则</span>
              </el-descriptions-item>
              <el-descriptions-item label="资源调度规则">
                <el-tag type="info" size="small">medical-resource-allocation.drl</el-tag>
                <span class="ml-2 rule-count">7 条规则</span>
              </el-descriptions-item>
              <el-descriptions-item label="今日规则触发">
                {{ droolsStatus.todayTriggerCount || 0 }} 次
              </el-descriptions-item>
              <el-descriptions-item label="规则执行成功率">
                {{ droolsStatus.successRate || '100' }}%
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 网络流量 & 系统信息 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>网络流量监控</h3>
          </div>
          <div class="card-body">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="traffic-item">
                  <div class="traffic-label">
                    <el-icon><Download /></el-icon>
                    入站流量
                  </div>
                  <div class="traffic-value">
                    {{ Math.round(monitoring.networkIn || 0) }} KB/s
                  </div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="traffic-item">
                  <div class="traffic-label">
                    <el-icon><Upload /></el-icon>
                    出站流量
                  </div>
                  <div class="traffic-value">
                    {{ Math.round(monitoring.networkOut || 0) }} KB/s
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-col>
      
      <el-col :span="12">
        <div class="content-card">
          <div class="card-header">
            <h3>系统信息</h3>
          </div>
          <div class="card-body">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="CPU核心">
                {{ monitoring.availableProcessors || '--' }} 核
              </el-descriptions-item>
              <el-descriptions-item label="活动线程">
                {{ monitoring.threadCount || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="JVM总内存">
                {{ monitoring.totalMemoryMB || '--' }} MB
              </el-descriptions-item>
              <el-descriptions-item label="已用内存">
                {{ monitoring.usedMemoryMB || '--' }} MB
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 系统警报 -->
    <div class="content-card">
      <div class="card-header">
        <h3>
          <el-icon><Bell /></el-icon>
          系统警报
        </h3>
      </div>
      <div class="card-body">
        <el-empty v-if="alerts.length === 0" description="暂无警报信息" />
        <div v-else class="alert-list">
          <el-alert
            v-for="alert in alerts"
            :key="alert.id"
            :title="alert.title"
            :type="alert.type"
            :description="alert.description"
            show-icon
            class="alert-item"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getRealtimeMonitoring } from '@/api/admin'
import request from '@/utils/request'
import { 
  Refresh, Download, Upload, Monitor, Connection, 
  Cpu, Files, VideoCamera, Setting, Bell 
} from '@element-plus/icons-vue'

const monitoring = ref({})
const edgeDevices = ref([])
const edgeLoading = ref(false)
const alerts = ref([])

// 服务状态
const serviceStatus = reactive({
  backend: false,
  mqtt: false,
  baichuanAI: false,
  chromaDB: false
})

// AI状态
const aiStatus = reactive({
  baichuan: { status: 'offline', model: '' },
  rag: { enabled: false },
  vectorDB: { documentCount: 0 },
  todayCallCount: 0,
  avgResponseTime: 0,
  apiKeyValid: false
})

// Drools状态
const droolsStatus = reactive({
  todayTriggerCount: 0,
  successRate: 0,
  ruleCount: 0
})

let timer = null

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const fetchMonitoring = async () => {
  try {
    const response = await getRealtimeMonitoring()
    const data = response.data || {}
    monitoring.value = data
    serviceStatus.backend = true
    serviceStatus.mqtt = data.mqttConnected || false
  } catch (error) {
    console.error('获取监控数据失败:', error)
    serviceStatus.backend = false
    serviceStatus.mqtt = false
  }
}

const refreshEdgeDevices = async () => {
  edgeLoading.value = true
  try {
    const response = await request({
      url: '/admin/edge/devices',
      method: 'get'
    })
    edgeDevices.value = response.data || []
  } catch (error) {
    console.error('获取边缘设备失败:', error)
    edgeDevices.value = []
  } finally {
    edgeLoading.value = false
  }
}

const fetchAIStatus = async () => {
  try {
    const response = await request({
      url: '/admin/ai/health',
      method: 'get'
    })
    if (response.data) {
      Object.assign(aiStatus, response.data)
      serviceStatus.baichuanAI = response.data.baichuan?.status === 'online'
      serviceStatus.chromaDB = response.data.vectorDB?.status === 'online'
    }
  } catch (error) {
    console.error('获取AI状态失败:', error)
    serviceStatus.baichuanAI = false
  }
}

const fetchDroolsStatus = async () => {
  try {
    const response = await request({
      url: '/admin/drools/status',
      method: 'get'
    })
    if (response.data) {
      Object.assign(droolsStatus, response.data)
    }
  } catch (error) {
    console.error('获取Drools状态失败:', error)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

const startPolling = () => {
  timer = setInterval(() => {
    fetchMonitoring()
  }, 5000)
}

onMounted(async () => {
  await Promise.all([
    fetchMonitoring(),
    refreshEdgeDevices(),
    fetchAIStatus(),
    fetchDroolsStatus()
  ])
  startPolling()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.system-monitoring {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .service-status-row {
    margin-bottom: 20px;
    
    .status-card {
      display: flex;
      align-items: center;
      padding: 20px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      position: relative;
      overflow: hidden;
      
      &.status-online {
        border-left: 4px solid #67c23a;
      }
      
      .status-icon {
        width: 60px;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f0f9eb;
        border-radius: 50%;
        margin-right: 16px;
        color: #67c23a;
      }
      
      .status-info {
        flex: 1;
        
        .status-name {
          font-size: 14px;
          color: #666;
          margin-bottom: 4px;
        }
        
        .status-value {
          font-size: 18px;
          font-weight: bold;
          color: #333;
        }
      }
      
      .status-indicator {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        position: absolute;
        top: 12px;
        right: 12px;
        
        &.online {
          background: #67c23a;
          box-shadow: 0 0 8px #67c23a;
          animation: pulse 2s infinite;
        }
        
        &.offline {
          background: #f56c6c;
        }
      }
    }
  }
  
  @keyframes pulse {
    0% { opacity: 1; }
    50% { opacity: 0.5; }
    100% { opacity: 1; }
  }
  
  .monitor-cards {
    margin-bottom: 20px;
    
    .monitor-card {
      .card-body {
        padding: 20px;
      }
      
      .monitor-item {
        text-align: center;
        
        .monitor-label {
          font-size: 14px;
          color: #666;
          margin-bottom: 8px;
        }
        
        .monitor-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
          margin-bottom: 12px;
        }
        
        .monitor-unit {
          font-size: 12px;
          color: #999;
          margin-top: 8px;
        }
      }
    }
  }
  
  .content-card {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    margin-bottom: 20px;
    
    .card-header {
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 16px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
    
    .card-body {
      padding: 20px;
    }
  }
  
  .sensor-tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }
  
  .traffic-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 6px;
    
    .traffic-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #666;
    }
    
    .traffic-value {
      font-size: 18px;
      font-weight: bold;
      color: #333;
    }
  }
  
  .ml-2 {
    margin-left: 8px;
  }
  
  .rule-count {
    color: #999;
    font-size: 12px;
  }
  
  .alert-list {
    .alert-item {
      margin-bottom: 12px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
