<template>
  <div class="dashboard-home">
    <div class="welcome-section">
      <h1>欢迎回来，{{ authStore.user?.realName || '用户' }}！</h1>
      <p class="subtitle">{{ currentDate }} · {{ authStore.isTeacher ? '教师' : '学生' }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);">
          <el-icon :size="28"><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.courseCount }}</span>
          <span class="stat-label">课程数量</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);">
          <el-icon :size="28"><Check /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.attendanceRate }}%</span>
          <span class="stat-label">出勤率</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);">
          <el-icon :size="28"><QuestionFilled /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.questionCount }}</span>
          <span class="stat-label">回答问题</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%);">
          <el-icon :size="28"><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.points }}</span>
          <span class="stat-label">当前积分</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h2>快捷操作</h2>
      <div class="action-grid">
        <div v-if="authStore.isTeacher" class="action-card" @click="router.push('/courses')">
          <el-icon :size="32"><Plus /></el-icon>
          <span>创建课程</span>
        </div>
        <div class="action-card" @click="router.push('/attendance')">
          <el-icon :size="32"><Check /></el-icon>
          <span>{{ authStore.isTeacher ? '发起签到' : '签到' }}</span>
        </div>
        <div class="action-card" @click="router.push('/questions')">
          <el-icon :size="32"><QuestionFilled /></el-icon>
          <span>课堂问答</span>
        </div>
        <div class="action-card" @click="router.push('/homework')">
          <el-icon :size="32"><Document /></el-icon>
          <span>{{ authStore.isTeacher ? '布置作业' : '提交作业' }}</span>
        </div>
        <div class="action-card" @click="router.push('/ai-assistant')">
          <el-icon :size="32"><ChatDotRound /></el-icon>
          <span>AI 助手</span>
        </div>
      </div>
    </div>

    <!-- 最近活动 -->
    <div class="recent-activity">
      <h2>最近活动</h2>
      <el-card shadow="never">
        <el-empty description="暂无最近活动" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  Reading,
  Check,
  QuestionFilled,
  Document,
  TrendCharts,
  ChatDotRound,
  Plus
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const currentDate = computed(() => {
  const now = new Date()
  const options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }
  return now.toLocaleDateString('zh-CN', options)
})

const stats = ref({
  courseCount: 0,
  attendanceRate: 0,
  questionCount: 0,
  points: 0
})
</script>

<style scoped>
.dashboard-home {
  /* 左对齐 */
}

.welcome-section {
  margin-bottom: 32px;
}

.welcome-section h1 {
  font-size: 28px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 8px;
}

.subtitle {
  color: #8b7355;
  font-size: 14px;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e8e0d5;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(61, 50, 37, 0.08);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #3d3225;
}

.stat-label {
  font-size: 13px;
  color: #8b7355;
  margin-top: 4px;
}

.quick-actions {
  margin-bottom: 32px;
}

.quick-actions h2 {
  font-size: 18px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.action-card {
  background: white;
  border-radius: 14px;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border: 1px solid #e8e0d5;
  transition: all 0.3s ease;
  color: #6d5a45;
}

.action-card:hover {
  background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);
  color: white;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(212, 165, 116, 0.3);
}

.action-card span {
  font-size: 14px;
  font-weight: 500;
}

.recent-activity h2 {
  font-size: 18px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 16px;
}
</style>
