<template>
  <div class="layout-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '250px'" class="layout-sidebar">
        <div class="sidebar-header">
          <h3 v-show="!isCollapse">医疗管理系统</h3>
          <h3 v-show="isCollapse">医疗</h3>
        </div>
        
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          background-color="#001529"
          text-color="#fff"
          active-text-color="#1890ff"
          router
        >
          <el-menu-item index="/admin-dashboard/overview">
            <el-icon><DataBoard /></el-icon>
            <span>系统概览</span>
          </el-menu-item>
          
          <el-sub-menu v-if="userStore.isAdmin" index="/admin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            
            <el-menu-item index="/admin/resources">
              <el-icon><Box /></el-icon>
              <span>医疗资源管理</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/logs">
              <el-icon><Document /></el-icon>
              <span>系统日志</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/monitoring">
              <el-icon><Monitor /></el-icon>
              <span>系统监控</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/config">
              <el-icon><Tools /></el-icon>
              <span>系统配置</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/rules">
              <el-icon><List /></el-icon>
              <span>Drools规则管理</span>
            </el-menu-item>
            
            <el-menu-item index="/admin/patients">
              <el-icon><Finished /></el-icon>
              <span>已确诊患者</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      
      <!-- 主体内容 -->
      <el-container>
        <!-- 顶部导航 -->
        <el-header class="layout-header" height="60px">
          <div class="header-left">
            <el-button
              type="text"
              @click="toggleCollapse"
            >
              <el-icon size="18">
                <Expand v-if="isCollapse" />
                <Fold v-else />
              </el-icon>
            </el-button>
            
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo?.username }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 内容区域 -->
        <el-main class="layout-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  DataBoard,
  Setting,
  Box,
  User,
  Document,
  Monitor,
  Tools,
  Expand,
  Fold,
  ArrowDown,
  List,
  Finished
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  return route.meta?.title || '系统概览'
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      userStore.logout()
      ElMessage.success('退出登录成功')
      router.push('/login')
    } catch (error) {
      // 用户取消
    }
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.layout-sidebar {
  transition: width 0.28s;
  
  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #002140;
    color: white;
    font-size: 16px;
    font-weight: bold;
  }
  
  .el-menu {
    border-right: none;
    height: calc(100vh - 60px);
    overflow-y: auto;
  }
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #333;
      
      &:hover {
        color: #409eff;
      }
    }
  }
}

.layout-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>