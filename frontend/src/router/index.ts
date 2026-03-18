import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    meta: { title: '控制台' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'courses',
        name: 'Courses',
        component: () => import('../views/Courses.vue'),
        meta: { title: '课程管理' }
      },
      {
        path: 'courses/:id',
        name: 'CourseDetail',
        component: () => import('../views/CourseDetail.vue'),
        meta: { title: '课程详情' }
      },
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('../views/Classes.vue'),
        meta: { title: '班级管理' }
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('../views/Attendance.vue'),
        meta: { title: '签到管理' }
      },
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('../views/Questions.vue'),
        meta: { title: '提问管理' }
      },
      {
        path: 'homework',
        name: 'Homework',
        component: () => import('../views/Homework.vue'),
        meta: { title: '作业管理' }
      },
      {
        path: 'files',
        name: 'Files',
        component: () => import('../views/Files.vue'),
        meta: { title: '文件管理' }
      },
      {
        path: 'ai-assistant',
        name: 'AIAssistant',
        component: () => import('../views/AIAssistant.vue'),
        meta: { title: 'AI助手' }
      },
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('../views/Ranking.vue'),
        meta: { title: '积分排行' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人资料' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/AdminUsers.vue'),
        meta: { title: '用户管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.meta.public !== true) {
    next('/login')
    return
  }

  if (to.path.startsWith('/admin')) {
    const userRaw = localStorage.getItem('user')
    const user = userRaw ? JSON.parse(userRaw) : null
    if (!user || user.role !== 3) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
