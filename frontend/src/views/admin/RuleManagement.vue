<template>
  <div class="rule-management">
    <el-card class="header-card">
      <div class="page-header">
        <h2>Drools规则引擎管理</h2>
        <p>管理分诊规则、资源调度规则、紧急处理规则</p>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 规则文件列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>规则文件列表</span>
              <el-button type="primary" size="small" @click="refreshRules">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          
          <div class="rule-list" v-loading="loading">
            <div
              v-for="rule in ruleFiles"
              :key="rule.name"
              class="rule-item"
              :class="{ active: selectedRule === rule.name }"
              @click="selectRule(rule.name)"
            >
              <el-icon><Document /></el-icon>
              <div class="rule-info">
                <span class="rule-name">{{ rule.displayName }}</span>
                <span class="rule-desc">{{ rule.description }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 规则编辑区域 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ selectedRuleInfo?.displayName || '请选择规则文件' }}</span>
              <div class="header-actions" v-if="selectedRule">
                <el-button type="success" @click="saveRule" :loading="saving">
                  <el-icon><Check /></el-icon> 保存规则
                </el-button>
                <el-button @click="reloadRule">
                  <el-icon><RefreshRight /></el-icon> 重新加载
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="!selectedRule" class="no-selection">
            <el-empty description="请从左侧选择一个规则文件进行编辑" />
          </div>

          <div v-else class="editor-container">
            <div class="rule-meta">
              <el-tag type="info">{{ selectedRule }}</el-tag>
              <span class="last-modified" v-if="ruleContent">
                修改后请点击"保存规则"生效
              </span>
            </div>
            
            <el-input
              v-model="ruleContent"
              type="textarea"
              :rows="25"
              placeholder="规则内容..."
              class="rule-editor"
              :disabled="loadingContent"
            />

            <div class="rule-help">
              <el-collapse>
                <el-collapse-item title="Drools规则语法帮助" name="help">
                  <div class="help-content">
                    <h4>基本结构：</h4>
                    <pre>rule "规则名称"
    when
        // 条件
        $patient : PatientData(heartRate > 100)
    then
        // 动作
        $patient.setTriageLevel(2);
end</pre>
                    <h4>常用条件：</h4>
                    <ul>
                      <li><code>heartRate > 100</code> - 心率大于100</li>
                      <li><code>bloodOxygen < 95</code> - 血氧低于95%</li>
                      <li><code>temperature > 38.5</code> - 体温超过38.5°C</li>
                      <li><code>symptom contains "胸痛"</code> - 症状包含胸痛</li>
                    </ul>
                    <h4>分诊等级说明：</h4>
                    <ul>
                      <li><code>Level 1</code> - 危急（需立即处理）</li>
                      <li><code>Level 2</code> - 紧急（10分钟内处理）</li>
                      <li><code>Level 3</code> - 较急（30分钟内处理）</li>
                      <li><code>Level 4</code> - 一般（60分钟内处理）</li>
                    </ul>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 规则测试区域 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>规则测试</span>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <h4>测试数据</h4>
          <el-form :model="testData" label-width="100px">
            <el-form-item label="心率">
              <el-input-number v-model="testData.heartRate" :min="40" :max="200" />
              <span class="unit">bpm</span>
            </el-form-item>
            <el-form-item label="血氧">
              <el-input-number v-model="testData.bloodOxygen" :min="70" :max="100" />
              <span class="unit">%</span>
            </el-form-item>
            <el-form-item label="体温">
              <el-input-number v-model="testData.temperature" :min="35" :max="42" :step="0.1" />
              <span class="unit">°C</span>
            </el-form-item>
            <el-form-item label="症状描述">
              <el-input v-model="testData.symptom" placeholder="如：胸痛、呼吸困难" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="testRule" :loading="testing">
                执行规则测试
              </el-button>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <h4>测试结果</h4>
          <div class="test-result" v-if="testResult">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="分诊等级">
                <el-tag :type="getLevelType(testResult.triageLevel)">
                  {{ getLevelText(testResult.triageLevel) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="建议科室">
                {{ testResult.department || '待分配' }}
              </el-descriptions-item>
              <el-descriptions-item label="触发规则">
                {{ testResult.triggeredRules?.join(', ') || '无' }}
              </el-descriptions-item>
              <el-descriptions-item label="处理建议">
                {{ testResult.suggestion || '按常规流程处理' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="点击测试查看结果" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Refresh, Check, RefreshRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const loadingContent = ref(false)
const saving = ref(false)
const testing = ref(false)
const selectedRule = ref('')
const ruleContent = ref('')
const testResult = ref(null)

const ruleFiles = ref([
  { name: 'triage-priority.drl', displayName: '分诊优先级规则', description: '根据生命体征判断分诊等级' },
  { name: 'doctor-assignment.drl', displayName: '医生分配规则', description: '根据患者情况分配医生' },
  { name: 'medical-resource-allocation.drl', displayName: '医疗资源分配规则', description: '医疗资源调度策略' },
  { name: 'resource-allocation.drl', displayName: '资源调度规则', description: '紧急情况资源调度' }
])

const selectedRuleInfo = computed(() => {
  return ruleFiles.value.find(r => r.name === selectedRule.value)
})

const testData = reactive({
  heartRate: 80,
  bloodOxygen: 98,
  temperature: 36.5,
  symptom: ''
})

// 刷新规则列表
const refreshRules = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/drools/rules')
    if (res.data && Array.isArray(res.data)) {
      // 后端返回的是对象数组 [{fileName, description, path}]
      ruleFiles.value = res.data.map(item => {
        const existing = ruleFiles.value.find(r => r.name === item.fileName)
        return {
          name: item.fileName,
          displayName: existing?.displayName || item.fileName.replace('.drl', ''),
          description: item.description || existing?.description || ''
        }
      })
    }
    ElMessage.success('规则列表已刷新')
  } catch (error) {
    console.error('获取规则列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 选择规则文件
const selectRule = async (ruleName) => {
  selectedRule.value = ruleName
  loadingContent.value = true
  
  try {
    const res = await request.get(`/admin/drools/rules/${ruleName}`)
    // 后端返回 {fileName, content, lastModified}
    ruleContent.value = res.data?.content || ''
  } catch (error) {
    ElMessage.error('加载规则内容失败')
    ruleContent.value = ''
  } finally {
    loadingContent.value = false
  }
}

// 保存规则
const saveRule = async () => {
  if (!selectedRule.value || !ruleContent.value) {
    ElMessage.warning('请先选择并编辑规则')
    return
  }
  
  saving.value = true
  try {
    await request.put(`/admin/drools/rules/${selectedRule.value}`, {
      content: ruleContent.value
    })
    ElMessage.success('规则保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

// 重新加载规则
const reloadRule = () => {
  if (selectedRule.value) {
    selectRule(selectedRule.value)
  }
}

// 测试规则
const testRule = async () => {
  testing.value = true
  try {
    const res = await request.post('/admin/drools/test', testData)
    testResult.value = res.data
    ElMessage.success('规则测试完成')
  } catch (error) {
    ElMessage.error('测试失败: ' + (error.response?.data?.message || error.message))
  } finally {
    testing.value = false
  }
}

// 获取等级样式
const getLevelType = (level) => {
  const types = { 1: 'danger', 2: 'warning', 3: 'info', 4: 'success' }
  return types[level] || 'info'
}

// 获取等级文本
const getLevelText = (level) => {
  const texts = { 1: '一级（危急）', 2: '二级（紧急）', 3: '三级（较急）', 4: '四级（一般）' }
  return texts[level] || '未知'
}

// 初始化加载
refreshRules()
</script>

<style lang="scss" scoped>
.rule-management {
  height: calc(100vh - 120px);
  overflow-y: auto;
  padding-bottom: 20px;
  
  .page-header {
    h2 { margin: 0 0 8px; color: #303133; }
    p { margin: 0; color: #909399; font-size: 14px; }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .rule-list {
    max-height: 500px;
    overflow-y: auto;
  }

  .rule-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;

    &:hover { background: #f5f7fa; }
    &.active {
      background: #ecf5ff;
      border-color: #409eff;
    }

    .rule-info {
      display: flex;
      flex-direction: column;
      .rule-name { font-weight: 500; color: #303133; }
      .rule-desc { font-size: 12px; color: #909399; margin-top: 4px; }
    }
  }

  .no-selection {
    padding: 60px 0;
  }

  .editor-container {
    .rule-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      .last-modified { color: #909399; font-size: 12px; }
    }

    .rule-editor {
      :deep(textarea) {
        font-family: 'Consolas', 'Monaco', monospace;
        font-size: 13px;
        line-height: 1.6;
      }
    }

    .rule-help {
      margin-top: 16px;
      .help-content {
        font-size: 13px;
        h4 { margin: 12px 0 8px; color: #303133; }
        pre {
          background: #f5f7fa;
          padding: 12px;
          border-radius: 4px;
          overflow-x: auto;
        }
        ul { padding-left: 20px; }
        code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; }
      }
    }
  }

  .test-result {
    padding: 10px 0;
  }

  .unit {
    margin-left: 8px;
    color: #909399;
  }
}
</style>
