<template>
  <div class="files-page">
    <div class="page-header">
      <h2>文件管理</h2>
      <p class="desc">课程文件资料</p>
    </div>

    <el-card shadow="never" v-loading="loading">
      <div class="filter-bar">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadFiles">
          <el-option label="全部课程" :value="0" />
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
      <div v-if="fileList.length > 0" class="file-list">
        <div v-for="f in fileList" :key="f.id" class="file-item">
          <el-icon :size="24"><Document /></el-icon>
          <div class="file-info" @click="handlePreview(f)">
            <div class="file-name">{{ f.fileName }}</div>
            <div class="file-meta">
              <span>类型: {{ getTypeName(f.type) }}</span>
              <span>大小: {{ formatSize(f.fileSize) }}</span>
              <span>上传时间: {{ formatTime(f.createTime) }}</span>
            </div>
          </div>
          <div class="file-actions">
            <el-button link type="primary" @click="handlePreview(f)">预览</el-button>
            <el-button link type="primary" @click="handleDownload(f)">下载</el-button>
            <el-button v-if="authStore.isTeacher" link type="danger" @click="handleDelete(f)">删除</el-button>
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
import { getCourseFiles, previewFile, downloadFile } from '../api/file'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const loading = ref(false)
const fileList = ref<any[]>([])
const courses = ref<any[]>([])
const selectedCourseId = ref<number>(0)
const authStore = useAuthStore()

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
  const types: Record<number, string> = { 1: '课件', 2: '作业', 3: '共享资料' }
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

const handlePreview = async (f: any) => {
  try {
    await previewFile(f.id)
  } catch {
    ElMessage.error('预览失败')
  }
}

const handleDownload = async (f: any) => {
  try {
    await downloadFile(f.id, f.fileName)
  } catch {
    ElMessage.error('下载失败')
  }
}

const handleDelete = async (f: any) => {
  try {
    await ElMessageBox.confirm(`确定删除文件「${f.fileName}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/files/${f.id}`)
    ElMessage.success('删除成功')
    await loadFiles()
  } catch {
    // 用户取消或接口失败时保持静默，避免重复提示
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }
.filter-bar { margin-bottom: 16px; }
.file-list { display: flex; flex-direction: column; gap: 12px; }
.file-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #faf8f5; border-radius: 8px; }
.file-info { flex: 1; cursor: pointer; }
.file-name { font-weight: 500; color: #3d3225; }
.file-meta { display: flex; gap: 16px; font-size: 12px; color: #999; margin-top: 4px; }
.file-actions { display: flex; gap: 8px; }
</style>
