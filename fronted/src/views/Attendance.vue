<template>
  <div class="attendance-page">
    <div class="page-header">
      <h2>签到管理</h2>
      <p class="desc">查看和管理课程签到</p>
      <el-button type="primary" @click="showCreateDialog = true" v-if="authStore.isTeacher">
        发起签到
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadActivities">
          <el-option label="全部课程" :value="0" />
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <!-- 签到活动列表 -->
      <div v-loading="loading">
        <div v-if="activities.length === 0 && !loading" class="empty-tip">
          <el-empty description="暂无签到活动" />
        </div>

        <div v-else class="activity-list">
          <div v-for="activity in activities" :key="activity.id" class="activity-card">
            <div class="activity-header">
              <h3>{{ activity.courseName }}</h3>
              <el-tag :type="activity.status === 1 ? 'success' : 'info'">
                {{ activity.status === 1 ? '进行中' : '已结束' }}
              </el-tag>
            </div>
            <div class="activity-info">
              <p>发起时间: {{ formatTime(activity.createTime) }}</p>
              <p>持续时间: {{ activity.duration }} 分钟</p>
              <p v-if="activity.location">签到位置: {{ activity.location }}</p>
            </div>
            <div class="activity-stats">
              <div class="stat-item">
                <span class="stat-value">{{ activity.totalStudents }}</span>
                <span class="stat-label">总人数</span>
              </div>
              <div class="stat-item signed">
                <span class="stat-value">{{ activity.signedCount }}</span>
                <span class="stat-label">已签到</span>
              </div>
              <div class="stat-item unsigned">
                <span class="stat-value">{{ activity.unsignedCount }}</span>
                <span class="stat-label">未签到</span>
              </div>
            </div>
            <div class="activity-actions">
              <!-- 学生签到按钮 -->
              <el-button v-if="!authStore.isTeacher && activity.status === 1" type="success" size="small" @click="handleSignIn(activity)">
                签到
              </el-button>
              <el-button type="primary" size="small" @click="viewDetail(activity)">
                {{ authStore.isTeacher ? '查看详情' : '查看详情' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 发起签到对话框 -->
    <el-dialog v-model="showCreateDialog" title="发起签到" width="450px">
      <el-form :model="signInForm" label-position="top">
        <el-form-item label="选择课程" required>
          <el-select v-model="signInForm.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择班级（可选，不选则对所有班级生效）">
          <el-select v-model="signInForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="签到时长（分钟）">
          <el-input-number v-model="signInForm.duration" :min="1" :max="60" style="width: 100%" />
        </el-form-item>
        <el-form-item label="签到位置（可选）">
          <el-input v-model="signInForm.location" placeholder="如：教室A101" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAttendance" :loading="creating">发起签到</el-button>
      </template>
    </el-dialog>

    <!-- 签到详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="签到详情" width="700px">
      <div v-if="currentActivity" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程">{{ currentActivity.courseName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentActivity.status === 1 ? 'success' : 'info'">
              {{ currentActivity.status === 1 ? '进行中' : '已结束' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ formatTime(currentActivity.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="持续时间">{{ currentActivity.duration }} 分钟</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="已签到" name="signed">
            <el-table :data="currentActivity.signedStudents || []" max-height="300">
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="userName" label="用户名" />
              <el-table-column prop="signTime" label="签到时间">
                <template #default="{ row }">
                  {{ row.signTime ? formatTime(row.signTime) : '-' }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="未签到" name="unsigned">
            <el-table :data="currentActivity.unsignedStudents || []" max-height="300">
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="userName" label="用户名" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getCourseList } from '../api/course'
import { getClassList } from '../api/class'
import { createAttendance, getCourseActivities, getActivityDetails, signIn as signInApi } from '../api/attendance'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const creating = ref(false)
const activities = ref<any[]>([])
const courses = ref<any[]>([])
const classes = ref<any[]>([])
const selectedCourseId = ref<number>(0)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentActivity = ref<any>(null)
const activeTab = ref('signed')

const signInForm = reactive({
  courseId: undefined as number | undefined,
  duration: 15,
  location: '',
  classIds: [] as number[]
})

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadCourses = async () => {
  try {
    const res = await getCourseList({})
    courses.value = res.records || []
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const loadClasses = async () => {
  try {
    const res = await getClassList({})
    classes.value = res.records || []
  } catch (e) {
    console.error('加载班级失败', e)
  }
}

const loadActivities = async () => {
  loading.value = true
  try {
    if (selectedCourseId.value === 0) {
      // 加载所有课程的签到活动
      const allActivities: any[] = []
      for (const course of courses.value) {
        const res = await getCourseActivities(course.id)
        if (res && res.length > 0) {
          allActivities.push(...res)
        }
      }
      activities.value = allActivities.sort((a, b) =>
        new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
    } else {
      activities.value = await getCourseActivities(selectedCourseId.value)
    }
  } catch (e) {
    console.error('加载签到活动失败', e)
  } finally {
    loading.value = false
  }
}

const handleCreateAttendance = async () => {
  if (!signInForm.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  creating.value = true
  try {
    await createAttendance({
      courseId: signInForm.courseId,
      duration: signInForm.duration,
      location: signInForm.location || undefined,
      classIds: signInForm.classIds.length > 0 ? signInForm.classIds : undefined
    })
    ElMessage.success('签到已发起')
    showCreateDialog.value = false
    signInForm.courseId = undefined
    signInForm.duration = 15
    signInForm.location = ''
    signInForm.classIds = []
    loadActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '发起签到失败')
  } finally {
    creating.value = false
  }
}

const handleSignIn = async (activity: any) => {
  try {
    await signInApi({
      activityId: activity.id,
      location: activity.location || undefined
    })
    ElMessage.success('签到成功')
    loadActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '签到失败')
  }
}

const viewDetail = async (activity: any) => {
  try {
    const detail = await getActivityDetails(activity.id)
    currentActivity.value = detail
    showDetailDialog.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

onMounted(async () => {
  await loadCourses()
  loadClasses()
  loadActivities()
})
</script>

<style scoped>
.attendance-page { }
.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }
.filter-bar { margin-bottom: 16px; }

.empty-tip { padding: 40px 0; }

.activity-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.activity-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.activity-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.activity-info {
  margin-bottom: 12px;
}

.activity-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #606266;
}

.activity-stats {
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-item.signed .stat-value { color: #67c23a; }
.stat-item.unsigned .stat-value { color: #f56c6c; }

.stat-label {
  font-size: 12px;
  color: #909399;
}

.activity-actions {
  text-align: right;
}

.detail-content { }

.detail-tabs {
  margin-top: 16px;
}
</style>
