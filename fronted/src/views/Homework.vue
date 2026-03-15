<template>
  <div class="homework-page">
    <div class="page-header">
      <h2>{{ authStore.isTeacher ? '作业管理' : '我的作业' }}</h2>
      <p class="desc">{{ authStore.isTeacher ? '查看和管理课程作业' : '查看和提交作业' }}</p>
    </div>

    <!-- 学生端：课程选择和作业列表 -->
    <div v-if="!authStore.isTeacher">
      <el-card shadow="never" class="course-select-card">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadHomeworks" style="width: 300px;">
          <el-option label="全部课程" :value="0" />
          <el-option v-for="course in courseList" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
      </el-card>

      <el-card shadow="never" v-loading="loading" class="homework-list-card">
        <div v-if="homeworks.length > 0" class="homework-list">
          <div v-for="hw in homeworks" :key="hw.id" class="homework-item" @click="viewHomeworkDetail(hw)">
            <div class="homework-header">
              <span class="homework-title">{{ hw.title }}</span>
              <el-tag :type="hw.deadline && new Date(hw.deadline) > new Date() ? 'success' : 'danger'">
                {{ hw.deadline && new Date(hw.deadline) > new Date() ? '进行中' : '已截止' }}
              </el-tag>
            </div>
            <div class="homework-content">{{ hw.content || '暂无内容' }}</div>
            <div class="homework-meta">
              <span>课程: {{ hw.courseName || '课程ID:' + hw.courseId }}</span>
              <span v-if="hw.chapter">章节: {{ hw.chapter }}</span>
              <span>总分: {{ hw.totalPoints }}分</span>
              <span v-if="hw.deadline">截止: {{ formatTime(hw.deadline) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无作业" />
      </el-card>
    </div>

    <!-- 教师端 -->
    <el-card v-else shadow="never" v-loading="loading">
      <div v-if="homeworks.length > 0" class="homework-list">
        <div v-for="hw in homeworks" :key="hw.id" class="homework-item">
          <div class="homework-header">
            <span class="homework-title">{{ hw.title }}</span>
            <el-tag :type="hw.deadline && new Date(hw.deadline) > new Date() ? 'success' : 'danger'">
              {{ hw.deadline && new Date(hw.deadline) > new Date() ? '进行中' : '已截止' }}
            </el-tag>
          </div>
          <div class="homework-content">{{ hw.content || '暂无内容' }}</div>
          <div class="homework-meta">
            <span>课程ID: {{ hw.courseId }}</span>
            <span v-if="hw.chapter">章节: {{ hw.chapter }}</span>
            <span>总分: {{ hw.totalPoints }}分</span>
            <span v-if="hw.deadline">截止: {{ formatTime(hw.deadline) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无作业" />
    </el-card>

    <!-- 作业详情对话框 -->
    <el-dialog v-model="detailVisible" title="作业详情" width="600px">
      <div v-if="currentHomework" class="homework-detail">
        <div class="detail-header">
          <h3>{{ currentHomework.title }}</h3>
          <el-tag :type="currentHomework.deadline && new Date(currentHomework.deadline) > new Date() ? 'success' : 'danger'">
            {{ currentHomework.deadline && new Date(currentHomework.deadline) > new Date() ? '进行中' : '已截止' }}
          </el-tag>
        </div>
        <div class="detail-content">
          <p><strong>作业内容：</strong></p>
          <p>{{ currentHomework.content || '暂无内容' }}</p>
        </div>
        <div class="detail-meta">
          <span>课程: {{ currentHomework.courseName || '课程ID:' + currentHomework.courseId }}</span>
          <span v-if="currentHomework.chapter">章节: {{ currentHomework.chapter }}</span>
          <span>总分: {{ currentHomework.totalPoints }}分</span>
          <span v-if="currentHomework.deadline">截止: {{ formatTime(currentHomework.deadline) }}</span>
        </div>

        <!-- 提交状态和提交按钮 -->
        <el-divider />
        <div class="submit-section">
          <div v-if="currentSubmit">
            <p><strong>我的提交：</strong></p>
            <p>提交内容: {{ currentSubmit.content || '无' }}</p>
            <p>提交时间: {{ formatTime(currentSubmit.submitTime) }}</p>
            <p v-if="currentSubmit.score !== undefined && currentSubmit.score !== null">
              得分: <el-tag type="success">{{ currentSubmit.score }}分</el-tag>
            </p>
            <p v-if="currentSubmit.feedback">教师反馈: {{ currentSubmit.feedback }}</p>
          </div>
          <div v-else-if="currentHomework.deadline && new Date(currentHomework.deadline) > new Date()">
            <el-input v-model="submitContent" type="textarea" :rows="4" placeholder="请输入作业内容" />
            <el-button type="primary" @click="handleSubmit" style="margin-top: 10px;">提交作业</el-button>
          </div>
          <div v-else>
            <el-alert title="已超过截止时间，无法提交" type="warning" :closable="false" />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getTeacherHomeworks, getStudentHomeworks, submitHomework, getMyHomeworkSubmit } from '../api/homework'
import { getCourseById } from '../api/course'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const homeworks = ref<any[]>([])
const courseList = ref<any[]>([])
const selectedCourseId = ref(0)

// 详情对话框
const detailVisible = ref(false)
const currentHomework = ref<any>(null)
const currentSubmit = ref<any>(null)
const submitContent = ref('')

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadHomeworks = async () => {
  loading.value = true
  try {
    if (authStore.isTeacher) {
      const res = await getTeacherHomeworks()
      homeworks.value = res.records || []
    } else {
      const res = await getStudentHomeworks()
      homeworks.value = res.records || []
      // 获取课程名称
      for (const hw of homeworks.value) {
        try {
          const course = await getCourseById(hw.courseId)
          hw.courseName = course?.name || ''
        } catch (e) {
          hw.courseName = ''
        }
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 加载课程列表供学生选择
const loadCourseList = async () => {
  if (!authStore.isTeacher) {
    courseList.value = homeworks.value.reduce((acc: any[], hw) => {
      const exists = acc.find((c: any) => c.id === hw.courseId)
      if (!exists) {
        acc.push({ id: hw.courseId, name: hw.courseName || `课程${hw.courseId}` })
      }
      return acc
    }, [])
  }
}

const viewHomeworkDetail = async (hw: any) => {
  currentHomework.value = hw
  detailVisible.value = true
  submitContent.value = ''

  // 获取提交状态
  try {
    const submit = await getMyHomeworkSubmit(hw.id)
    currentSubmit.value = submit
  } catch (e) {
    currentSubmit.value = null
  }
}

const handleSubmit = async () => {
  if (!submitContent.value.trim()) {
    ElMessage.warning('请输入作业内容')
    return
  }

  try {
    await submitHomework(currentHomework.value.id, submitContent.value)
    ElMessage.success('提交成功')
    // 刷新提交状态
    const submit = await getMyHomeworkSubmit(currentHomework.value.id)
    currentSubmit.value = submit
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
}

onMounted(() => {
  loadHomeworks().then(() => {
    loadCourseList()
  })
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.course-select-card { margin-bottom: 16px; }

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.homework-item {
  padding: 16px;
  background: #faf8f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.homework-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.homework-title {
  font-weight: 600;
  font-size: 16px;
  color: #3d3225;
}

.homework-content {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.homework-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.homework-detail .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.homework-detail .detail-header h3 {
  margin: 0;
  color: #3d3225;
}

.homework-detail .detail-content {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.homework-detail .detail-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #666;
}

.submit-section {
  margin-top: 16px;
}
</style>
