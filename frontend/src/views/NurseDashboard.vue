<template>
  <div class="nurse-dashboard">
    <!-- 顶部导航 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>👩‍⚕️ 护士工作台</h1>
        <p>急诊分诊复核与患者管理系统</p>
      </div>
      <div class="header-right">
        <div class="real-time-status">
          <el-badge :value="newPatientsCount" type="danger">
            <el-button type="primary" @click="refreshData" :loading="loading">
              <i class="el-icon-refresh"></i> 实时刷新
            </el-button>
          </el-badge>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32">👩‍⚕️</el-avatar>
            <span>{{ currentUser }}</span>
            <i class="el-icon-arrow-down"></i>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主要工作区 -->
    <div class="main-workspace">
      <!-- 左侧：分诊复核队列 -->
      <div class="triage-queue">
        <div class="queue-header">
          <h3>🔍 分诊复核队列</h3>
          <div class="queue-controls">
            <el-select v-model="priorityFilter" placeholder="优先级筛选" size="small">
              <el-option label="全部" value=""></el-option>
              <el-option label="Ⅰ级-急危(红色)" value="1"></el-option>
              <el-option label="Ⅱ级-急重(橙色)" value="2"></el-option>
              <el-option label="Ⅲ级-急症(黄色)" value="3"></el-option>
              <el-option label="Ⅳ级-亚急症(绿色)" value="4"></el-option>
            </el-select>
          </div>
        </div>

        <div class="patient-queue">
          <div 
            v-for="patient in filteredPatients" 
            :key="patient.id"
            :class="['patient-item', { selected: selectedPatient?.id === patient.id }]"
            @click="selectPatient(patient)"
          >
            <div class="patient-header">
              <div class="patient-basic">
                <span class="patient-name">{{ patient.patientName || patient.patient?.patientName || '待登记' }}</span>
                <span :class="['ai-badge', `level-${patient.triageLevel}`]">
                  {{ getTriageLevelText(patient.triageLevel) }}
                </span>
              </div>
              <div class="patient-timing">
                <span class="arrival-time">{{ formatTime(patient.arrivalTime) }}</span>
                <span class="waiting-time">等待 {{ getWaitingTime(patient.arrivalTime) }}</span>
              </div>
            </div>

            <div class="patient-data">
              <div class="vital-signs-mini">
                <span class="vital">🌡️{{ getVitalSign(patient.vitalSigns, 'temperature') }}°C</span>
                <span class="vital">💓{{ getVitalSign(patient.vitalSigns, 'heartRate') }}/分</span>
                <span class="vital">🩸{{ getVitalSign(patient.vitalSigns, 'systolicBP') }}/{{ getVitalSign(patient.vitalSigns, 'diastolicBP') }}</span>
                <span class="vital">🫁{{ getVitalSign(patient.vitalSigns, 'bloodOxygen') }}%</span>
              </div>
              <div class="chief-complaint">
                <strong>主诉：</strong>{{ patient.chiefComplaint || '语音识别中...' }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间：患者详情和操作面板 -->
      <div class="patient-details">
        <div v-if="!selectedPatient" class="no-selection">
          <div class="empty-state">
            <i class="el-icon-user"></i>
            <h3>选择患者查看详情</h3>
            <p>从分诊队列中选择患者进行复核操作</p>
          </div>
        </div>

        <div v-else class="patient-detail-panel">
          <!-- 上半部分：患者信息和生命体征 -->
          <div class="upper-section">
            <!-- 患者基本信息 -->
            <div class="info-section">
              <div class="info-header">
                <h4>👤 患者信息</h4>
                <el-button 
                  size="small" 
                  type="text" 
                  @click="toggleEditMode"
                  :icon="editMode ? 'el-icon-check' : 'el-icon-edit'"
                >
                  {{ editMode ? '保存修改' : '编辑信息' }}
                </el-button>
              </div>
              <div class="info-grid">
                <div class="info-item">
                  <label>姓名：</label>
                  <span>{{ selectedPatient.patientName || selectedPatient.patient?.patientName || '待登记' }}</span>
                </div>
                <div class="info-item">
                  <label>证件号：</label>
                  <span>{{ selectedPatient.idNumber || selectedPatient.patient?.idCard || selectedPatient.patient?.idNumber || '未登记' }}</span>
                </div>
                <div class="info-item">
                  <label>到院时间：</label>
                  <span>{{ formatDateTime(selectedPatient.arrivalTime) }}</span>
                </div>
                <div class="info-item">
                  <label>边缘设备：</label>
                  <span>{{ selectedPatient.edgeDeviceId }}</span>
                </div>
                <div class="info-item">
                  <label>等待时间：</label>
                  <span class="waiting-highlight">{{ getWaitingTime(selectedPatient.arrivalTime) }}</span>
                </div>
                <div class="info-item info-item-full">
                  <label>主诉：</label>
                  <div class="editable-field">
                    <el-input
                      v-if="editMode"
                      v-model="editForm.chiefComplaint"
                      type="textarea"
                      :rows="2"
                      placeholder="请输入患者主诉..."
                      class="edit-textarea"
                    ></el-input>
                    <span v-else class="chief-complaint-text">{{ selectedPatient.chiefComplaint || '语音识别中...' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 生命体征详情 -->
            <div class="vitals-section">
              <h4>📊 生命体征详情</h4>
              <div class="vitals-chart">
                <div class="vital-detail">
                  <div class="vital-label">体温</div>
                  <div class="vital-edit-area">
                    <el-input-number
                      v-if="editMode"
                      v-model="editForm.vitalSigns.temperature"
                      :min="35"
                      :max="45"
                      :step="0.1"
                      :precision="1"
                      size="small"
                      class="vital-input"
                    ></el-input-number>
                    <div v-else class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'temperature') }}°C</div>
                  </div>
                  <div class="vital-status normal">正常</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">心率</div>
                  <div class="vital-edit-area">
                    <el-input-number
                      v-if="editMode"
                      v-model="editForm.vitalSigns.heartRate"
                      :min="40"
                      :max="200"
                      size="small"
                      class="vital-input"
                    ></el-input-number>
                    <div v-else class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'heartRate') }}</div>
                  </div>
                  <div class="vital-status abnormal">异常</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">血压</div>
                  <div class="vital-edit-area">
                    <div v-if="editMode" class="bp-inputs">
                      <el-input-number
                        v-model="editForm.vitalSigns.systolicBP"
                        :min="60"
                        :max="250"
                        size="small"
                        class="vital-input bp-input"
                        placeholder="收缩压"
                      ></el-input-number>
                      <span class="bp-separator">/</span>
                      <el-input-number
                        v-model="editForm.vitalSigns.diastolicBP"
                        :min="30"
                        :max="150"
                        size="small"
                        class="vital-input bp-input"
                        placeholder="舒张压"
                      ></el-input-number>
                    </div>
                    <div v-else class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'systolicBP') }}/{{ getVitalSign(selectedPatient.vitalSigns, 'diastolicBP') }}</div>
                  </div>
                  <div class="vital-status abnormal">偏高</div>
                </div>
                <div class="vital-detail">
                  <div class="vital-label">血氧</div>
                  <div class="vital-edit-area">
                    <el-input-number
                      v-if="editMode"
                      v-model="editForm.vitalSigns.bloodOxygen"
                      :min="70"
                      :max="100"
                      size="small"
                      class="vital-input"
                    ></el-input-number>
                    <div v-else class="vital-value">{{ getVitalSign(selectedPatient.vitalSigns, 'bloodOxygen') }}%</div>
                  </div>
                  <div class="vital-status low">偏低</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 下半部分：AI分析和护士复核 -->
          <div class="lower-section">
            <!-- AI分析结果 -->
            <div class="ai-analysis">
              <div class="ai-header">
                <h4>🤖 AI分析结果</h4>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="requestAIDiagnosis" 
                  :loading="aiDiagnosisLoading"
                  :disabled="!selectedPatient || !selectedPatient.chiefComplaint"
                >
                  <i class="el-icon-cpu"></i> 获取AI诊断
                </el-button>
              </div>
              
              <!-- AI诊断结果展示区域 -->
              <div v-if="!aiDiagnosisResult && !aiDiagnosisLoading" class="no-diagnosis">
                <div class="empty-ai-state">
                  <i class="el-icon-warning-outline"></i>
                  <p>点击获取AI诊断</p>
                  <span class="hint">提供初步诊断和分诊科室建议</span>
                </div>
              </div>
              
              <div v-if="aiDiagnosisLoading" class="ai-loading">
                <el-skeleton :rows="3" animated>
                  <template #template>
                    <el-skeleton-item variant="text" style="width: 100%" />
                    <el-skeleton-item variant="text" style="width: 80%" />
                    <el-skeleton-item variant="text" style="width: 60%" />
                  </template>
                </el-skeleton>
                <p class="loading-text">AI正在分析中，请稍候...</p>
              </div>
              
              <div v-if="aiDiagnosisResult" class="ai-result">
                <div class="ai-diagnosis">
                  <strong>AI初步诊断：</strong>
                  <p class="ai-text">{{ aiDiagnosisResult.diagnosis || aiDiagnosisResult.analysis }}</p>
                </div>
                <div class="confidence-meter">
                  <label>置信度：</label>
                  <el-progress 
                    :percentage="Math.round((aiDiagnosisResult.confidence || 0) * 100)"
                    :color="getConfidenceColor(aiDiagnosisResult.confidence)"
                    :show-text="false"
                  ></el-progress>
                  <span class="confidence-text">{{ Math.round((aiDiagnosisResult.confidence || 0) * 100) }}%</span>
                </div>
                
                <div v-if="aiDiagnosisResult.recommendedDepartment" class="ai-recommendations">
                  <div class="recommendation-item">
                    <strong>推荐科室：</strong>
                    <span class="department-tag">{{ aiDiagnosisResult.recommendedDepartment }}</span>
                  </div>
                  <div v-if="aiDiagnosisResult.urgency" class="recommendation-item">
                    <strong>紧急程度：</strong>
                    <span :class="['urgency-tag', aiDiagnosisResult.urgency.toLowerCase()]">{{ aiDiagnosisResult.urgency }}</span>
                  </div>
                </div>
                
                <div class="ai-timestamp">
                  <small>AI分析时间：{{ aiDiagnosisTimestamp }}</small>
                </div>
              </div>
            </div>

            <!-- 护士复核操作 -->
            <div class="nurse-review">
              <h4>✍️ 护士复核</h4>
              <el-form :model="reviewForm" ref="reviewFormRef" label-width="auto">
                <el-form-item label="初步分诊等级">
                  <span class="current-level">
                    边缘端: <span :class="['level-tag', `level-${selectedPatient?.triageLevel}`]">
                      {{ getTriageLevelText(selectedPatient?.triageLevel) }}
                    </span>
                  </span>
                </el-form-item>
                
                <el-form-item label="护士修正等级">
                  <el-radio-group v-model="reviewForm.triageLevel" size="small">
                    <el-radio :label="1" border><span class="level-1">Ⅰ级-急危</span></el-radio>
                    <el-radio :label="2" border><span class="level-2">Ⅱ级-急重</span></el-radio>
                    <el-radio :label="3" border><span class="level-3">Ⅲ级-急症</span></el-radio>
                    <el-radio :label="4" border><span class="level-4">Ⅳ级-亚急症</span></el-radio>
                  </el-radio-group>
                  <div class="level-info-hint">
                    <small style="color: #909399;">
                      Ⅰ级(红)-即刻/复苏区 | Ⅱ级(橙)-10min/抢救区 | Ⅲ级(黄)-30min/优先区 | Ⅳ级(绿)-60min-2h/普通区
                    </small>
                  </div>
                  <div class="level-change-hint" v-if="reviewForm.triageLevel !== selectedPatient?.triageLevel">
                    <el-alert
                      :title="`正在修正分诊等级：${getTriageLevelText(selectedPatient?.triageLevel)} → ${getTriageLevelText(reviewForm.triageLevel)}`"
                      type="warning"
                      :closable="false"
                      show-icon
                    ></el-alert>
                  </div>
                </el-form-item>

                <el-form-item label="建议科室">
                  <el-select v-model="reviewForm.department" placeholder="选择科室" size="small">
                    <el-option label="急诊科" value="急诊科"></el-option>
                    <el-option label="心内科" value="心内科"></el-option>
                    <el-option label="呼吸科" value="呼吸科"></el-option>
                    <el-option label="消化科" value="消化科"></el-option>
                    <el-option label="神经科" value="神经科"></el-option>
                  </el-select>
                </el-form-item>
                
                <el-form-item label="护士备注">
                  <el-input
                    v-model="reviewForm.nurseNotes"
                    type="textarea"
                    :rows="2"
                    placeholder="请记录护士观察和备注..."
                  ></el-input>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="submitReview" :loading="submitting" size="small">
                    确认复核
                  </el-button>
                  <el-button type="warning" @click="requestReassessment" size="small">
                    申请重新评估
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：边缘设备监控 -->
      <div class="device-monitor">
        <div class="monitor-header">
          <h3>📱 边缘设备监控</h3>
          <el-button size="small" @click="refreshDevices">刷新</el-button>
        </div>
        
        <div class="device-list">
          <!-- 没有设备时显示提示 -->
          <div v-if="edgeDevices.length === 0" class="no-devices">
            <i class="el-icon-connection"></i>
            <p>等待边缘设备接入...</p>
            <span class="hint">请确保边缘设备已启动并连接到系统</span>
          </div>
          
          <!-- 边缘设备列表 -->
          <div 
            v-for="device in edgeDevices" 
            :key="device.id"
            :class="['device-card', device.status.toLowerCase()]"
          >
            <div class="device-header">
              <div class="device-info">
                <span class="device-name">{{ device.name }}</span>
                <span :class="['status-badge', device.status.toLowerCase()]">
                  {{ device.status === 'ONLINE' ? '在线' : device.status === 'OFFLINE' ? '离线' : '故障' }}
                </span>
              </div>
              <div class="device-time">
                {{ formatTime(device.lastUpdate) }}
              </div>
            </div>
            
            <div class="device-metrics">
              <div class="metric">
                <span class="metric-label">处理患者</span>
                <span class="metric-value">{{ device.processedCount }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">数据质量</span>
                <span class="metric-value">{{ device.dataQuality }}%</span>
              </div>
              <div class="metric">
                <span class="metric-label">响应时间</span>
                <span class="metric-value">{{ device.responseTime }}ms</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分诊等级调整对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="🔧 分诊等级调整" width="50%">
      <div class="adjust-form">
        <div class="current-assessment">
          <h4>当前AI评估</h4>
          <p><strong>等级：</strong>{{ getTriageLevelText(adjustingPatient?.triageLevel) }}</p>
          <p><strong>置信度：</strong>{{ Math.round((adjustingPatient?.triageScore || 0) * 100) }}%</p>
          <p><strong>理由：</strong>{{ adjustingPatient?.aiDiagnosis }}</p>
        </div>
        
        <el-form :model="adjustForm">
          <el-form-item label="调整后等级">
            <el-radio-group v-model="adjustForm.newLevel">
              <el-radio :label="1">Ⅰ级-急危(红)</el-radio>
              <el-radio :label="2">Ⅱ级-急重(橙)</el-radio>
              <el-radio :label="3">Ⅲ级-急症(黄)</el-radio>
              <el-radio :label="4">Ⅳ级-亚急症(绿)</el-radio>
            </el-radio-group>
            <div style="margin-top: 10px;">
              <small style="color: #909399;">
                Ⅰ级-即刻处理(复苏区/抢救区) | Ⅱ级-10分钟内(抢救区) | Ⅲ级-30分钟内(优先诊疗区) | Ⅳ级-60分钟-2小时(普通诊疗区)
              </small>
            </div>
          </el-form-item>
          
          <el-form-item label="调整原因">
            <el-input
              v-model="adjustForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请说明调整分诊等级的具体原因..."
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjustment">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 个人信息对话框 -->
    <el-dialog v-model="profileDialogVisible" title="👩‍⚕️ 个人信息" width="600px">
      <el-form :model="profileForm" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.fullName" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="profileForm.department" placeholder="请选择科室" style="width: 100%">
            <el-option label="急诊科" value="急诊科"></el-option>
            <el-option label="门诊部" value="门诊部"></el-option>
            <el-option label="ICU" value="ICU"></el-option>
            <el-option label="内科" value="内科"></el-option>
            <el-option label="外科" value="外科"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="profileForm.title" placeholder="请选择职称" style="width: 100%">
            <el-option label="护士" value="护士"></el-option>
            <el-option label="护师" value="护师"></el-option>
            <el-option label="主管护师" value="主管护师"></el-option>
            <el-option label="副主任护师" value="副主任护师"></el-option>
            <el-option label="主任护师" value="主任护师"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="执业证号">
          <el-input v-model="profileForm.licenseNumber" placeholder="请输入执业证号"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="changePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置对话框 -->
    <el-dialog v-model="settingsDialogVisible" title="⚙️ 系统设置" width="600px">
      <el-form :model="settingsForm" label-width="120px">
        <el-divider content-position="left">通知设置</el-divider>
        <el-form-item label="消息通知">
          <el-switch v-model="settingsForm.notifications"></el-switch>
          <span class="setting-hint">接收系统通知消息</span>
        </el-form-item>
        <el-form-item label="声音提醒">
          <el-switch v-model="settingsForm.soundAlerts"></el-switch>
          <span class="setting-hint">紧急患者声音提醒</span>
        </el-form-item>
        
        <el-divider content-position="left">界面设置</el-divider>
        <el-form-item label="自动刷新">
          <el-switch v-model="settingsForm.autoRefresh"></el-switch>
          <span class="setting-hint">自动刷新患者队列</span>
        </el-form-item>
        <el-form-item label="刷新间隔" v-if="settingsForm.autoRefresh">
          <el-slider v-model="settingsForm.refreshInterval" :min="10" :max="120" :step="10" show-stops></el-slider>
          <span class="setting-hint">{{ settingsForm.refreshInterval }}秒</span>
        </el-form-item>
        <el-form-item label="主题">
          <el-radio-group v-model="settingsForm.theme">
            <el-radio label="light">浅色</el-radio>
            <el-radio label="dark">深色</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="settingsForm.language" placeholder="请选择语言" style="width: 200px">
            <el-option label="简体中文" value="zh-CN"></el-option>
            <el-option label="English" value="en-US"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重新评估申请对话框 -->
    <el-dialog v-model="reassessmentDialogVisible" title="🔄 申请重新评估" width="500px">
      <div class="reassessment-form">
        <el-alert
          title="重新评估将通知边缘设备重新采集数据"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />
        
        <el-form :model="reassessmentForm" label-width="100px">
          <el-form-item label="评估类型">
            <el-radio-group v-model="reassessmentForm.reassessType">
              <el-radio label="VITAL_SIGNS">重新采集生命体征</el-radio>
              <el-radio label="VOICE">重新语音输入</el-radio>
              <el-radio label="AI">重新AI分析</el-radio>
              <el-radio label="ALL">全部重新</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="紧急程度">
            <el-radio-group v-model="reassessmentForm.urgency">
              <el-radio label="normal">普通</el-radio>
              <el-radio label="urgent">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="申请原因" required>
            <el-input
              v-model="reassessmentForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请说明需要重新评估的原因，例如：生命体征数据异常、语音识别不清晰等..."
            ></el-input>
          </el-form-item>
        </el-form>
        
        <div v-if="selectedPatient" class="patient-info-summary">
          <h4>当前患者信息</h4>
          <p><strong>姓名：</strong>{{ selectedPatient.patientName || selectedPatient.patient?.patientName }}</p>
          <p><strong>当前分诊等级：</strong>{{ getTriageLevelText(selectedPatient.triageLevel) }}</p>
          <p><strong>边缘设备：</strong>{{ selectedPatient.edgeDeviceId || '未知' }}</p>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="reassessmentDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmReassessment" :loading="reassessmentSubmitting">
          确认申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'
import { getNurseStats, getTriageQueue, submitTriage, submitTriageReview } from '@/api/nurse'

const router = useRouter()
const userStore = useUserStore()
const currentUser = ref('李护士')

// WebSocket连接
const { connect, disconnect, subscribe, connected } = useWebSocket()

// 数据状态
const selectedPatient = ref(null)
const priorityFilter = ref('')
const submitting = ref(false)
const loading = ref(false)
const adjustDialogVisible = ref(false)
const adjustingPatient = ref(null)
const aiExpanded = ref(false)
const profileDialogVisible = ref(false)
const settingsDialogVisible = ref(false)
const reassessmentDialogVisible = ref(false)
const reassessmentSubmitting = ref(false)

// AI诊断相关状态
const aiDiagnosisLoading = ref(false)
const aiDiagnosisResult = ref(null)
const aiDiagnosisTimestamp = ref('')

// 编辑模式状态
const editMode = ref(false)
const editForm = reactive({
  chiefComplaint: '',
  vitalSigns: {
    temperature: 36.5,
    heartRate: 75,
    systolicBP: 120,
    diastolicBP: 80,
    bloodOxygen: 98
  }
})

// 个人信息表单
const profileForm = reactive({
  fullName: userStore.userInfo?.fullName || '',
  username: userStore.userInfo?.username || '',
  email: userStore.userInfo?.email || '',
  phone: '',
  department: '急诊科',
  title: '主管护师',
  licenseNumber: ''
})

// 系统设置表单
const settingsForm = reactive({
  notifications: true,
  soundAlerts: true,
  autoRefresh: true,
  refreshInterval: 30,
  theme: 'light',
  language: 'zh-CN'
})

// 统计数据 - 从后端API获取
const stats = reactive({
  newArrivals: 0,
  pendingTriage: 0,
  confirmedToday: 0,
  avgResponseTime: '0分钟'
})

// 边缘设备数据 - 从后端API获取
const edgeDevices = ref([])

// 患者队列数据 - 从后端API获取
const patients = ref([])

// 表单数据
const reviewForm = reactive({
  triageLevel: 1,
  nurseNotes: '',
  department: ''
})

const adjustForm = reactive({
  newLevel: 1,
  reason: ''
})

// 重新评估表单
const reassessmentForm = reactive({
  reassessType: 'VITAL_SIGNS',  // VITAL_SIGNS-重新采集生命体征, VOICE-重新语音输入, AI-重新AI分析, ALL-全部重新
  reason: '',
  urgency: 'normal'  // normal-普通, urgent-紧急
})

// 计算属性
const newPatientsCount = computed(() => stats.newArrivals)

const filteredPatients = computed(() => {
  if (!priorityFilter.value) return patients.value
  return patients.value.filter(p => p.triageLevel.toString() === priorityFilter.value)
})

// 方法
const refreshData = async () => {
  try {
    loading.value = true
    ElMessage.info('正在刷新数据...')
    await loadInitialData()
    ElMessage.success(`数据已刷新，当前有 ${patients.value.length} 位患者待复核`)
  } catch (error) {
    console.error('刷新失败:', error)
    ElMessage.error('刷新失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const refreshDevices = async () => {
  try {
    await loadEdgeDevices()
    ElMessage.success('设备状态已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

const selectPatient = (patient) => {
  selectedPatient.value = patient
  aiExpanded.value = false  // 重置展开状态
  // 重置AI诊断状态
  aiDiagnosisResult.value = null
  aiDiagnosisLoading.value = false
  aiDiagnosisTimestamp.value = ''
  
  // 重置编辑模式
  editMode.value = false
  
  // 初始化编辑表单数据
  editForm.chiefComplaint = patient.chiefComplaint || ''
  
  // 解析生命体征，确保是数字类型
  const temp = getVitalSign(patient.vitalSigns, 'temperature')
  const hr = getVitalSign(patient.vitalSigns, 'heartRate')
  const sbp = getVitalSign(patient.vitalSigns, 'systolicBP')
  const dbp = getVitalSign(patient.vitalSigns, 'diastolicBP')
  const spo2 = getVitalSign(patient.vitalSigns, 'bloodOxygen')
  
  editForm.vitalSigns = {
    temperature: temp && temp !== '--' ? parseFloat(temp) : 36.5,
    heartRate: hr && hr !== '--' ? parseInt(hr) : 75,
    systolicBP: sbp && sbp !== '--' ? parseInt(sbp) : 120,
    diastolicBP: dbp && dbp !== '--' ? parseInt(dbp) : 80,
    bloodOxygen: spo2 && spo2 !== '--' ? parseInt(spo2) : 98
  }
  
  console.log('初始化编辑表单:', editForm.vitalSigns)
  
  reviewForm.triageLevel = patient.triageLevel
  reviewForm.department = patient.assignedDepartment || ''
  reviewForm.nurseNotes = ''
}

const toggleAIExpand = () => {
  aiExpanded.value = !aiExpanded.value
}

// 切换编辑模式
const toggleEditMode = async () => {
  if (editMode.value) {
    // 保存模式 - 提交修改
    try {
      await savePatientEdits()
      editMode.value = false
      ElMessage.success('患者信息修改成功')
    } catch (error) {
      ElMessage.error('保存失败：' + error.message)
    }
  } else {
    // 开启编辑模式
    editMode.value = true
    ElMessage.info('已进入编辑模式，可修改患者主诉和生理数据')
  }
}

// 请求AI诊断
const requestAIDiagnosis = async () => {
  if (!selectedPatient.value) {
    ElMessage.warning('请先选择一个患者')
    return
  }
  
  if (!selectedPatient.value.chiefComplaint) {
    ElMessage.warning('患者主诉信息不完整，无法进行AI分析')
    return
  }
  
  try {
    aiDiagnosisLoading.value = true
    aiDiagnosisResult.value = null
    
    // 构建请求数据
    const requestData = {
      symptoms: selectedPatient.value.chiefComplaint,
      medicalHistory: selectedPatient.value.medicalHistory || '',
      vitalSigns: {
        temperature: getVitalSign(selectedPatient.value.vitalSigns, 'temperature'),
        heartRate: getVitalSign(selectedPatient.value.vitalSigns, 'heartRate'),
        systolicBP: getVitalSign(selectedPatient.value.vitalSigns, 'systolicBP'),
        diastolicBP: getVitalSign(selectedPatient.value.vitalSigns, 'diastolicBP'),
        bloodOxygen: getVitalSign(selectedPatient.value.vitalSigns, 'bloodOxygen')
      }
    }
    
    // 调用AI诊断接口
    const response = await fetch('/api/ai/rag-diagnosis', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(requestData)
    })
    
    if (!response.ok) {
      throw new Error('AI诊断服务异常')
    }
    
    const result = await response.json()
    
    if (result.success) {
      aiDiagnosisResult.value = result.data
      aiDiagnosisTimestamp.value = new Date().toLocaleString('zh-CN')
      ElMessage.success('AI诊断完成')
    } else {
      throw new Error(result.message || 'AI诊断失败')
    }
    
  } catch (error) {
    console.error('AI诊断错误:', error)
    ElMessage.error(`AI诊断失败: ${error.message}`)
  } finally {
    aiDiagnosisLoading.value = false
  }
}

// 保存患者编辑信息
const savePatientEdits = async () => {
  if (!selectedPatient.value) {
    throw new Error('未选择患者')
  }
  
  const updateData = {
    patientId: selectedPatient.value.id,
    chiefComplaint: editForm.chiefComplaint,
    vitalSigns: {
      temperature: editForm.vitalSigns.temperature,
      heartRate: editForm.vitalSigns.heartRate,
      systolicBP: editForm.vitalSigns.systolicBP,
      diastolicBP: editForm.vitalSigns.diastolicBP,
      bloodOxygen: editForm.vitalSigns.bloodOxygen
    },
    nurseEdited: true,
    editedBy: currentUser.value,
    editTime: new Date().toISOString()
  }
  
  const response = await fetch('/api/nurse/update-patient-info', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${userStore.token}`
    },
    body: JSON.stringify(updateData)
  })
  
  if (!response.ok) {
    throw new Error('保存失败')
  }
  
  // 更新本地患者信息
  selectedPatient.value.chiefComplaint = editForm.chiefComplaint
  selectedPatient.value.vitalSigns = JSON.stringify(editForm.vitalSigns)
  
  // 重置AI诊断结果（因为数据变化了）
  aiDiagnosisResult.value = null
  aiDiagnosisTimestamp.value = ''
}

const getTriageLevelText = (level) => {
  // 严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)
  // 四级分诊标准：Ⅰ级-急危、Ⅱ级-急重、Ⅲ级-急症、Ⅳ级-亚急症/非急症
  const levels = {
    1: 'Ⅰ级-急危',    // 红色 - 即刻
    2: 'Ⅱ级-急重',    // 橙色 - 10分钟内
    3: 'Ⅲ级-急症',    // 黄色 - 30分钟内
    4: 'Ⅳ级-亚急症'   // 绿色 - 60分钟-2小时
  }
  return levels[level] || '未分诊'
}

const getVitalSign = (vitalSigns, key) => {
  try {
    // 处理双重转义的JSON字符串
    let signsStr = vitalSigns || '{}'
    // 如果外层是字符串（带引号），先解析一次
    if (typeof signsStr === 'string' && signsStr.startsWith('"')) {
      signsStr = JSON.parse(signsStr)
    }
    // 再解析实际的JSON对象
    const signs = JSON.parse(signsStr)
    return signs[key] || '--'
  } catch (error) {
    console.warn('解析生命体征失败:', error, vitalSigns)
    return '--'
  }
}

const formatTime = (time) => {
  return new Date(time).toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

const formatDateTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

const getWaitingTime = (arrivalTime) => {
  const diff = Date.now() - new Date(arrivalTime).getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  return `${minutes}分钟`
}

const getConfidenceColor = (score) => {
  if (score > 0.8) return '#67c23a'
  if (score > 0.6) return '#e6a23c'
  return '#f56c6c'
}

const confirmTriage = async (patient, level) => {
  try {
    console.log('开始确认分诊:', patient)
    
    const result = await ElMessageBox.confirm(
      `确认将患者 ${patient.patientName || patient.patient?.patientName || '待登记'} 的分诊等级设为 ${getTriageLevelText(level)} 吗？`,
      '确认分诊',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 获取patientId - 兼容多种数据来源
    const patientId = patient.patientId || patient.patient?.id
    
    if (!patientId) {
      console.error('无法获取patientId:', patient)
      ElMessage.error('患者ID不存在，无法提交')
      return
    }
    
    console.log('准备提交确认:', { patientId, level })
    
    // 调用真实API提交分诊确认 - 包含AI诊断结果和生命体征
    const { data } = await submitTriageReview({
      patientId,
      triageRecordId: patient.id,  // 分诊记录ID
      confirmedLevel: level,
      nurseNotes: '护士确认分诊',
      // 传递AI诊断结果到医生端
      aiDiagnosis: aiDiagnosisResult.value?.diagnosis || aiDiagnosisResult.value?.analysis || '',
      aiConfidence: aiDiagnosisResult.value?.confidence || 0,
      recommendedDepartment: aiDiagnosisResult.value?.recommendedDepartment || '',
      // 传递生命体征数据
      vitalSigns: patient.vitalSigns || patient.sensorData || null
    })
    
    console.log('确认分诊响应:', data)
    
    if (data.success) {
      // 移除已确认的患者
      const index = patients.value.findIndex(p => p.id === patient.id)
      if (index > -1) {
        patients.value.splice(index, 1)
      }
      stats.confirmedToday++
      ElMessage.success('分诊确认成功')
    } else {
      ElMessage.error(data.message || '确认失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认分诊失败:', error)
      console.error('错误详情:', error.response || error.message)
      ElMessage.error('提交失败: ' + (error.response?.data?.message || error.message || '请检查网络连接'))
    }
  }
}

const adjustTriage = (patient) => {
  adjustingPatient.value = patient
  adjustForm.newLevel = patient.triageLevel
  adjustForm.reason = ''
  adjustDialogVisible.value = true
}

const confirmAdjustment = async () => {
  if (!adjustForm.reason.trim()) {
    ElMessage.warning('请输入调整原因')
    return
  }
  
  try {
    // 调用真实API提交调整
    const { data } = await submitTriageReview({
      triageRecordId: adjustingPatient.value.id,
      confirmedLevel: adjustForm.newLevel,
      nurseNotes: `调整原因: ${adjustForm.reason}`
    })
    
    if (data.success) {
      adjustingPatient.value.triageLevel = adjustForm.newLevel
      adjustDialogVisible.value = false
      ElMessage.success('分诊等级调整成功')
    } else {
      ElMessage.error(data.message || '调整失败')
    }
  } catch (error) {
    console.error('调整失败:', error)
    ElMessage.error('调整失败')
  }
}

const viewDetails = (patient) => {
  selectPatient(patient)
  ElMessage.info('已选中患者，查看右侧详情面板')
}

const submitReview = async () => {
  if (!selectedPatient.value) {
    ElMessage.warning('请选择患者')
    return
  }
  
  try {
    submitting.value = true
    
    // 添加调试日志
    console.log('准备提交复核，selectedPatient:', selectedPatient.value)
    console.log('患者ID:', selectedPatient.value.patient?.id)
    console.log('确认级别:', reviewForm.triageLevel)
    console.log('护士备注:', reviewForm.nurseNotes)
    
    // 调用真实API提交分诊复核
    const response = await submitTriageReview({
      patientId: selectedPatient.value.patient?.id,
      triageRecordId: selectedPatient.value.id,  // 分诊记录ID
      confirmedLevel: reviewForm.triageLevel,
      nurseNotes: reviewForm.nurseNotes
    })
    
    console.log('API完整响应:', response)
    console.log('response.data:', response.data)
    
    // 处理多种响应格式
    const isSuccess = 
      response.data === '分诊确认成功' || 
      response.data?.success === true ||
      response.success === true ||
      (typeof response.data === 'object' && response.data?.message?.includes('成功'))
    
    if (isSuccess) {
      ElMessage.success('复核提交成功')
      
      // 移除已复核的患者
      const index = patients.value.findIndex(p => p.id === selectedPatient.value.id)
      if (index > -1) {
        patients.value.splice(index, 1)
      }
      
      selectedPatient.value = null
      stats.confirmedToday++
      
      // 重置表单
      reviewForm.nurseNotes = ''
      reviewForm.triageLevel = 1
    } else {
      const errorMsg = response.data?.message || response.message || '提交失败'
      ElMessage.error(errorMsg)
    }
  } catch (error) {
    console.error('提交复核失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const requestReassessment = () => {
  if (!selectedPatient.value) {
    ElMessage.warning('请先选择一个患者')
    return
  }
  // 重置表单
  reassessmentForm.reassessType = 'VITAL_SIGNS'
  reassessmentForm.reason = ''
  reassessmentForm.urgency = 'normal'
  reassessmentDialogVisible.value = true
}

// 确认提交重新评估申请
const confirmReassessment = async () => {
  if (!reassessmentForm.reason.trim()) {
    ElMessage.warning('请填写重新评估的原因')
    return
  }
  
  try {
    reassessmentSubmitting.value = true
    
    // 获取边缘设备ID
    const deviceId = selectedPatient.value.edgeDeviceId || 'jetson-orin-nano-01'
    
    // 调用简化的重新评估API
    const requestData = {
      patientId: selectedPatient.value.patientId || selectedPatient.value.id,
      patientName: selectedPatient.value.patientName || selectedPatient.value.patient?.patientName,
      deviceId: deviceId,
      reassessType: reassessmentForm.reassessType,
      reason: reassessmentForm.reason,
      urgency: reassessmentForm.urgency,
      nurseName: currentUser.value
    }
    
    const response = await fetch('/api/nurse/request-reassessment', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(requestData)
    })
    
    const result = await response.json()
    
    if (result.success) {
      ElMessage.success('重新评估申请已发送到边缘设备')
      reassessmentDialogVisible.value = false
      
      // 更新患者状态为"待重新评估"
      const patientIndex = patients.value.findIndex(p => p.id === selectedPatient.value.id)
      if (patientIndex > -1) {
        patients.value[patientIndex].status = 'REASSESSING'
        patients.value[patientIndex].reassessReason = reassessmentForm.reason
      }
    } else {
      ElMessage.error(result.message || '提交失败')
    }
    
  } catch (error) {
    console.error('重新评估申请失败:', error)
    ElMessage.error('提交失败: ' + error.message)
  } finally {
    reassessmentSubmitting.value = false
  }
}

const addToWaitlist = () => {
  ElMessage.success('已加入等待队列')
}

// 保存个人信息
const saveProfile = () => {
  ElMessage.success('个人信息已保存')
  profileDialogVisible.value = false
}

// 保存设置
const saveSettings = () => {
  // 保存到 localStorage
  localStorage.setItem('userSettings', JSON.stringify(settingsForm))
  ElMessage.success('设置已保存')
  settingsDialogVisible.value = false
}

// 修改密码
const changePassword = () => {
  ElMessageBox.prompt('请输入新密码', '修改密码', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /.{6,}/,
    inputErrorMessage: '密码至少需要6位'
  }).then(({ value }) => {
    ElMessage.success('密码修改成功，请重新登录')
  }).catch(() => {
    // 用户取消
  })
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      profileDialogVisible.value = true
      break
    case 'settings':
      settingsDialogVisible.value = true
      break
    case 'logout':
      // 退出登录：清除用户信息
      userStore.logout()
      ElMessage.success('退出登录成功')
      router.push('/login')
      break
  }
}

onMounted(async () => {
  console.log('护士工作台开始加载')
  
  // 初始化WebSocket连接
  connect()
  
  // 使用轮询等待WebSocket连接成功后再订阅主题
  const waitForConnection = () => {
    return new Promise((resolve) => {
      const checkConnection = () => {
        if (connected.value) {
          resolve(true)
        } else {
          setTimeout(checkConnection, 200)
        }
      }
      // 最多等待10秒
      setTimeout(() => resolve(false), 10000)
      checkConnection()
    })
  }
  
  waitForConnection().then((isConnected) => {
    console.log('WebSocket连接状态:', isConnected)
    
    if (!isConnected) {
      console.warn('WebSocket连接超时，尝试重新连接...')
      connect()
    }
    
    // 订阅护士端消息 - 边缘端实时分诊数据
    subscribe('/topic/edge-triage', (message) => {
    console.log('收到边缘端分诊数据:', message)
    
    // 新患者到达 - 边缘端采集并分诊后推送
    if (message.type === 'NEW_PATIENT' || message.type === 'EDGE_TRIAGE' || message.deviceId) {
      // 解析生命体征数据
      let vitalSignsData = message.vitalSigns || {}
      if (typeof vitalSignsData === 'string') {
        try {
          vitalSignsData = JSON.parse(vitalSignsData)
        } catch (e) {
          vitalSignsData = {}
        }
      }
      
      // 合并生命体征数据（同时支持嵌套和平铺格式）
      const vitals = {
        temperature: message.temperature || vitalSignsData.temperature || '--',
        heartRate: message.heartRate || vitalSignsData.heartRate || '--',
        bloodOxygen: message.bloodOxygen || vitalSignsData.bloodOxygen || '--',
        systolicBP: message.systolicBP || vitalSignsData.systolicBP || '--',
        diastolicBP: message.diastolicBP || vitalSignsData.diastolicBP || '--'
      }
      
      // 构建新患者数据
      const newPatient = {
        id: message.triageRecordId || message.id,
        patientId: message.patientId || message.patient?.id,
        
        // 患者信息 - 从border-端传入
        patient: {
          id: message.patientId || message.patient?.id,  // 重要！确保患者ID被设置
          patientName: message.patientName || message.patient?.patientName || '待登记',
          age: message.patientAge || message.patient?.patientAge,
          gender: message.patientGender || message.patient?.patientGender,
          idCard: message.patient?.idCard || '',
          phone: message.patient?.phone || ''
        },
        patientName: message.patientName || message.patient?.patientName || '待登记',
        
        // 分诊结果
        triageLevel: message.triageLevel,
        triageScore: message.triageScore || message.confidence,
        triagePriority: message.triagePriority,
        triageColor: message.triageColor,
        waitTime: message.waitTime,
        
        // 主诉/症状 - 语音识别结果
        chiefComplaint: message.chiefComplaint || message.voiceText || message.symptomText || '',
        
        // 生命体征 - JSON字符串格式
        vitalSigns: JSON.stringify(vitals),
        
        // 时间信息
        arrivalTime: message.arrivalTime || message.timestamp || new Date(),
        
        // 边缘设备ID - 重要！护士需要看到数据来源
        edgeDeviceId: message.deviceId || message.edgeDeviceId,
        
        // AI诊断结果
        aiDiagnosis: message.diagnosis || message.aiAnalysis,
        aiConfidence: message.confidence,
        
        // 处理信息
        edgeProcessingTime: message.edgeProcessingTime,
        dataQuality: message.dataQuality,
        source: message.source || 'edge-device'
      }
      
      console.log('解析后的新患者数据:', newPatient)
      
      // 添加到患者队列最前面
      patients.value.unshift(newPatient)
      stats.newArrivals++
      stats.pendingTriage++
      
      // 显示通知
      const levelText = getTriageLevelText(newPatient.triageLevel)
      ElMessage({
        message: `新患者到达: ${newPatient.patientName} - ${levelText} [设备:${newPatient.edgeDeviceId}]`,
        type: newPatient.triageLevel <= 2 ? 'error' : 'info',
        duration: 5000
      })
      
      console.log(`✅ 新患者已添加到队列 - 姓名:${newPatient.patientName}, 主诉:${newPatient.chiefComplaint}, 设备:${newPatient.edgeDeviceId}`)
    }
  })
  
  // 订阅分诊队列更新
  subscribe('/topic/nurse-triage', (message) => {
    console.log('收到分诊通知:', message)
    
    if (message.type === 'TRIAGE_UPDATED') {
      // 更新患者信息
      const index = patients.value.findIndex(p => p.id === message.triage.id)
      if (index > -1) {
        patients.value[index] = { ...patients.value[index], ...message.triage }
        ElMessage.success('分诊信息已更新')
      }
    }
  })
  
  // 订阅边缘设备状态 - 实时监控Jetson设备
  subscribe('/topic/device-status', (message) => {
    console.log('边缘设备状态通知:', message)
    
    if (message.type === 'DEVICE_STATUS' || message.deviceId) {
      const deviceId = message.deviceId
      const index = edgeDevices.value.findIndex(d => d.id === deviceId)
      
      if (index > -1) {
        // 更新设备状态
        edgeDevices.value[index] = {
          ...edgeDevices.value[index],
          status: message.status || message.deviceStatus,
          lastUpdate: new Date(message.timestamp || Date.now()),
          processedCount: message.processedCount || edgeDevices.value[index].processedCount,
          dataQuality: message.dataQuality || message.quality,
          responseTime: message.responseTime || message.latency
        }
      } else {
        // 新设备上线
        edgeDevices.value.push({
          id: deviceId,
          name: message.deviceName || `边缘设备-${deviceId}`,
          status: message.status || 'ONLINE',
          lastUpdate: new Date(message.timestamp || Date.now()),
          processedCount: message.processedCount || 0,
          dataQuality: message.dataQuality || 95,
          responseTime: message.responseTime || 0
        })
      }
    }
  })
  
  // 订阅紧急患者警报
  subscribe('/topic/urgent-patient', (message) => {
    console.log('紧急患者警报:', message)
    
    ElMessage({
      message: `紧急！${message.patientName || '患者'} - ${message.diagnosis || '极高危分诊'}`,
      type: 'error',
      duration: 10000,
      showClose: true
    })
    
    // 播放警报音效（已禁用，避免404错误）
    // if (window.Audio) {
    //   const audio = new Audio('/alert.mp3')
    //   audio.play().catch(e => console.log('无法播放警报音效'))
    // }
  })
  
  }) // waitForConnection.then 结束
  
  // 异步加载初始数据，不阻塞页面渲染
  loadInitialData().catch(err => {
    console.error('加载初始数据失败:', err)
  })
  
  loadEdgeDevices().catch(err => {
    console.error('加载边缘设备失败:', err)
  })
  
  console.log('护士工作台加载完成')
})

const loadInitialData = async () => {
  try {
    console.log('开始加载护士工作台数据...')
    
    // 设置超时控制，防止无限等待
    const timeout = (ms) => new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Request timeout')), ms)
    )
    
    try {
      // 获取统计数据，5秒超时
      const statsRes = await Promise.race([
        getNurseStats(),
        timeout(5000)
      ])
      console.log('统计数据响应:', statsRes)
      if (statsRes && statsRes.success) {
        Object.assign(stats, statsRes.data)
      } else if (statsRes && statsRes.data && statsRes.data.success) {
        Object.assign(stats, statsRes.data.data)
      }
    } catch (error) {
      console.warn('获取统计数据失败:', error.message)
      // 使用默认值，不阻塞页面
    }
    
    try {
      // 获取分诊队列，5秒超时
      const queueRes = await Promise.race([
        getTriageQueue(),
        timeout(5000)
      ])
      console.log('分诊队列响应:', queueRes)
      if (queueRes && queueRes.success) {
        patients.value = queueRes.data || []
      } else if (queueRes && queueRes.data && queueRes.data.success) {
        patients.value = queueRes.data.data || []
      }
    } catch (error) {
      console.warn('获取分诊队列失败:', error.message)
      // 使用空数组，不阻塞页面
      patients.value = []
    }
    
    console.log('护士工作台数据加载完成')
  } catch (error) {
    console.error('加载初始数据失败:', error)
    ElMessage.warning('部分数据加载失败，请稍后刷新')
  }
}

// 加载边缘设备数据
const loadEdgeDevices = async () => {
  try {
    console.log('开始加载边缘设备...')
    
    // 设置超时控制
    const timeout = (ms) => new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Request timeout')), ms)
    )
    
    try {
      // 调用边缘设备API，5秒超时
      const response = await Promise.race([
        fetch('/api/edge/devices/status', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
          }
        }),
        timeout(5000)
      ])
      
      if (response.ok) {
        const result = await response.json()
        console.log('边缘设备响应:', result)
        
        if (result.success && result.data && result.data.length > 0) {
          // 使用后端API返回的真实数据
          edgeDevices.value = result.data.map(device => ({
            id: device.deviceId,
            name: `边缘设备-${device.deviceId}`,
            status: device.online ? 'ONLINE' : 'OFFLINE',
            lastUpdate: new Date(device.lastHeartbeat || Date.now()),
            processedCount: device.todayDataCount || 0,
            dataQuality: 95, // 可从后端数据质量分数获取
            responseTime: 200 // 可根据实际情况计算
          }))
          console.log(`✅ 加载了 ${edgeDevices.value.length} 个真实边缘设备`)
        } else {
          console.log('⚠️ 后端API没有返回设备数据，等待真实设备接入')
          edgeDevices.value = []
        }
      } else {
        console.warn('边缘设备API响应失败:', response.status)
        edgeDevices.value = []
      }
    } catch (error) {
      console.warn('加载边缘设备失败:', error.message)
      // 不使用模拟数据，等待真实设备接入
      edgeDevices.value = []
    }
    
    console.log('边缘设备加载完成')
  } catch (error) {
    console.error('加载边缘设备数据失败:', error)
    edgeDevices.value = []
  }
}
</script>

<style lang="scss" scoped>
.nurse-dashboard {
  min-height: 100vh;
  min-width: 1200px; /* 最小宽度，保证布局不会压缩变形 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-x: auto; /* 窗口过小时允许水平滚动 */
  
  .dashboard-header {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 12px 24px; /* 缩小内边距 */
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 4px 20px rgba(0,0,0,0.08);
    border-bottom: 2px solid rgba(102, 126, 234, 0.1);
    
    .header-left {
      h1 {
        margin: 0 0 4px 0;
        color: #2c3e50;
        font-size: 22px; /* 缩小字体 */
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
      
      p {
        margin: 0;
        color: #7f8c8d;
        font-size: 12px; /* 缩小字体 */
        font-weight: 500;
      }
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 24px;
      
      .real-time-status {
        .el-button {
          border-radius: 24px;
          padding: 10px 24px;
          font-weight: 600;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
          transition: all 0.3s ease;
          
          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
          }
        }
      }
      
      .user-info {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 10px 16px;
        border-radius: 24px;
        transition: all 0.3s ease;
        background: rgba(102, 126, 234, 0.05);
        
        &:hover {
          background: rgba(102, 126, 234, 0.1);
          transform: translateY(-2px);
        }
        
        span {
          font-weight: 600;
          color: #2c3e50;
        }
      }
    }
  }
  
  .main-workspace {
    display: grid;
    grid-template-columns: 260px 1fr 280px; /* 固定左右宽度，中间自适应 */
    gap: 16px;
    padding: 16px;
    height: calc(100vh - 90px); /* 固定高度 */
    width: 100%;
    box-sizing: border-box;
    overflow: hidden; /* 禁止整体滚动 */
    
    .device-monitor {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 8px 32px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
      
      .monitor-header {
        padding: 12px 16px; /* 缩小内边距 */
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
        border-bottom: 1px solid rgba(102, 126, 234, 0.1);
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-shrink: 0;
        
        h3 {
          margin: 0;
          color: #2c3e50;
          font-size: 14px; /* 缩小字体 */
          font-weight: 700;
        }
        
        .el-button {
          border-radius: 16px;
          font-weight: 600;
          font-size: 12px;
          padding: 6px 12px;
        }
      }
      
      .device-list {
        flex: 1;
        padding: 10px;
        overflow-y: auto;
        
        /* 滚动条样式 */
        &::-webkit-scrollbar {
          width: 4px;
        }
        
        &::-webkit-scrollbar-track {
          background: rgba(0,0,0,0.05);
          border-radius: 2px;
        }
        
        &::-webkit-scrollbar-thumb {
          background: rgba(102, 126, 234, 0.3);
          border-radius: 2px;
          
          &:hover {
            background: rgba(102, 126, 234, 0.5);
          }
        }
        
        /* 空设备提示 */
        .no-devices {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          height: 100%;
          padding: 20px;
          text-align: center;
          color: #95a5a6;
          
          i {
            font-size: 36px;
            margin-bottom: 12px;
            color: #bdc3c7;
          }
          
          p {
            margin: 0 0 6px 0;
            font-size: 13px;
            font-weight: 600;
            color: #7f8c8d;
          }
          
          .hint {
            font-size: 11px;
            color: #95a5a6;
            line-height: 1.4;
          }
        }
        
        .device-card {
          margin-bottom: 8px;
          padding: 10px;
          border-radius: 8px;
          border: 1px solid transparent;
          transition: all 0.2s ease;
          
          &.online {
            background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(103, 194, 58, 0.05) 100%);
            border-left: 3px solid #67c23a;
          }
          
          &.offline {
            background: linear-gradient(135deg, rgba(144, 147, 153, 0.1) 0%, rgba(144, 147, 153, 0.05) 100%);
            border-left: 3px solid #909399;
          }
          
          &.error {
            background: linear-gradient(135deg, rgba(245, 108, 108, 0.1) 0%, rgba(245, 108, 108, 0.05) 100%);
            border-left: 3px solid #f56c6c;
          }
          
          .device-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            
            .device-info {
              display: flex;
              align-items: center;
              gap: 6px;
              
              .device-name {
                font-weight: 700;
                color: #2c3e50;
                font-size: 12px;
              }
              
              .status-badge {
                padding: 2px 6px;
                border-radius: 8px;
                font-size: 10px;
                color: white;
                font-weight: 600;
                
                &.online { background: #67c23a; }
                &.offline { background: #909399; }
                &.error { background: #f56c6c; }
              }
            }
            
            .device-time {
              font-size: 10px;
              color: #95a5a6;
              font-weight: 500;
            }
          }
          
          .device-metrics {
            display: flex;
            justify-content: space-between;
            gap: 4px;
            
            .metric {
              text-align: center;
              flex: 1;
              padding: 4px;
              background: rgba(255, 255, 255, 0.5);
              border-radius: 6px;
              
              .metric-label {
                display: block;
                font-size: 9px;
                color: #7f8c8d;
                margin-bottom: 2px;
                font-weight: 600;
              }
              
              .metric-value {
                font-size: 12px;
                font-weight: 700;
                color: #2c3e50;
              }
            }
          }
        }
      }
    }
    
    .triage-queue {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 8px 32px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
      
      .queue-header {
        padding: 12px 16px; /* 缩小内边距 */
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
        border-bottom: 1px solid rgba(102, 126, 234, 0.1);
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-shrink: 0;
        
        h3 {
          margin: 0;
          color: #2c3e50;
          font-size: 14px; /* 缩小字体 */
          font-weight: 700;
        }
        
        .queue-controls {
          .el-select {
            width: 100px; /* 缩小宽度 */
          }
        }
      }
      
      .patient-queue {
        flex: 1;
        overflow-y: auto;
        padding: 8px;
        
        /* 滚动条样式 */
        &::-webkit-scrollbar {
          width: 4px;
        }
        
        &::-webkit-scrollbar-track {
          background: rgba(0,0,0,0.05);
          border-radius: 2px;
        }
        
        &::-webkit-scrollbar-thumb {
          background: rgba(102, 126, 234, 0.3);
          border-radius: 2px;
          
          &:hover {
            background: rgba(102, 126, 234, 0.5);
          }
        }
        
        .patient-item {
          padding: 10px; /* 缩小内边距 */
          margin-bottom: 8px;
          border-radius: 10px;
          cursor: pointer;
          transition: all 0.2s ease;
          background: rgba(255, 255, 255, 0.6);
          border: 1px solid transparent;
          
          &:hover {
            background: rgba(255, 255, 255, 0.9);
            transform: translateX(2px);
            box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
          }
          
          &.selected {
            background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 126, 234, 0.1) 100%);
            border-color: rgba(64, 158, 255, 0.3);
            box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
            transform: translateX(4px);
          }
          
          .patient-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 8px;
            
            .patient-basic {
              display: flex;
              flex-direction: column;
              gap: 4px;
              
              .patient-name {
                font-weight: 700;
                color: #2c3e50;
                font-size: 14px; /* 缩小字体 */
              }
              
              .ai-badge {
                padding: 2px 8px; /* 缩小内边距 */
                border-radius: 10px;
                font-size: 11px;
                color: white;
                align-self: flex-start;
                font-weight: 600;
                box-shadow: 0 2px 6px rgba(0,0,0,0.15);
                
                &.level-1 { 
                  background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%);
                }
                &.level-2 { 
                  background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%);
                }
                &.level-3 { 
                  background: linear-gradient(135deg, #ffeb3b 0%, #f9a825 100%);
                  color: #333;
                }
                &.level-4 { 
                  background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
                }
              }
            }
            
            .patient-timing {
              text-align: right;
              font-size: 11px; /* 缩小字体 */
              color: #7f8c8d;
              
              .arrival-time {
                display: block;
                margin-bottom: 2px;
                font-weight: 500;
              }
              
              .waiting-time {
                color: #e67e22;
                font-weight: 700;
                background: rgba(230, 126, 34, 0.1);
                padding: 1px 6px;
                border-radius: 8px;
              }
            }
          }
          
          .patient-data {
            margin-bottom: 0;
            
            .vital-signs-mini {
              display: flex;
              gap: 3px;
              margin-bottom: 6px;
              flex-wrap: nowrap;
              overflow-x: auto;
              
              &::-webkit-scrollbar {
                height: 3px;
              }
              
              &::-webkit-scrollbar-thumb {
                background: rgba(102, 126, 234, 0.3);
                border-radius: 2px;
              }
              
              .vital {
                font-size: 10px;
                color: #2c3e50;
                background: rgba(102, 126, 234, 0.1);
                padding: 2px 5px;
                border-radius: 8px;
                font-weight: 600;
                white-space: nowrap;
                flex-shrink: 0;
              }
            }
            
            .chief-complaint {
              font-size: 11px;
              color: #34495e;
              line-height: 1.4;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              
              strong {
                color: #2c3e50;
                font-weight: 700;
              }
            }
          }
        }
      }
    }
    
    .patient-details {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);
      border-radius: 12px;
      padding: 12px; /* 缩小内边距 */
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 8px 32px rgba(0,0,0,0.1);
      
      .no-selection {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .empty-state {
          text-align: center;
          color: #7f8c8d;
          
          i {
            font-size: 60px;
            margin-bottom: 16px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
          }
          
          h3 {
            margin: 0 0 8px 0;
            color: #2c3e50;
            font-size: 18px;
            font-weight: 700;
          }
          
          p {
            color: #95a5a6;
            font-size: 13px;
            font-weight: 500;
          }
        }
      }
      
      .patient-detail-panel {
        height: 100%;
        display: flex;
        flex-direction: column;
        gap: 10px;
        overflow: hidden;  /* 禁止滚动，内容完整显示 */
        
        /* 上半部分：患者信息 + 生命体征 */
        .upper-section {
          display: grid;
          grid-template-columns: 1fr 1fr; /* 等宽 */
          gap: 10px;
          flex: 0 0 auto; /* 不拉伸不压缩 */
        }
        
        /* 下半部分：AI分析 + 护士复核 */
        .lower-section {
          display: grid;
          grid-template-columns: 1fr 1fr; /* 等宽，与上半部分一致 */
          gap: 10px;
          flex: 1; /* 填充剩余空间 */
          min-height: 0; /* 允许压缩 */
        }
        
        .info-section, .vitals-section, .ai-analysis, .nurse-review {
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
          padding: 12px; /* 缩小内边距 */
          border-radius: 10px;
          border: 1px solid rgba(102, 126, 234, 0.1);
          transition: all 0.3s ease;
          
          &:hover {
            border-color: rgba(102, 126, 234, 0.2);
            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
          }
          
          h4 {
            margin: 0 0 8px 0;
            color: #2c3e50;
            font-size: 14px; /* 缩小字体 */
            font-weight: 700;
            padding-bottom: 6px;
            border-bottom: 1px solid rgba(102, 126, 234, 0.2);
          }
        }
        
        .info-grid {
          display: flex;
          flex-direction: column;
          gap: 4px; /* 缩小间距 */
          
          .info-item {
            display: flex;
            font-size: 12px;
            
            label {
              min-width: 60px; /* 缩小标签宽度 */
              color: #666;
            }
            
            span {
              color: #333;
              
              &.waiting-highlight {
                color: #e6a23c;
                font-weight: bold;
              }
            }
          }
        }
        
        .vitals-chart {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 8px; /* 缩小间距 */
          
          .vital-detail {
            text-align: center;
            padding: 10px; /* 缩小内边距 */
            background: rgba(255, 255, 255, 0.8);
            border-radius: 8px;
            border: 1px solid rgba(102, 126, 234, 0.1);
            transition: all 0.3s ease;
            
            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
              border-color: rgba(102, 126, 234, 0.3);
            }
            
            .vital-label {
              font-size: 11px; /* 缩小字体 */
              color: #7f8c8d;
              margin-bottom: 4px;
              font-weight: 600;
            }
            
            .vital-value {
              font-size: 16px; /* 缩小字体 */
              font-weight: 700;
              color: #2c3e50;
              margin-bottom: 4px;
            }
            
            .vital-status {
              font-size: 10px; /* 缩小字体 */
              padding: 2px 6px;
              border-radius: 8px;
              display: inline-block;
              font-weight: 600;
              
              &.normal {
                background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
                color: white;
                box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
              }
              
              &.abnormal {
                background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%);
                color: white;
                box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
              }
              
              &.low {
                background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%);
                color: white;
                box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
              }
            }
          }
        }
        
        .ai-analysis {
          overflow: hidden;
          display: flex;
          flex-direction: column;
          
          .ai-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px; /* 缩小间距 */
            
            h4 {
              margin: 0;
              font-size: 13px;
              font-weight: 700;
              color: #2c3e50;
            }
            
            .el-button {
              border-radius: 6px;
              font-size: 11px;
              padding: 6px 10px;
              
              .el-icon-cpu {
                margin-right: 4px;
              }
            }
          }
          
          .no-diagnosis {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            
            .empty-ai-state {
              text-align: center;
              padding: 16px 12px; /* 缩小内边距 */
              background: rgba(248, 249, 250, 0.8);
              border-radius: 10px;
              border: 1px dashed #e1e5e9;
              
              .el-icon-warning-outline {
                font-size: 24px; /* 缩小图标 */
                color: #909399;
                margin-bottom: 8px;
              }
              
              p {
                font-size: 12px;
                color: #606266;
                margin: 6px 0;
                font-weight: 600;
              }
              
              .hint {
                font-size: 11px;
                color: #909399;
                line-height: 1.3;
              }
            }
          }
          
          .ai-loading {
            flex: 1;
            padding: 12px;
            background: rgba(255, 255, 255, 0.6);
            border-radius: 10px;
            border: 1px solid rgba(102, 126, 234, 0.1);
            
            .loading-text {
              text-align: center;
              margin-top: 10px;
              font-size: 12px;
              color: #606266;
              font-weight: 600;
            }
          }
          
          .ai-result {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 8px;
            overflow-y: auto;
            max-height: none; /* 移除最大高度限制 */
            
            /* 滚动条样式 */
            &::-webkit-scrollbar {
              width: 4px;
            }
            
            &::-webkit-scrollbar-track {
              background: rgba(0,0,0,0.05);
              border-radius: 2px;
            }
            
            &::-webkit-scrollbar-thumb {
              background: rgba(102, 126, 234, 0.3);
              border-radius: 2px;
            }
          }
          
          .ai-diagnosis {
            flex: 1;
            overflow-y: auto;
            padding: 10px;
            background: rgba(255, 255, 255, 0.8);
            border-radius: 10px;
            border: 1px solid rgba(102, 126, 234, 0.1);
            
            /* 细滚动条 */
            &::-webkit-scrollbar {
              width: 4px;
            }
            
            &::-webkit-scrollbar-track {
              background: rgba(0,0,0,0.05);
              border-radius: 2px;
            }
            
            &::-webkit-scrollbar-thumb {
              background: rgba(102, 126, 234, 0.3);
              border-radius: 2px;
            }
            
            strong {
              font-size: 12px;
              color: #2c3e50;
              font-weight: 700;
            }
            
            .ai-text {
              margin: 6px 0 0 0;
              color: #34495e;
              font-size: 12px;
              line-height: 1.6;
              white-space: pre-wrap;
              word-wrap: break-word;
            }
          }
          
          .ai-recommendations {
            padding: 8px 10px;
            background: rgba(240, 248, 255, 0.8);
            border-radius: 10px;
            border: 1px solid rgba(64, 158, 255, 0.1);
            
            .recommendation-item {
              display: flex;
              align-items: center;
              gap: 6px;
              margin-bottom: 6px;
              
              &:last-child {
                margin-bottom: 0;
              }
              
              strong {
                font-size: 11px;
                color: #2c3e50;
                font-weight: 600;
                min-width: 60px;
              }
              
              .department-tag {
                background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
                color: white;
                padding: 3px 8px;
                border-radius: 6px;
                font-size: 11px;
                font-weight: 600;
                box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
              }
              
              .urgency-tag {
                padding: 4px 10px;
                border-radius: 8px;
                font-size: 12px;
                font-weight: 600;
                
                &.高, &.极高 {
                  background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%);
                  color: white;
                  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
                }
                
                &.中 {
                  background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%);
                  color: white;
                  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
                }
                
                &.低 {
                  background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
                  color: white;
                  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
                }
              }
            }
          }
          
          .ai-timestamp {
            text-align: right;
            padding: 8px 12px;
            
            small {
              font-size: 11px;
              color: #909399;
            }
          }
          
          .confidence-meter {
            flex-shrink: 0;
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 12px 15px;
            background: rgba(255, 255, 255, 0.8);
            border-radius: 12px;
            border: 1px solid rgba(102, 126, 234, 0.1);
            
            label {
              font-size: 13px;
              color: #2c3e50;
              white-space: nowrap;
              font-weight: 600;
            }
            
            .el-progress {
              flex: 1;
            }
            
            .confidence-text {
              font-size: 14px;
              font-weight: 700;
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
              min-width: 40px;
              text-align: right;
            }
          }
        }
        
        .nurse-review {
          display: flex;
          flex-direction: column;
          overflow: hidden;
          
          h4 {
            margin: 0 0 8px 0;
            font-size: 14px;
            font-weight: 700;
            color: #2c3e50;
            flex-shrink: 0;
          }
          
          :deep(.el-form) {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
            
            .el-form-item {
              margin-bottom: 8px;
              flex-shrink: 0;
              
              /* 护士备注填充剩余空间 */
              &:nth-child(4) {
                flex: 1;
                display: flex;
                flex-direction: column;
                min-height: 0;
                
                .el-form-item__content {
                  flex: 1;
                  display: flex;
                  flex-direction: column;
                  
                  .el-textarea {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    
                    .el-textarea__inner {
                      flex: 1;
                      resize: none;
                      min-height: 60px;
                    }
                  }
                }
              }
              
              &:last-child {
                margin-top: auto;
                margin-bottom: 0;
              }
              
              .el-form-item__label {
                font-size: 12px;
              }
            }
          }
          
          .el-radio-group {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 6px;
            
            .el-radio {
              margin: 0;
              font-size: 11px;
              border-radius: 8px;
              padding: 6px 8px;
              
              .level-1 { 
                color: #f56c6c;
                font-weight: 700;
              }
              .level-2 { 
                color: #e6a23c;
                font-weight: 700;
              }
              .level-3 { 
                color: #333;
                font-weight: 700;
              }
              .level-4 { 
                color: #67c23a;
                font-weight: 700;
              }
            }
          }
          
          .el-button {
            border-radius: 8px;
            font-weight: 600;
            font-size: 12px;
            padding: 8px 12px;
          }
        }
      }
    }
  }
  
  .adjust-form {
    .current-assessment {
      padding: 15px;
      background: #f8f9fa;
      border-radius: 8px;
      margin-bottom: 20px;
      border-left: 4px solid #409eff;
      
      h4 {
        margin: 0 0 10px 0;
        color: #333;
      }
      
      p {
        margin: 5px 0;
        color: #666;
      }
    }
  }
  
  // 重新评估表单样式
  .reassessment-form {
    .patient-info-summary {
      padding: 15px;
      background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
      border-radius: 8px;
      margin-top: 20px;
      border-left: 4px solid #e6a23c;
      
      h4 {
        margin: 0 0 12px 0;
        color: #333;
        font-size: 14px;
      }
      
      p {
        margin: 6px 0;
        color: #666;
        font-size: 13px;
        
        strong {
          color: #333;
        }
      }
    }
    
    .el-radio-group {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
  }
  
  // 设置提示样式
  .setting-hint {
    margin-left: 12px;
    font-size: 13px;
    color: #95a5a6;
  }
  
  /* 新增编辑模式样式 */
  .vital-edit-area {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 24px;
    
    .vital-input {
      width: 80px;
      
      .el-input__inner {
        text-align: center;
        padding: 4px 8px;
        font-size: 12px;
        border-radius: 6px;
      }
    }
    
    .vital-value {
      font-size: 18px;
      font-weight: 700;
      color: #2c3e50;
    }
    
    .bp-inputs {
      display: flex;
      align-items: center;
      gap: 6px;
      
      .bp-input {
        width: 60px;
      }
      
      .bp-separator {
        font-size: 16px;
        font-weight: 700;
        color: #666;
      }
    }
  }
  
  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 700;
      color: #2c3e50;
    }
    
    .el-button {
      border: none;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 12px;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
      }
    }
  }
  
  .editable-field {
    .edit-textarea {
      width: 100%;
      margin-top: 6px;
      
      .el-textarea__inner {
        border-radius: 8px;
        border: 2px solid #e1e5e9;
        transition: all 0.3s ease;
        
        &:focus {
          border-color: #409eff;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
        }
      }
    }
    
    .chief-complaint-text {
      color: #34495e;
      line-height: 1.6;
      font-size: 13px;
    }
  }
  
  .current-level {
    font-size: 13px;
    color: #606266;
    
    .level-tag {
      padding: 4px 12px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 12px;
      margin-left: 8px;
      
      &.level-1 {
        background: linear-gradient(135deg, #f56c6c 0%, #c0392b 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
      }
      
      &.level-2 {
        background: linear-gradient(135deg, #e6a23c 0%, #d68910 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
      }
      
      &.level-3 {
        background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
      }
      
      &.level-4 {
        background: linear-gradient(135deg, #67c23a 0%, #27ae60 100%);
        color: white;
        box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
      }
    }
  }
  
  .level-change-hint {
    margin-top: 10px;
    
    .el-alert {
      border-radius: 8px;
    }
  }
}
</style>