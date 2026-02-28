<template>
  <div class="homework-page">
    <div class="page-header">
      <h2>作业管理</h2>
      <p class="desc">查看和提交作业</p>
    </div>

    <el-card shadow="never" v-loading="loading">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getTeacherHomeworks } from '../api/homework'

const authStore = useAuthStore()
const loading = ref(false)
const homeworks = ref<any[]>([])

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadHomeworks = async () => {
  if (!authStore.isTeacher) return
  loading.value = true
  try {
    const res = await getTeacherHomeworks()
    homeworks.value = res.records
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHomeworks()
})
</script>

<style scoped>
.homework-page { }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.homework-item {
  padding: 16px;
  background: #faf8f5;
  border-radius: 8px;
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
</style>
