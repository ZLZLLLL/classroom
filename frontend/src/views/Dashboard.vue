<template>
  <div class="dashboard-home">
    <div class="welcome-section">
      <h1>欢迎回来，{{ authStore.user?.realName || '用户' }}！</h1>
      <p class="subtitle">{{ currentDate }} · {{ roleText }}</p>
    </div>

    <div class="stats-grid" v-if="!authStore.isAdmin" v-loading="loadingUserStats">
      <div v-if="authStore.isStudent" class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);">
          <el-icon :size="28"><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.courseCount }}</span>
          <span class="stat-label">课程数量</span>
        </div>
      </div>

      <div v-if="authStore.isStudent" class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);">
          <el-icon :size="28"><Check /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.attendanceRate }}%</span>
          <span class="stat-label">出勤率</span>
        </div>
      </div>

      <div v-if="authStore.isStudent" class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);">
          <el-icon :size="28"><QuestionFilled /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.questionCount }}</span>
          <span class="stat-label">回答问题</span>
        </div>
      </div>

      <div v-if="authStore.isStudent" class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%);">
          <el-icon :size="28"><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.points }}</span>
          <span class="stat-label">当前积分</span>
        </div>
      </div>

      <div v-else-if="authStore.isTeacher" class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%);">
          <el-icon :size="28"><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.courseCount }}</span>
          <span class="stat-label">课程数量</span>
        </div>
      </div>
    </div>

    <div class="stats-grid" v-else v-loading="loadingAdminStats">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);">
          <el-icon :size="28"><User /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ adminStats.totalRegistered }}</span>
          <span class="stat-label">注册人数</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);">
          <el-icon :size="28"><School /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ adminStats.teacherCount }}</span>
          <span class="stat-label">教师人数</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);">
          <el-icon :size="28"><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ adminStats.studentCount }}</span>
          <span class="stat-label">学生人数</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%);">
          <el-icon :size="28"><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value stat-small">{{ adminStats.uptime }}</span>
          <span class="stat-label">系统运行时长</span>
        </div>
      </div>
    </div>

    <el-card v-if="authStore.isAdmin" shadow="never" class="admin-version-card">
      <span>系统版本：{{ adminStats.version || '1.0.0' }}</span>
    </el-card>

    <!-- 快捷操作 -->
    <div class="quick-actions" v-if="!authStore.isAdmin">
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

    <div class="announcement-publisher" v-if="authStore.isAdmin">
      <h2>发布系统公告</h2>
      <el-card shadow="never">
        <el-form label-position="top">
          <el-form-item label="公告标题">
            <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
          </el-form-item>
          <el-form-item label="公告内容">
            <el-input v-model="announcementForm.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
          </el-form-item>
          <el-button type="primary" :loading="publishingAnnouncement" @click="handlePublishAnnouncement">发布公告</el-button>
        </el-form>
      </el-card>
    </div>

    <!-- 最近活动 -->
    <div class="recent-activity">
      <h2>最近活动</h2>
      <el-card shadow="never">
        <div v-if="announcements.length > 0" class="activity-list">
          <div v-for="item in announcements" :key="item.id" class="activity-item">
            <div class="activity-title">{{ item.title }}</div>
            <div class="activity-content">{{ item.content }}</div>
            <div class="activity-meta">
              <span>发布人：{{ item.publisherName || '管理员' }}</span>
              <span>{{ new Date(item.createTime).toLocaleString() }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无最近活动" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import {
  Reading,
  Check,
  QuestionFilled,
  Document,
  TrendCharts,
  ChatDotRound,
  Plus,
  User,
  School,
  Clock
} from '@element-plus/icons-vue'
import { getAdminDashboardStats, publishSystemAnnouncement } from '../api/admin'
import { getRecentAnnouncements, type SystemAnnouncement } from '../api/announcement'
import { getCourseList, getMyCourses } from '../api/course'
import { getMyAttendanceStatistics } from '../api/attendance'
import { getMyPoints } from '../api/points'
import { getCourseQuestions, getTeacherQuestions } from '../api/question'

const router = useRouter()
const authStore = useAuthStore()

const roleText = computed(() => {
  if (authStore.isAdmin) return '管理员'
  return authStore.isTeacher ? '教师' : '学生'
})

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

const loadingUserStats = ref(false)
const loadingAdminStats = ref(false)
const publishingAnnouncement = ref(false)
const announcements = ref<SystemAnnouncement[]>([])
const adminStats = reactive({
  totalRegistered: 0,
  teacherCount: 0,
  studentCount: 0,
  uptime: '-',
  version: '1.0.0'
})
const announcementForm = reactive({
  title: '',
  content: ''
})

const loadAdminStats = async () => {
  if (!authStore.isAdmin) return
  loadingAdminStats.value = true
  try {
    const res = await getAdminDashboardStats()
    adminStats.totalRegistered = res.totalRegistered || 0
    adminStats.teacherCount = res.teacherCount || 0
    adminStats.studentCount = res.studentCount || 0
    adminStats.uptime = res.uptime || '-'
    adminStats.version = res.version || '1.0.0'
  } finally {
    loadingAdminStats.value = false
  }
}

const loadAnnouncements = async () => {
  try {
    announcements.value = await getRecentAnnouncements(10)
  } catch {
    announcements.value = []
  }
}

const handlePublishAnnouncement = async () => {
  if (!announcementForm.title.trim() || !announcementForm.content.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }

  publishingAnnouncement.value = true
  try {
    await publishSystemAnnouncement({
      title: announcementForm.title.trim(),
      content: announcementForm.content.trim()
    })
    announcementForm.title = ''
    announcementForm.content = ''
    ElMessage.success('公告发布成功')
    await loadAnnouncements()
  } finally {
    publishingAnnouncement.value = false
  }
}

const loadUserStats = async () => {
  if (authStore.isAdmin) return
  loadingUserStats.value = true
  try {
    if (authStore.isTeacher) {
      const teacherCourses = await getCourseList({ page: 1, size: 200 })
      const teacherQuestions = await getTeacherQuestions()
      stats.value.courseCount = teacherCourses?.total || teacherCourses?.records?.length || 0
      stats.value.questionCount = teacherQuestions?.length || 0
      stats.value.attendanceRate = 0
      stats.value.points = 0
      return
    }

    const [courses, attendanceSummary, myPoints] = await Promise.all([
      getMyCourses(),
      getMyAttendanceStatistics(),
      getMyPoints()
    ])

    stats.value.courseCount = courses?.length || 0
    stats.value.attendanceRate = attendanceSummary?.rate || 0
    stats.value.points = myPoints || 0

    let answered = 0
    for (const course of (courses || [])) {
      const questions = await getCourseQuestions(course.id)
      answered += (questions || []).filter((q: any) => !!q.myAnswer).length
    }
    stats.value.questionCount = answered
  } catch {
    stats.value = { courseCount: 0, attendanceRate: 0, questionCount: 0, points: 0 }
  } finally {
    loadingUserStats.value = false
  }
}

onMounted(async () => {
  await loadAnnouncements()
  if (authStore.isAdmin) {
    await loadAdminStats()
  } else {
    await loadUserStats()
  }
})
</script>

<style scoped>


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

.stat-small {
  font-size: 16px;
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

.admin-version-card {
  margin-bottom: 24px;
}

.announcement-publisher {
  margin-bottom: 24px;
}

.announcement-publisher h2 {
  font-size: 18px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 16px;
}

.activity-list {
  display: grid;
  gap: 12px;
}

.activity-item {
  padding: 12px;
  border: 1px solid #eee6db;
  border-radius: 10px;
  background: #fffaf5;
}

.activity-title {
  font-weight: 600;
  color: #3d3225;
  margin-bottom: 4px;
}

.activity-content {
  color: #6e5a45;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.activity-meta {
  font-size: 12px;
  color: #9a8368;
  display: flex;
  justify-content: space-between;
}
</style>
