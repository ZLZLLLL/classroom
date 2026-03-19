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
              <el-tag :type="canSubmitHomework(hw) ? 'success' : 'danger'">
                {{ canSubmitHomework(hw) ? '进行中' : '已截止' }}
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
        <div v-for="hw in homeworks" :key="hw.id" class="homework-item" @click="viewHomeworkDetail(hw)">
          <div class="homework-header">
            <span class="homework-title">{{ hw.title }}</span>
            <el-tag :type="canSubmitHomework(hw) ? 'success' : 'danger'">
              {{ canSubmitHomework(hw) ? '进行中' : '已截止' }}
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
          <el-tag :type="canSubmitHomework(currentHomework) ? 'success' : 'danger'">
            {{ canSubmitHomework(currentHomework) ? '进行中' : '已截止' }}
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

        <!-- 学生提交状态和提交按钮 -->
        <el-divider />
        <div class="submit-section" v-if="!authStore.isTeacher">
          <div v-if="currentSubmit">
            <p><strong>我的提交：</strong></p>
            <p>提交内容: {{ currentSubmit.content || '无' }}</p>
            <p v-if="currentSubmit.filePath">
              附件:
              <a :href="currentSubmit.filePath" target="_blank" rel="noopener noreferrer">查看附件</a>
            </p>
            <p>提交时间: {{ formatTime(currentSubmit.submitTime) }}</p>
            <p v-if="currentSubmit.score !== undefined && currentSubmit.score !== null">
              得分: <el-tag type="success">{{ currentSubmit.score }}分</el-tag>
            </p>
            <p v-if="currentSubmit.feedback">教师反馈: {{ currentSubmit.feedback }}</p>
          </div>
          <div v-else-if="canSubmitHomework(currentHomework)">
            <el-input v-model="submitContent" type="textarea" :rows="4" placeholder="请输入作业内容" />
            <div class="submit-upload-row">
              <el-upload :show-file-list="false" :http-request="handleHomeworkFileUpload" accept="image/*,.pdf,.doc,.docx,.zip,.rar,.txt">
                <el-button>上传附件/图片</el-button>
              </el-upload>
              <span v-if="submitFileName" class="submit-file-name">已上传：{{ submitFileName }}</span>
            </div>
            <el-button type="primary" @click="handleSubmit" style="margin-top: 10px;">提交作业</el-button>
          </div>
          <div v-else>
            <el-alert title="已超过截止时间，无法提交" type="warning" :closable="false" />
          </div>
        </div>

        <!-- 教师端：查看提交与打分 -->
        <div class="teacher-submit-section" v-else>
          <p><strong>学生提交列表：</strong></p>
          <el-table :data="teacherSubmits" size="small" max-height="320" v-loading="loadingTeacherSubmits" @row-click="openGradeDialog">
            <el-table-column prop="userName" label="学生" min-width="120" />
            <el-table-column prop="content" label="提交内容" min-width="180" show-overflow-tooltip />
            <el-table-column label="附件" width="110">
              <template #default="{ row }">
                <a v-if="row.filePath" :href="row.filePath" target="_blank" rel="noopener noreferrer">查看</a>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.submitTime) }}
              </template>
            </el-table-column>
            <el-table-column label="得分" width="90">
              <template #default="{ row }">
                {{ row.score ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : 'warning'">
                  {{ row.status === 2 ? '已批改' : '待批改' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="openGradeDialog(row)">打分</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingTeacherSubmits && teacherSubmits.length === 0" description="暂无学生提交" />
        </div>
      </div>
    </el-dialog>

    <!-- 教师端：作业详情大面板 -->
    <el-drawer v-model="teacherDetailVisible" :with-header="false" size="90%">
      <div class="teacher-detail">
        <div class="teacher-detail-header">
          <div>
            <h2>{{ currentHomework?.title || '作业详情' }}</h2>
            <div class="teacher-detail-meta">
              <el-tag v-if="currentHomework" :type="canSubmitHomework(currentHomework) ? 'success' : 'danger'">
                {{ currentHomework && canSubmitHomework(currentHomework) ? '进行中' : '已截止' }}
              </el-tag>
              <span>总分：{{ currentHomework?.totalPoints ?? '-' }}分</span>
              <span v-if="currentHomework?.deadline">截止：{{ formatTime(currentHomework.deadline) }}</span>
            </div>
          </div>
          <el-button @click="teacherDetailVisible = false">关闭</el-button>
        </div>

        <el-card shadow="never" class="teacher-detail-content">
          <div class="detail-content">
            <p><strong>作业内容：</strong></p>
            <p>{{ currentHomework?.content || '暂无内容' }}</p>
          </div>
        </el-card>

        <el-tabs v-model="teacherSubmitTab" class="teacher-submit-tabs">
          <el-tab-pane :label="`已提交(${teacherSubmits.length})`" name="submitted">
            <el-table :data="teacherSubmits" size="small" max-height="520" v-loading="loadingTeacherSubmits" @row-click="openGradeDialog">
              <el-table-column prop="userName" label="学生" min-width="140" />
              <el-table-column prop="content" label="提交内容" min-width="240" show-overflow-tooltip />
              <el-table-column label="附件" width="120">
                <template #default="{ row }">
                  <a v-if="row.filePath" :href="row.filePath" target="_blank" rel="noopener noreferrer">查看</a>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="提交时间" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.submitTime) }}
                </template>
              </el-table-column>
              <el-table-column label="得分" width="90">
                <template #default="{ row }">
                  {{ row.score ?? '-' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.status === 2 ? 'success' : 'warning'">
                    {{ row.status === 2 ? '已批改' : '待批改' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openGradeDialog(row)">打分</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loadingTeacherSubmits && teacherSubmits.length === 0" description="暂无学生提交" />
          </el-tab-pane>

          <el-tab-pane :label="`未提交(${pendingStudents.length})`" name="notSubmitted">
            <el-table :data="pendingStudents" size="small" max-height="520">
              <el-table-column prop="userName" label="学生" min-width="160" />
              <el-table-column prop="className" label="班级" min-width="160" />
              <el-table-column prop="classId" label="班级ID" width="120" />
            </el-table>
            <el-empty v-if="pendingStudents.length === 0" description="暂无未提交学生" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-drawer v-model="gradeDrawerVisible" :with-header="false" size="100%">
      <div class="grade-drawer">
        <div class="grade-drawer-header">
          <div>
            <h2>作业打分</h2>
            <div class="grade-drawer-meta">
              <span>作业：{{ currentHomework?.title || '-' }}</span>
              <span>学生：{{ gradingSubmit?.userName || '-' }}</span>
            </div>
          </div>
          <el-button @click="gradeDrawerVisible = false">关闭</el-button>
        </div>

        <el-form label-position="top" class="grade-drawer-form">
          <el-form-item label="作业内容">
            <el-input :model-value="currentHomework?.content || '暂无内容'" type="textarea" :rows="5" disabled />
          </el-form-item>
          <el-form-item label="学生回答">
            <el-input :model-value="gradingSubmit?.content || '无'" type="textarea" :rows="6" disabled />
            <div style="margin-top: 6px">
              <a v-if="gradingSubmit?.filePath" :href="gradingSubmit.filePath" target="_blank" rel="noopener noreferrer">查看附件</a>
              <span v-else>-</span>
            </div>
          </el-form-item>
          <el-form-item label="得分">
            <el-input-number v-model="gradeForm.score" :min="0" :max="currentHomework?.totalPoints || 100" />
          </el-form-item>
          <el-form-item label="反馈">
            <el-input v-model="gradeForm.feedback" type="textarea" :rows="4" placeholder="请输入反馈（可选）" />
          </el-form-item>
        </el-form>

        <div class="grade-drawer-footer">
          <el-button @click="gradeDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="grading" @click="handleGradeSubmit">提交评分</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getTeacherHomeworks, getStudentHomeworks, submitHomework, getMyHomeworkSubmit, gradeHomework, getHomeworkSubmitStatus, type HomeworkPendingStudent } from '../api/homework'
import { getCourseById } from '../api/course'
import { ElMessage } from 'element-plus'
import { uploadFile } from '../api/file'

const authStore = useAuthStore()
const route = useRoute()
const loading = ref(false)
const homeworks = ref<any[]>([])
const courseList = ref<any[]>([])
const selectedCourseId = ref(0)

// 详情对话框
const detailVisible = ref(false)
const teacherDetailVisible = ref(false)
const teacherSubmitTab = ref('submitted')
const currentHomework = ref<any>(null)
const currentSubmit = ref<any>(null)
const teacherSubmits = ref<any[]>([])
const pendingStudents = ref<HomeworkPendingStudent[]>([])
const loadingTeacherSubmits = ref(false)
const submitContent = ref('')
const submitFilePath = ref('')
const submitFileName = ref('')
const gradeDrawerVisible = ref(false)
const grading = ref(false)
const gradingSubmit = ref<any>(null)
const gradeForm = ref({ score: 0, feedback: '' })

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const canSubmitHomework = (hw: any) => {
  if (!hw) return false
  // 未设置截止时间时，默认允许提交
  if (!hw.deadline) return true
  return new Date(hw.deadline) > new Date()
}

const loadHomeworks = async () => {
  loading.value = true
  try {
    if (authStore.isTeacher) {
      const res = await getTeacherHomeworks()
      homeworks.value = res.records || []
    } else {
      const res = await getStudentHomeworks()
      const all = res.records || []
      // 获取课程名称
      for (const hw of all) {
        try {
          const course = await getCourseById(hw.courseId)
          hw.courseName = course?.name || ''
        } catch (e) {
          hw.courseName = ''
        }
      }
      homeworks.value = selectedCourseId.value
        ? all.filter((hw: any) => hw.courseId === selectedCourseId.value)
        : all
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
  submitContent.value = ''
  submitFilePath.value = ''
  submitFileName.value = ''

  if (authStore.isTeacher) {
    teacherDetailVisible.value = true
    teacherSubmitTab.value = 'submitted'
    await loadTeacherSubmits(hw.id)
  } else {
    detailVisible.value = true
    // 获取提交状态
    try {
      currentSubmit.value = await getMyHomeworkSubmit(hw.id)
    } catch (e) {
      currentSubmit.value = null
    }
  }
}

const loadTeacherSubmits = async (homeworkId: number) => {
  loadingTeacherSubmits.value = true
  try {
    const status = await getHomeworkSubmitStatus(homeworkId)
    teacherSubmits.value = status.submitted || []
    pendingStudents.value = status.notSubmitted || []
  } catch (e: any) {
    teacherSubmits.value = []
    pendingStudents.value = []
    ElMessage.error(e.message || '获取提交列表失败')
  } finally {
    loadingTeacherSubmits.value = false
  }
}

const handleSubmit = async () => {
  if (!submitContent.value.trim() && !submitFilePath.value) {
    ElMessage.warning('请输入作业内容或上传附件')
    return
  }

  try {
    await submitHomework(currentHomework.value.id, submitContent.value, submitFilePath.value || undefined)
    ElMessage.success('提交成功')
    // 刷新提交状态
    currentSubmit.value = await getMyHomeworkSubmit(currentHomework.value.id)
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
}

const handleHomeworkFileUpload = async (options: any) => {
  if (!currentHomework.value) return
  try {
    const uploaded = await uploadFile(options.file, {
      courseId: currentHomework.value.courseId,
      type: 2,
      category: 'homework-submit',
      persist: false
    })
    submitFilePath.value = uploaded.fileUrl || uploaded.filePath || ''
    submitFileName.value = uploaded.fileName || options.file.name || '附件'
    ElMessage.success('附件上传成功')
  } catch {
    ElMessage.error('附件上传失败')
  }
}

const openGradeDialog = (submit: any) => {
  gradingSubmit.value = submit
  gradeForm.value = {
    score: Number(submit.score) || 0,
    feedback: submit.feedback || ''
  }
  gradeDrawerVisible.value = true
}

const handleGradeSubmit = async () => {
  if (!gradingSubmit.value) return
  grading.value = true
  try {
    await gradeHomework(gradingSubmit.value.id, gradeForm.value.score, gradeForm.value.feedback)
    ElMessage.success('评分成功')
    gradeDrawerVisible.value = false
    if (currentHomework.value) {
      await loadTeacherSubmits(currentHomework.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '评分失败')
  } finally {
    grading.value = false
  }
}

onMounted(() => {
  loadHomeworks().then(async () => {
    loadCourseList()

    // 从课程详情跳转时，支持自动定位课程/作业。
    const qCourseId = Number(route.query.courseId || 0)
    const qHomeworkId = Number(route.query.homeworkId || 0)
    if (!authStore.isTeacher && qCourseId) {
      selectedCourseId.value = qCourseId
      await loadHomeworks()
    }
    if (!authStore.isTeacher && qHomeworkId) {
      const target = homeworks.value.find((hw: any) => hw.id === qHomeworkId)
      if (target) {
        await viewHomeworkDetail(target)
      }
    }
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
  margin-bottom: 12px;
}

.detail-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.submit-section {
  margin-top: 12px;
}

.submit-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.submit-file-name {
  color: #666;
  font-size: 12px;
}

.teacher-detail {
  padding: 16px;
}

.teacher-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.teacher-detail-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.teacher-detail-content {
  margin-bottom: 12px;
}

.teacher-submit-tabs {
  margin-top: 8px;
}

.grade-drawer {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.grade-drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.grade-drawer-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.grade-drawer-form {
  flex: 1;
  overflow: auto;
  padding-right: 8px;
}

.grade-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
}
</style>

