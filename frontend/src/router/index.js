import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/login'
  },
  // 医生工作台
  {
    path: '/doctor-dashboard',
    name: 'DoctorDashboard',
    component: () => import('@/views/DoctorDashboard.vue'),
    meta: { requiresAuth: true, role: 'DOCTOR', title: '医生工作台' }
  },
  // 护士工作台
  {
    path: '/nurse-dashboard',
    name: 'NurseDashboard',
    component: () => import('@/views/NurseDashboard.vue'),
    meta: { requiresAuth: true, role: 'NURSE', title: '护士工作台' }
  },
  // 管理员工作台
  {
    path: '/admin-dashboard',
    name: 'AdminDashboard',
    component: () => import('@/layout/index.vue'),
    redirect: '/admin-dashboard/overview',
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: 'overview',
        name: 'AdminOverview',
        component: () => import('@/views/dashboard/Overview.vue'),
        meta: { title: '系统概览' }
      }
    ]
  },
  // 兼容原有管理员路由
  {
    path: '/dashboard',
    redirect: '/admin-dashboard'
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/layout/index.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'resources',
        name: 'ResourceManagement',
        component: () => import('@/views/admin/ResourceManagement.vue'),
        meta: { title: '医疗资源管理' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'logs',
        name: 'SystemLogs',
        component: () => import('@/views/admin/SystemLogs.vue'),
        meta: { title: '系统日志' }
      },
      {
        path: 'monitoring',
        name: 'SystemMonitoring',
        component: () => import('@/views/admin/SystemMonitoring.vue'),
        meta: { title: '系统监控' }
      },
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('@/views/admin/SystemConfig.vue'),
        meta: { title: '系统配置' }
      },
      {
        path: 'rules',
        name: 'RuleManagement',
        component: () => import('@/views/admin/RuleManagement.vue'),
        meta: { title: 'Drools规则管理' }
      },
      {
        path: 'patients',
        name: 'DiagnosedPatients',
        component: () => import('@/views/admin/DiagnosedPatients.vue'),
        meta: { title: '已确诊患者' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 初始化用户信息（从 localStorage 恢复）
  if (!userStore.currentRole && userStore.token) {
    userStore.initUser()
  }
  
  console.log('路由守卫检查:', {
    to: to.path,
    from: from.path,
    requiresAuth: to.meta.requiresAuth,
    requiredRole: to.meta.role,
    isLoggedIn: userStore.isLoggedIn,
    currentRole: userStore.currentRole,
    token: userStore.token ? '存在' : '不存在'
  })
  
  // 未登录但需要认证的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    console.log('未登录，跳转到登录页')
    next('/login')
    return
  }
  
  // 已登录但访问登录页
  if (to.path === '/login' && userStore.isLoggedIn) {
    // 根据角色重定向到对应的工作台
    const dashboards = {
      'DOCTOR': '/doctor-dashboard',
      'NURSE': '/nurse-dashboard',
      'ADMIN': '/admin-dashboard'
    }
    const targetRoute = dashboards[userStore.currentRole] || '/admin-dashboard'
    console.log('已登录访问登录页，重定向到:', targetRoute)
    next(targetRoute)
    return
  }
  
  // 检查角色权限
  if (to.meta.role && to.meta.role !== userStore.currentRole) {
    // 如果没有角色信息，跳转到登录页
    if (!userStore.currentRole) {
      console.log('未检测到用户角色，跳转到登录页')
      next('/login')
      return
    }
    
    // 根据实际角色重定向到对应的工作台
    const dashboards = {
      'DOCTOR': '/doctor-dashboard',
      'NURSE': '/nurse-dashboard',
      'ADMIN': '/admin-dashboard'
    }
    const targetRoute = dashboards[userStore.currentRole]
    
    // 防止循环重定向
    if (targetRoute && to.path !== targetRoute) {
      console.log('角色权限不匹配，重定向到:', targetRoute)
      next(targetRoute)
      return
    }
  }
  
  // 检查管理员权限（兼容旧的 requiresAdmin 配置）
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    const dashboards = {
      'DOCTOR': '/doctor-dashboard',
      'NURSE': '/nurse-dashboard',
      'ADMIN': '/admin-dashboard'
    }
    const targetRoute = dashboards[userStore.currentRole] || '/login'
    console.log('管理员权限不足，重定向到:', targetRoute)
    next(targetRoute)
    return
  }
  
  console.log('路由守卫通过，允许访问:', to.path)
  next()
})

export default router