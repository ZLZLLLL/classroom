<template>
  <div class="exam-notice-page">
    <div class="page-header">
      <h2>考试通知</h2>
      <p class="desc">查看课程考试通知</p>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-timeline v-if="notices.length">
        <el-timeline-item v-for="notice in notices" :key="notice.id" :timestamp="formatTime(notice.createTime)">
          <h4>{{ notice.title }}</h4>
          <p class="notice-meta">课程：{{ notice.courseName || '课程ID:' + notice.courseId }}</p>
          <p class="notice-content">{{ notice.content }}</p>
          <el-button size="small" type="primary" plain @click="goToExam(notice.examId)">查看考试</el-button>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无考试通知" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getExamNotices, type ExamNotice } from '../api/exam'

const router = useRouter()
const loading = ref(false)
const notices = ref<ExamNotice[]>([])

const loadNotices = async () => {
  loading.value = true
  try {
    const res = await getExamNotices()
    notices.value = res || []
  } finally {
    loading.value = false
  }
}

const goToExam = (examId: number) => {
  router.push({ path: '/exams', query: { examId } })
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.exam-notice-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-header .desc {
  color: #888;
  margin-top: 4px;
}

.notice-meta {
  color: #666;
  margin: 6px 0;
}

.notice-content {
  margin-bottom: 12px;
}
</style>
