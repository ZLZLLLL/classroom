<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="32"><Reading /></el-icon>
        </div>
        <span class="logo-text">课堂互动</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info" @click="router.push('/profile')">
          <el-avatar :size="40" :src="authStore.user?.avatar">
            {{ authStore.user?.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="user-detail">
            <span class="username">{{ authStore.user?.realName }}</span>
            <span class="role">{{ authStore.isTeacher ? '教师' : '学生' }}</span>
          </div>
        </div>
        <el-button text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  Reading,
  HomeFilled,
  Reading as CourseIcon,
  User,
  Check,
  QuestionFilled,
  Document,
  Files,
  ChatDotRound,
  TrendCharts,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const teacherNav = [
  { path: '/dashboard', label: '控制台', icon: HomeFilled },
  { path: '/courses', label: '课程管理', icon: CourseIcon },
  { path: '/classes', label: '班级管理', icon: User },
  { path: '/attendance', label: '签到管理', icon: Check },
  { path: '/questions', label: '提问管理', icon: QuestionFilled },
  { path: '/homework', label: '作业管理', icon: Document },
  { path: '/files', label: '文件管理', icon: Files },
  { path: '/ranking', label: '积分排行', icon: TrendCharts },
  { path: '/ai-assistant', label: 'AI助手', icon: ChatDotRound }
]

const studentNav = [
  { path: '/dashboard', label: '控制台', icon: HomeFilled },
  { path: '/courses', label: '我的课程', icon: CourseIcon },
  { path: '/attendance', label: '签到', icon: Check },
  { path: '/questions', label: '课堂问答', icon: QuestionFilled },
  { path: '/homework', label: '我的作业', icon: Document },
  { path: '/files', label: '学习资料', icon: Files },
  { path: '/ranking', label: '积分排行', icon: TrendCharts },
  { path: '/ai-assistant', label: 'AI助手', icon: ChatDotRound }
]

const navItems = computed(() => authStore.isTeacher ? teacherNav : studentNav)

const isActive = (path: string) => {
  if (path === '/dashboard') {
    return route.path === '/dashboard'
  }
  return route.path.startsWith(path)
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f8f6f3;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #4a3f35 0%, #3d342a 100%);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  z-index: 100;
}

.sidebar-header {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #f5f0e8;
  letter-spacing: 1px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.sidebar-nav::-webkit-scrollbar {
  display: none;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  border-radius: 12px;
  margin-bottom: 4px;
  transition: all 0.2s ease;
  font-size: 14px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f5f0e8;
}

.nav-item.active {
  background: linear-gradient(135deg, #d4a574 0%, #c4956a 100%);
  color: #3d342a;
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: background 0.2s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.username {
  color: #f5f0e8;
  font-size: 14px;
  font-weight: 500;
}

.role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.sidebar-footer .el-button {
  color: rgba(255, 255, 255, 0.5);
}

.sidebar-footer .el-button:hover {
  color: #f5f0e8;
  background: rgba(255, 255, 255, 0.08);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  padding: 24px;
  min-height: 100vh;
}
</style>
