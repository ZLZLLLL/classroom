<template>
  <div class="files-page">
    <div class="page-header">
      <h2>文件管理</h2>
      <p class="desc">课程文件资料</p>
    </div>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadFiles">
          <el-option label="全部课程" :value="0" />
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
      <div v-if="fileList.length > 0" class="file-list">
        <div v-for="f in fileList" :key="f.id" class="file-item">
          <el-icon :size="24"><Document /></el-icon>
          <div class="file-info">
            <div class="file-name">{{ f.fileName }}</div>
            <div class="file-meta">
              <span>类型: {{ getTypeName(f.type) }}</span>
              <span>大小: {{ formatSize(f.fileSize) }}</span>
              <span>上传时间: {{ formatTime(f.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文件" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { getCourseList } from '../api/course'
import { getCourseFiles } from '../api/file'

const loading = ref(false)
const fileList = ref<any[]>([])
const courses = ref<any[]>([])
const selectedCourseId = ref<number>(0)

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const formatSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

const getTypeName = (type: number) => {
  const types = { 1: '课件', 2: '作业', 3: '共享资料' }
  return types[type] || '未知'
}

const loadCourses = async () => {
  try {
    const res = await getCourseList({})
    courses.value = res.records || []
  } catch (e) {
    // ignore
  }
}

const loadFiles = async () => {
  if (selectedCourseId.value === 0) {
    fileList.value = []
    return
  }
  loading.value = true
  try {
    fileList.value = await getCourseFiles(selectedCourseId.value)
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.files-page { }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }
.filter-bar { margin-bottom: 16px; }
.file-list { display: flex; flex-direction: column; gap: 12px; }
.file-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #faf8f5; border-radius: 8px; }
.file-info { flex: 1; }
.file-name { font-weight: 500; color: #3d3225; }
.file-meta { display: flex; gap: 16px; font-size: 12px; color: #999; margin-top: 4px; }
</style>
