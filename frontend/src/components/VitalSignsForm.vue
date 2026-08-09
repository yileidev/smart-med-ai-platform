<template>
  <div class="vital-signs-form">
    <el-form :model="vitalSigns" :rules="rules" ref="vitalSignsForm" label-width="120px">
      <div class="form-section">
        <h3>📊 生命体征录入</h3>
        <div class="vital-grid">
          <!-- 体温 -->
          <el-form-item label="体温 (°C)" prop="temperature" class="vital-item">
            <el-input-number
              v-model="vitalSigns.temperature"
              :min="30"
              :max="45"
              :precision="1"
              :step="0.1"
              size="large"
              placeholder="36.5"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getTemperatureStatus()">
              {{ getTemperatureLabel() }}
            </div>
          </el-form-item>

          <!-- 收缩压 -->
          <el-form-item label="收缩压 (mmHg)" prop="systolicBP" class="vital-item">
            <el-input-number
              v-model="vitalSigns.systolicBP"
              :min="50"
              :max="250"
              size="large"
              placeholder="120"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getSystolicBPStatus()">
              {{ getSystolicBPLabel() }}
            </div>
          </el-form-item>

          <!-- 舒张压 -->
          <el-form-item label="舒张压 (mmHg)" prop="diastolicBP" class="vital-item">
            <el-input-number
              v-model="vitalSigns.diastolicBP"
              :min="30"
              :max="150"
              size="large"
              placeholder="80"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getDiastolicBPStatus()">
              {{ getDiastolicBPLabel() }}
            </div>
          </el-form-item>

          <!-- 心率 -->
          <el-form-item label="心率 (次/分)" prop="heartRate" class="vital-item">
            <el-input-number
              v-model="vitalSigns.heartRate"
              :min="20"
              :max="200"
              size="large"
              placeholder="80"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getHeartRateStatus()">
              {{ getHeartRateLabel() }}
            </div>
          </el-form-item>

          <!-- 呼吸频率 -->
          <el-form-item label="呼吸 (次/分)" prop="respiratoryRate" class="vital-item">
            <el-input-number
              v-model="vitalSigns.respiratoryRate"
              :min="5"
              :max="50"
              size="large"
              placeholder="18"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getRespiratoryRateStatus()">
              {{ getRespiratoryRateLabel() }}
            </div>
          </el-form-item>

          <!-- 血氧饱和度 -->
          <el-form-item label="血氧饱和度 (%)" prop="bloodOxygen" class="vital-item">
            <el-input-number
              v-model="vitalSigns.bloodOxygen"
              :min="50"
              :max="100"
              size="large"
              placeholder="98"
              @change="updateTriageLevel"
            />
            <div class="vital-status" :class="getBloodOxygenStatus()">
              {{ getBloodOxygenLabel() }}
            </div>
          </el-form-item>
        </div>
      </div>

      <!-- 意识状态 -->
      <el-form-item label="意识状态" prop="consciousness">
        <el-select 
          v-model="vitalSigns.consciousness" 
          placeholder="请选择意识状态"
          size="large"
          style="width: 100%"
          @change="updateTriageLevel"
        >
          <el-option label="清醒" value="清醒" />
          <el-option label="嗜睡" value="嗜睡" />
          <el-option label="烦躁" value="烦躁" />
          <el-option label="昏迷" value="昏迷" />
          <el-option label="休克" value="休克" />
        </el-select>
      </el-form-item>

      <!-- 主诉症状 -->
      <el-form-item label="主诉症状" prop="chiefComplaint">
        <el-input
          v-model="vitalSigns.chiefComplaint"
          type="textarea"
          :rows="3"
          placeholder="请详细描述患者主要症状..."
          @input="updateTriageLevel"
        />
      </el-form-item>

      <!-- AI分诊预测结果 -->
      <div class="triage-prediction" v-if="triagePrediction">
        <h4>🤖 AI分诊预测</h4>
        <div class="prediction-result">
          <div class="triage-level" :class="'level-' + triagePrediction.level">
            <span class="level-badge">{{ formatTriageLevel(triagePrediction.level) }}</span>
            <span class="level-desc">{{ triagePrediction.description }}</span>
          </div>
          <div class="confidence">
            <el-progress 
              :percentage="Math.round(triagePrediction.confidence * 100)" 
              :color="getConfidenceColor(triagePrediction.confidence)"
              :show-text="true"
              :format="() => `置信度: ${Math.round(triagePrediction.confidence * 100)}%`"
            />
          </div>
          <div class="wait-time">
            <i class="el-icon-time"></i>
            <span>{{ triagePrediction.waitTime }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ submitText || '提交分诊' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'VitalSignsForm',
  props: {
    initialData: {
      type: Object,
      default: () => ({})
    },
    submitText: {
      type: String,
      default: '提交分诊'
    }
  },
  data() {
    return {
      submitting: false,
      vitalSigns: {
        temperature: 36.5,
        systolicBP: 120,
        diastolicBP: 80,
        heartRate: 80,
        respiratoryRate: 18,
        bloodOxygen: 98,
        consciousness: '清醒',
        chiefComplaint: ''
      },
      triagePrediction: null,
      rules: {
        temperature: [
          { required: true, message: '请输入体温', trigger: 'blur' },
          { type: 'number', min: 30, max: 45, message: '体温范围应在30-45°C之间', trigger: 'blur' }
        ],
        systolicBP: [
          { required: true, message: '请输入收缩压', trigger: 'blur' },
          { type: 'number', min: 50, max: 250, message: '收缩压范围应在50-250mmHg之间', trigger: 'blur' }
        ],
        diastolicBP: [
          { required: true, message: '请输入舒张压', trigger: 'blur' },
          { type: 'number', min: 30, max: 150, message: '舒张压范围应在30-150mmHg之间', trigger: 'blur' }
        ],
        heartRate: [
          { required: true, message: '请输入心率', trigger: 'blur' },
          { type: 'number', min: 20, max: 200, message: '心率范围应在20-200次/分之间', trigger: 'blur' }
        ],
        respiratoryRate: [
          { required: true, message: '请输入呼吸频率', trigger: 'blur' },
          { type: 'number', min: 5, max: 50, message: '呼吸频率范围应在5-50次/分之间', trigger: 'blur' }
        ],
        bloodOxygen: [
          { required: true, message: '请输入血氧饱和度', trigger: 'blur' },
          { type: 'number', min: 50, max: 100, message: '血氧饱和度范围应在50-100%之间', trigger: 'blur' }
        ],
        consciousness: [
          { required: true, message: '请选择意识状态', trigger: 'change' }
        ],
        chiefComplaint: [
          { required: true, message: '请输入主诉症状', trigger: 'blur' },
          { min: 5, message: '主诉症状至少输入5个字符', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    // 初始化数据
    if (this.initialData && Object.keys(this.initialData).length > 0) {
      this.vitalSigns = { ...this.vitalSigns, ...this.initialData }
    }
    this.updateTriageLevel()
  },
  methods: {
    // 更新分诊预测
    updateTriageLevel() {
      // 模拟AI分诊预测逻辑
      const { temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, bloodOxygen, consciousness, chiefComplaint } = this.vitalSigns
      
      let level = 5
      let confidence = 0.7
      let description = 'V级（蓝色）- 非急症'
      let waitTime = '120分钟内或预约'

      // I级分诊判断
      if (this.isLevel1Criteria()) {
        level = 1
        confidence = 0.95
        description = 'I级（红色）- 濒危'
        waitTime = '立即处理'
      }
      // II级分诊判断
      else if (this.isLevel2Criteria()) {
        level = 2
        confidence = 0.9
        description = 'II级（橙色）- 危急'
        waitTime = '10分钟内'
      }
      // III级分诊判断
      else if (this.isLevel3Criteria()) {
        level = 3
        confidence = 0.85
        description = 'III级（黄色）- 急症'
        waitTime = '30分钟内'
      }
      // IV级分诊判断
      else if (this.isLevel4Criteria()) {
        level = 4
        confidence = 0.8
        description = 'IV级（绿色）- 次急症'
        waitTime = '60分钟内'
      }

      this.triagePrediction = {
        level,
        confidence,
        description,
        waitTime
      }
    },

    // 分诊等级判断方法
    isLevel1Criteria() {
      const { temperature, systolicBP, heartRate, respiratoryRate, bloodOxygen, consciousness, chiefComplaint } = this.vitalSigns
      
      if (temperature < 35.0 || temperature > 40.0) return true
      if (systolicBP < 70 || systolicBP > 200) return true
      if (heartRate < 40 || heartRate > 150) return true
      if (respiratoryRate < 8 || respiratoryRate > 35) return true
      if (bloodOxygen < 85) return true
      if (consciousness === '昏迷' || consciousness === '休克') return true
      
      const complaint = chiefComplaint.toLowerCase()
      if (complaint.includes('心脏骤停') || complaint.includes('呼吸骤停') || 
          complaint.includes('大出血') || complaint.includes('严重外伤') ||
          complaint.includes('中毒') || complaint.includes('窒息')) return true
          
      return false
    },

    isLevel2Criteria() {
      const { temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, bloodOxygen, consciousness, chiefComplaint } = this.vitalSigns
      
      if (temperature >= 38.5 && temperature < 40.0) return true
      if (systolicBP >= 180 || systolicBP <= 90) return true
      if (diastolicBP >= 110) return true
      if (heartRate >= 120 || heartRate <= 50) return true
      if (respiratoryRate >= 25 || respiratoryRate <= 10) return true
      if (bloodOxygen >= 85 && bloodOxygen < 90) return true
      if (consciousness === '嗜睡' || consciousness === '烦躁') return true
      
      const complaint = chiefComplaint.toLowerCase()
      if (complaint.includes('胸痛') || complaint.includes('呼吸困难') ||
          complaint.includes('腹痛') || complaint.includes('头痛') ||
          complaint.includes('意识障碍') || complaint.includes('抽搐')) return true
          
      return false
    },

    isLevel3Criteria() {
      const { temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, bloodOxygen, chiefComplaint } = this.vitalSigns
      
      if (temperature >= 37.5 && temperature < 38.5) return true
      if (systolicBP >= 160 && systolicBP < 180) return true
      if (diastolicBP >= 100 && diastolicBP < 110) return true
      if (heartRate >= 100 && heartRate < 120) return true
      if (respiratoryRate >= 20 && respiratoryRate < 25) return true
      if (bloodOxygen >= 90 && bloodOxygen < 95) return true
      
      const complaint = chiefComplaint.toLowerCase()
      if (complaint.includes('发热') || complaint.includes('呕吐') ||
          complaint.includes('腹泻') || complaint.includes('外伤') ||
          complaint.includes('过敏') || complaint.includes('皮疹')) return true
          
      return false
    },

    isLevel4Criteria() {
      const { temperature, systolicBP, diastolicBP, chiefComplaint } = this.vitalSigns
      
      if (temperature >= 37.0 && temperature < 37.5) return true
      if (systolicBP >= 140 && systolicBP < 160) return true
      if (diastolicBP >= 90 && diastolicBP < 100) return true
      
      const complaint = chiefComplaint.toLowerCase()
      if (complaint.includes('咳嗽') || complaint.includes('感冒') ||
          complaint.includes('头晕') || complaint.includes('乏力') ||
          complaint.includes('关节痛') || complaint.includes('轻微外伤')) return true
          
      return false
    },

    // 生命体征状态判断方法
    getTemperatureStatus() {
      const temp = this.vitalSigns.temperature
      if (temp < 35.0 || temp > 40.0) return 'critical'
      if (temp < 36.0 || temp > 38.5) return 'warning'
      if (temp > 37.5) return 'attention'
      return 'normal'
    },

    getTemperatureLabel() {
      const temp = this.vitalSigns.temperature
      if (temp < 35.0) return '体温过低'
      if (temp > 40.0) return '超高热'
      if (temp > 38.5) return '高热'
      if (temp > 37.5) return '低热'
      if (temp < 36.0) return '体温偏低'
      return '正常'
    },

    getSystolicBPStatus() {
      const sbp = this.vitalSigns.systolicBP
      if (sbp < 70 || sbp > 200) return 'critical'
      if (sbp < 90 || sbp > 180) return 'warning'
      if (sbp > 160) return 'attention'
      return 'normal'
    },

    getSystolicBPLabel() {
      const sbp = this.vitalSigns.systolicBP
      if (sbp < 70) return '严重低血压'
      if (sbp > 200) return '高血压危象'
      if (sbp < 90) return '低血压'
      if (sbp > 180) return '高血压危象'
      if (sbp > 160) return '高血压'
      return '正常'
    },

    getDiastolicBPStatus() {
      const dbp = this.vitalSigns.diastolicBP
      if (dbp > 110) return 'critical'
      if (dbp > 100) return 'warning'
      if (dbp > 90) return 'attention'
      return 'normal'
    },

    getDiastolicBPLabel() {
      const dbp = this.vitalSigns.diastolicBP
      if (dbp > 110) return '高血压危象'
      if (dbp > 100) return '高血压'
      if (dbp > 90) return '血压偏高'
      return '正常'
    },

    getHeartRateStatus() {
      const hr = this.vitalSigns.heartRate
      if (hr < 40 || hr > 150) return 'critical'
      if (hr < 50 || hr > 120) return 'warning'
      if (hr > 100) return 'attention'
      return 'normal'
    },

    getHeartRateLabel() {
      const hr = this.vitalSigns.heartRate
      if (hr < 40) return '严重心动过缓'
      if (hr > 150) return '严重心动过速'
      if (hr < 50) return '心动过缓'
      if (hr > 120) return '心动过速'
      if (hr > 100) return '心率偏快'
      return '正常'
    },

    getRespiratoryRateStatus() {
      const rr = this.vitalSigns.respiratoryRate
      if (rr < 8 || rr > 35) return 'critical'
      if (rr < 10 || rr > 25) return 'warning'
      if (rr > 20) return 'attention'
      return 'normal'
    },

    getRespiratoryRateLabel() {
      const rr = this.vitalSigns.respiratoryRate
      if (rr < 8) return '呼吸抑制'
      if (rr > 35) return '严重呼吸急促'
      if (rr < 10) return '呼吸缓慢'
      if (rr > 25) return '呼吸急促'
      if (rr > 20) return '呼吸偏快'
      return '正常'
    },

    getBloodOxygenStatus() {
      const spo2 = this.vitalSigns.bloodOxygen
      if (spo2 < 85) return 'critical'
      if (spo2 < 90) return 'warning'
      if (spo2 < 95) return 'attention'
      return 'normal'
    },

    getBloodOxygenLabel() {
      const spo2 = this.vitalSigns.bloodOxygen
      if (spo2 < 85) return '严重缺氧'
      if (spo2 < 90) return '中度缺氧'
      if (spo2 < 95) return '轻度缺氧'
      return '正常'
    },

    formatTriageLevel(level) {
      const levels = {
        1: 'I级',
        2: 'II级', 
        3: 'III级',
        4: 'IV级',
        5: 'V级'
      }
      return levels[level] || 'N/A'
    },

    getConfidenceColor(confidence) {
      if (confidence >= 0.9) return '#67C23A'
      if (confidence >= 0.8) return '#E6A23C'
      return '#F56C6C'
    },

    submitForm() {
      this.$refs.vitalSignsForm.validate((valid) => {
        if (valid) {
          this.submitting = true
          
          const formData = {
            ...this.vitalSigns,
            triagePrediction: this.triagePrediction
          }
          
          this.$emit('submit', formData)
          
          setTimeout(() => {
            this.submitting = false
          }, 1000)
        }
      })
    },

    resetForm() {
      this.$refs.vitalSignsForm.resetFields()
      this.triagePrediction = null
    }
  }
}
</script>

<style scoped>
.vital-signs-form {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.form-section h3 {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.vital-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.vital-item {
  position: relative;
}

.vital-status {
  position: absolute;
  right: 0;
  top: 30px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.vital-status.normal {
  background: #f0f9ff;
  color: #1890ff;
}

.vital-status.attention {
  background: #fff7e6;
  color: #fa8c16;
}

.vital-status.warning {
  background: #fff2e8;
  color: #fa541c;
}

.vital-status.critical {
  background: #fff1f0;
  color: #f5222d;
}

.triage-prediction {
  margin: 24px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.triage-prediction h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 16px;
}

.prediction-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.triage-level {
  display: flex;
  align-items: center;
  gap: 12px;
}

.level-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-weight: 600;
  font-size: 14px;
}

.triage-level.level-1 .level-badge {
  background: #ff4d4f;
  color: white;
}

.triage-level.level-2 .level-badge {
  background: #ff7a00;
  color: white;
}

.triage-level.level-3 .level-badge {
  background: #faad14;
  color: white;
}

.triage-level.level-4 .level-badge {
  background: #52c41a;
  color: white;
}

.triage-level.level-5 .level-badge {
  background: #1890ff;
  color: white;
}

.level-desc {
  font-size: 14px;
  color: #666;
}

.wait-time {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

@media (max-width: 768px) {
  .vital-grid {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    justify-content: center;
  }
}
</style>