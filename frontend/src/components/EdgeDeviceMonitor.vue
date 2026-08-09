<template>
  <div class="edge-device-monitor">
    <div class="monitor-header">
      <h3>📱 边缘设备实时监控</h3>
      <div class="header-actions">
        <el-button size="small" @click="refreshDevices" :loading="refreshing">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
        <el-badge :value="onlineDeviceCount" type="success">
          <el-button size="small" type="primary">在线设备</el-button>
        </el-badge>
      </div>
    </div>

    <div class="device-grid">
      <div 
        v-for="device in edgeDevices" 
        :key="device.deviceId"
        :class="['device-card', getDeviceStatusClass(device)]"
        @click="showDeviceDetail(device)"
      >
        <!-- 设备头部 -->
        <div class="device-header">
          <div class="device-info">
            <div class="device-name">{{ getDeviceName(device.deviceId) }}</div>
            <div :class="['status-indicator', getDeviceStatusClass(device)]">
              <i :class="getStatusIcon(device)"></i>
              <span>{{ getStatusText(device) }}</span>
            </div>
          </div>
          <div class="device-time">
            <i class="el-icon-time"></i>
            {{ formatLastHeartbeat(device.lastHeartbeat) }}
          </div>
        </div>

        <!-- 设备指标 -->
        <div class="device-metrics">
          <div class="metric-item">
            <div class="metric-label">今日数据</div>
            <div class="metric-value">{{ device.todayDataCount || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">总数据量</div>
            <div class="metric-value">{{ device.dataCount || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">在线状态</div>
            <div class="metric-value">
              <el-tag :type="device.online ? 'success' : 'danger'" size="mini">
                {{ device.online ? '在线' : '离线' }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 错误信息（如果有） -->
        <div v-if="device.errorMessage" class="device-error">
          <i class="el-icon-warning"></i>
          <span>{{ device.errorMessage }}</span>
        </div>
      </div>

      <!-- 添加设备卡片（管理员） -->
      <div v-if="isAdmin" class="device-card add-device" @click="showAddDevice">
        <div class="add-device-content">
          <i class="el-icon-plus"></i>
          <span>添加设备</span>
        </div>
      </div>
    </div>

    <!-- 实时数据流 -->
    <div class="realtime-data" v-if="realtimeMessages.length > 0">
      <div class="data-header">
        <h4>📊 实时数据流</h4>
        <el-button size="mini" @click="clearMessages" type="text">清空</el-button>
      </div>
      <div class="message-list">
        <div 
          v-for="message in realtimeMessages.slice(-5)" 
          :key="message.id"
          :class="['message-item', 'level-' + (message.triageLevel || 5)]"
        >
          <div class="message-header">
            <span class="device-id">{{ message.deviceId }}</span>
            <span class="triage-level">{{ formatTriageLevel(message.triageLevel) }}</span>
            <span class="timestamp">{{ formatTimestamp(message.timestamp) }}</span>
          </div>
          <div class="message-content">
            <div class="vital-signs">
              体温: {{ message.temperature }}°C, 
              心率: {{ message.heartRate }}/分, 
              血氧: {{ message.bloodOxygen }}%
            </div>
            <div class="voice-text">{{ message.voiceText }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备详情对话框 -->
    <el-dialog 
      title="设备详情" 
      v-model="deviceDetailVisible" 
      width="800px"
      @close="selectedDevice = null"
    >
      <div v-if="selectedDevice" class="device-detail">
        <!-- 详情内容 -->
        <div class="detail-sections">
          <div class="detail-section">
            <h4>设备信息</h4>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="设备ID">{{ selectedDevice.deviceId }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="selectedDevice.online ? 'success' : 'danger'">
                  {{ selectedDevice.online ? '在线' : '离线' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="最后心跳">{{ formatLastHeartbeat(selectedDevice.lastHeartbeat) }}</el-descriptions-item>
              <el-descriptions-item label="数据质量">
                <el-progress :percentage="Math.round((selectedDevice.avgQuality || 0) * 100)" :color="getQualityColor(selectedDevice.avgQuality)"></el-progress>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <h4>数据统计</h4>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-number">{{ selectedDevice.dataCount || 0 }}</div>
                <div class="stat-label">总数据量</div>
              </div>
              <div class="stat-card">
                <div class="stat-number">{{ selectedDevice.todayDataCount || 0 }}</div>
                <div class="stat-label">今日数据</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { edgeDeviceApi } from '@/api/edge-device'

export default {
  name: 'EdgeDeviceMonitor',
  setup() {
    // 响应式数据
    const edgeDevices = ref([])
    const realtimeMessages = ref([])
    const refreshing = ref(false)
    const deviceDetailVisible = ref(false)
    const selectedDevice = ref(null)
    
    // WebSocket连接
    let stompClient = null
    
    // 计算属性
    const onlineDeviceCount = computed(() => {
      return edgeDevices.value.filter(device => device.online).length
    })
    
    const isAdmin = computed(() => {
      // 这里应该从用户状态获取角色信息
      return true // 临时设置
    })

    // 获取设备状态
    const fetchDeviceStatus = async () => {
      try {
        refreshing.value = true
        const response = await edgeDeviceApi.getDeviceStatus()
        edgeDevices.value = response.data || []
      } catch (error) {
        console.error('获取设备状态失败:', error)
        ElMessage.error('获取设备状态失败')
      } finally {
        refreshing.value = false
      }
    }

    // 初始化WebSocket连接
    const initWebSocket = () => {
      try {
        const socket = new SockJS('/api/ws')
        stompClient = Stomp.over(socket)
        
        stompClient.connect({}, (frame) => {
          console.log('WebSocket连接成功:', frame)
          
          // 订阅边缘设备数据
          stompClient.subscribe('/topic/edge-triage', (message) => {
            const data = JSON.parse(message.body)
            handleNewTriageData(data)
          })
          
          // 订阅设备状态更新
          stompClient.subscribe('/topic/device-status', (message) => {
            const statusData = JSON.parse(message.body)
            handleDeviceStatusUpdate(statusData)
          })
          
        }, (error) => {
          console.error('WebSocket连接失败:', error)
        })
      } catch (error) {
        console.error('初始化WebSocket失败:', error)
      }
    }

    // 处理新的分诊数据
    const handleNewTriageData = (data) => {
      const message = {
        id: Date.now(),
        deviceId: data.edgeData?.deviceId,
        triageLevel: data.edgeData?.triageLevel,
        temperature: data.edgeData?.temperature,
        heartRate: data.edgeData?.heartRate,
        bloodOxygen: data.edgeData?.bloodOxygen,
        voiceText: data.edgeData?.voiceText,
        timestamp: new Date()
      }
      
      realtimeMessages.value.push(message)
      
      // 限制消息数量
      if (realtimeMessages.value.length > 50) {
        realtimeMessages.value = realtimeMessages.value.slice(-50)
      }
      
      // 刷新设备状态
      fetchDeviceStatus()
    }

    // 处理设备状态更新
    const handleDeviceStatusUpdate = (statusData) => {
      const deviceIndex = edgeDevices.value.findIndex(d => d.deviceId === statusData.deviceId)
      if (deviceIndex !== -1) {
        edgeDevices.value[deviceIndex] = {
          ...edgeDevices.value[deviceIndex],
          status: statusData.status,
          errorMessage: statusData.errorMessage,
          online: statusData.status === 'ONLINE'
        }
      }
    }

    // 工具方法
    const getDeviceName = (deviceId) => {
      const names = {
        'jetson-001': '急诊台1号',
        'jetson-002': '急诊台2号', 
        'jetson-003': '急诊台3号'
      }
      return names[deviceId] || `设备-${deviceId}`
    }

    const getDeviceStatusClass = (device) => {
      if (device.online) return 'online'
      if (device.status === 'ERROR') return 'error'
      return 'offline'
    }

    const getStatusIcon = (device) => {
      if (device.online) return 'el-icon-success'
      if (device.status === 'ERROR') return 'el-icon-error'
      return 'el-icon-warning'
    }

    const getStatusText = (device) => {
      if (device.online) return '在线'
      if (device.status === 'ERROR') return '故障'
      return '离线'
    }

    const formatLastHeartbeat = (timestamp) => {
      if (!timestamp) return '未知'
      const date = new Date(timestamp)
      const now = new Date()
      const diff = Math.floor((now - date) / 1000)
      
      if (diff < 60) return `${diff}秒前`
      if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
      return `${Math.floor(diff / 3600)}小时前`
    }

    const formatTriageLevel = (level) => {
      const levels = {
        1: 'I级(红)',
        2: 'II级(橙)',
        3: 'III级(黄)',
        4: 'IV级(绿)',
        5: 'V级(蓝)'
      }
      return levels[level] || 'N/A'
    }

    const formatTimestamp = (timestamp) => {
      return new Date(timestamp).toLocaleTimeString()
    }

    const getQualityColor = (quality) => {
      if (quality >= 0.9) return '#67C23A'
      if (quality >= 0.8) return '#E6A23C'
      return '#F56C6C'
    }

    // 事件处理
    const refreshDevices = () => {
      fetchDeviceStatus()
    }

    const showDeviceDetail = (device) => {
      selectedDevice.value = device
      deviceDetailVisible.value = true
    }

    const clearMessages = () => {
      realtimeMessages.value = []
    }

    const showAddDevice = () => {
      ElMessage.info('添加设备功能开发中')
    }

    // 生命周期
    onMounted(() => {
      fetchDeviceStatus()
      initWebSocket()
    })

    onUnmounted(() => {
      if (stompClient) {
        stompClient.disconnect()
      }
    })

    return {
      edgeDevices,
      realtimeMessages,
      refreshing,
      deviceDetailVisible,
      selectedDevice,
      onlineDeviceCount,
      isAdmin,
      refreshDevices,
      showDeviceDetail,
      clearMessages,
      showAddDevice,
      getDeviceName,
      getDeviceStatusClass,
      getStatusIcon,
      getStatusText,
      formatLastHeartbeat,
      formatTriageLevel,
      formatTimestamp,
      getQualityColor
    }
  }
}
</script>

<style scoped>
.edge-device-monitor {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.monitor-header h3 {
  margin: 0;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.device-card {
  border: 2px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.device-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.device-card.online {
  border-color: #52c41a;
  background: #f6ffed;
}

.device-card.offline {
  border-color: #faad14;
  background: #fffbe6;
}

.device-card.error {
  border-color: #ff4d4f;
  background: #fff1f0;
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.device-name {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.status-indicator.online {
  color: #52c41a;
}

.status-indicator.offline {
  color: #faad14;
}

.status-indicator.error {
  color: #ff4d4f;
}

.device-time {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.device-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.metric-item {
  text-align: center;
}

.metric-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.device-error {
  margin-top: 12px;
  padding: 8px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #ff4d4f;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.add-device {
  border-style: dashed;
  border-color: #d9d9d9;
  background: #fafafa;
}

.add-device-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: #999;
}

.add-device-content i {
  font-size: 24px;
  margin-bottom: 8px;
}

.realtime-data {
  margin-top: 20px;
  border-top: 1px solid #e9ecef;
  padding-top: 20px;
}

.data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.data-header h4 {
  margin: 0;
  color: #2c3e50;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
}

.message-item {
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  border-left: 4px solid #e9ecef;
}

.message-item.level-1 {
  background: #fff1f0;
  border-left-color: #ff4d4f;
}

.message-item.level-2 {
  background: #fff7e6;
  border-left-color: #fa8c16;
}

.message-item.level-3 {
  background: #fffbe6;
  border-left-color: #faad14;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
}

.device-id {
  font-weight: 600;
  color: #2c3e50;
}

.triage-level {
  padding: 2px 6px;
  border-radius: 10px;
  background: #f0f0f0;
  color: #666;
}

.timestamp {
  color: #999;
}

.message-content {
  font-size: 13px;
}

.vital-signs {
  color: #666;
  margin-bottom: 4px;
}

.voice-text {
  color: #2c3e50;
  font-weight: 500;
}

.device-detail .detail-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

@media (max-width: 768px) {
  .device-grid {
    grid-template-columns: 1fr;
  }
  
  .device-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>