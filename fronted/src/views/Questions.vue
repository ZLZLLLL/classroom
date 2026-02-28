<template>
  <div class="questions-page">
    <div class="page-header">
      <h2>提问管理</h2>
      <p class="desc">课堂互动问答</p>
    </div>

    <el-card shadow="never" v-loading="loading">
      <div v-if="questions.length > 0" class="question-list">
        <div v-for="q in questions" :key="q.id" class="question-item">
          <div class="question-header">
            <span class="course-name">课程ID: {{ q.courseId }}</span>
            <el-tag :type="q.status === 1 ? 'success' : 'info'">
              {{ q.status === 1 ? '进行中' : '已结束' }}
            </el-tag>
          </div>
          <div class="question-content">{{ q.content }}</div>
          <div class="question-meta">
            <span>分值: {{ q.points }}分</span>
            <span>时限: {{ q.duration }}秒</span>
            <span>创建时间: {{ formatTime(q.createTime) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无提问" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getTeacherQuestions } from '../api/question'

const authStore = useAuthStore()
const loading = ref(false)
const questions = ref<any[]>([])

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadQuestions = async () => {
  if (!authStore.isTeacher) return
  loading.value = true
  try {
    questions.value = await getTeacherQuestions()
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.questions-page { }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  padding: 16px;
  background: #faf8f5;
  border-radius: 8px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.course-name {
  font-weight: 500;
  color: #3d3225;
}

.question-content {
  font-size: 15px;
  color: #3d3225;
  margin-bottom: 8px;
}

.question-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
</style>
