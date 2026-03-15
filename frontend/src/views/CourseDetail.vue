<template>
  <div class="course-detail page-full">
    <div class="page-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div v-if="course" class="header-info">
        <h2>{{ course.name }}</h2>
        <p>{{ course.description }}</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="course-tabs">
      <el-tab-pane label="课程概览" name="overview">
        <div class="overview-content">
          <el-card shadow="never">
            <template #header>
              <span>课程信息</span>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">课程名称</span>
                <span class="value">{{ course?.name }}</span>
              </div>
              <div class="info-item">
                <span class="label">授课教师</span>
                <span class="value">{{ course?.teacherName }}</span>
              </div>
              <div class="info-item">
                <span class="label">学生人数</span>
                <span class="value">{{ course?.studentCount }} 人</span>
              </div>
              <div class="info-item">
                <span class="label">关联班级</span>
                <span class="value">{{ course?.classNames?.join('、') || '暂无' }}</span>
              </div>
            </div>
          </el-card>

          <el-card v-if="authStore.isTeacher" shadow="never" class="quick-actions">
            <template #header>
              <span>快捷操作</span>
            </template>
            <div class="action-grid">
              <div class="action-item" @click="activeTab = 'attendance'">
                <el-icon :size="32"><Check /></el-icon>
                <span>发起签到</span>
              </div>
              <div class="action-item" @click="activeTab = 'questions'">
                <el-icon :size="32"><QuestionFilled /></el-icon>
                <span>发起提问</span>
              </div>
              <div class="action-item" @click="activeTab = 'homework'">
                <el-icon :size="32"><Document /></el-icon>
                <span>发布作业</span>
              </div>
              <div class="action-item" @click="activeTab = 'files'">
                <el-icon :size="32"><Files /></el-icon>
                <span>上传资料</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="签到" name="attendance">
        <div class="tab-content">
          <!-- 教师端：签到活动列表 -->
          <template v-if="authStore.isTeacher">
            <div class="toolbar">
              <el-button type="primary" @click="showSignInDialog = true">
                <el-icon><Plus /></el-icon>
                发起签到
              </el-button>
            </div>
            <div v-loading="loadingActivities" class="activity-list">
              <el-card v-for="activity in activities" :key="activity.id" shadow="never" class="activity-card" @click="viewActivityDetails(activity)">
                <div class="activity-header">
                  <h3>签到活动 #{{ activity.id }}</h3>
                  <el-tag :type="activity.status === 1 ? 'success' : 'info'">
                    {{ activity.status === 1 ? '进行中' : '已结束' }}
                  </el-tag>
                </div>
                <div class="activity-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.totalStudents || 0 }}</span>
                    <span class="stat-label">总人数</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.signedCount || 0 }}</span>
                    <span class="stat-label">已签到</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.unsignedCount || 0 }}</span>
                    <span class="stat-label">未签到</span>
                  </div>
                </div>
                <div class="activity-time">
                  发起时间：{{ new Date(activity.createTime).toLocaleString() }}
                  <span v-if="activity.duration">，持续{{ activity.duration }}分钟</span>
                </div>
              </el-card>
              <el-empty v-if="!loadingActivities && activities.length === 0" description="暂无签到活动" />
            </div>
          </template>
          <!-- 学生端：显示待签到列表 -->
          <template v-else>
            <div v-loading="loadingStudentActivities" class="activity-list">
              <el-card v-for="activity in studentActivities" :key="activity.id" shadow="never" class="activity-card student-activity">
                <div class="activity-header">
                  <h3>{{ activity.courseName }}</h3>
                  <el-tag type="warning">待签到</el-tag>
                </div>
                <div class="activity-time">
                  发起时间：{{ new Date(activity.createTime).toLocaleString() }}，持续{{ activity.duration }}分钟
                </div>
                <el-button type="primary" class="signin-btn" @click="handleStudentSignIn(activity)">
                  立即签到
                </el-button>
              </el-card>
              <el-empty v-if="!loadingStudentActivities && studentActivities.length === 0" description="暂无待签到活动" />
            </div>
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提问" name="questions">
        <div class="tab-content">
          <div v-if="authStore.isTeacher" class="toolbar">
            <el-button type="primary" @click="showQuestionDialog = true">
              <el-icon><Plus /></el-icon>
              发起提问
            </el-button>
          </div>
          <div v-loading="loadingQuestions" class="question-list">
            <el-card v-for="q in questions" :key="q.id" shadow="never" class="question-card">
              <div class="question-header">
                <el-tag :type="q.type === 1 ? '' : q.type === 2 ? 'warning' : 'info'" size="small">
                  {{ q.type === 1 ? '单选' : q.type === 2 ? '多选' : '问答' }}
                </el-tag>
                <el-tag :type="q.status === 1 ? 'success' : 'info'" size="small" style="margin-left: 8px">
                  {{ q.status === 1 ? '进行中' : '已结束' }}
                </el-tag>
                <span class="question-time">{{ new Date(q.createTime).toLocaleString() }}</span>
                <el-button
                  v-if="authStore.isTeacher && q.status === 1"
                  type="danger"
                  link
                  size="small"
                  style="margin-left: auto"
                  @click="handleCloseQuestion(q)"
                >结束提问</el-button>
              </div>
              <p class="question-content">{{ q.content }}</p>
              <div v-if="q.options && q.options.length > 0" class="question-options">
                <span v-for="opt in q.options" :key="opt.label" class="option-tag">
                  {{ opt.label }}. {{ opt.content }}
                </span>
              </div>
              <div class="question-stats">
                <span>答题人数：{{ q.answerCount || 0 }}</span>
                <span v-if="q.type !== 3">正确人数：{{ q.correctCount || 0 }}</span>
                <span>分值：{{ q.points }} 分</span>
              </div>
            </el-card>
            <el-empty v-if="!loadingQuestions && questions.length === 0" description="暂无提问记录" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="作业" name="homework">
        <div class="tab-content">
          <div v-if="authStore.isTeacher" class="toolbar">
            <el-button type="primary" @click="showHomeworkDialog = true">
              <el-icon><Plus /></el-icon>
              发布作业
            </el-button>
          </div>
          <div v-loading="loadingHomeworks" class="homework-list">
            <el-card v-for="hw in homeworks" :key="hw.id" shadow="never" class="homework-card">
              <div class="homework-header">
                <h3>{{ hw.title }}</h3>
                <el-tag v-if="hw.deadline" :type="new Date(hw.deadline) > new Date() ? 'success' : 'danger'">
                  {{ new Date(hw.deadline) > new Date() ? '进行中' : '已截止' }}
                </el-tag>
              </div>
              <p class="homework-content">{{ hw.content || '暂无内容' }}</p>
              <div class="homework-meta">
                <span v-if="hw.chapter">章节：{{ hw.chapter }}</span>
                <span>总分：{{ hw.totalPoints }}分</span>
                <span v-if="hw.deadline">截止：{{ new Date(hw.deadline).toLocaleString() }}</span>
              </div>
            </el-card>
            <el-empty v-if="!loadingHomeworks && homeworks.length === 0" description="暂无作业" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="资料" name="files">
        <div class="tab-content">
          <div class="toolbar">
            <el-upload
              v-if="authStore.isTeacher"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="{ courseId: courseId, type: 3 }"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传资料
              </el-button>
            </el-upload>
          </div>
          <el-empty description="暂无资料" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="学生" name="students" v-if="authStore.isTeacher">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" :disabled="selectedStudentIds.length === 0" @click="showAddPointsDialog = true">
              给选中学生加分 ({{ selectedStudentIds.length }})
            </el-button>
          </div>
          <el-table :data="studentsWithPoints" v-loading="loadingStudents" @selection-change="handleStudentSelectionChange">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="coursePoints" label="本课程积分" width="120">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.coursePoints || 0 }} 分</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 签到对话框 -->
    <el-dialog v-model="showSignInDialog" title="发起签到" width="400px">
      <el-form :model="signInForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发起）">
          <el-select v-model="signInForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="签到时长（分钟）">
          <el-input-number v-model="signInForm.duration" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="签到位置（可选）">
          <el-input v-model="signInForm.location" placeholder="如：教室A101" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSignInDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAttendance">发起签到</el-button>
      </template>
    </el-dialog>

    <!-- 提问对话框 -->
    <el-dialog v-model="showQuestionDialog" title="发起提问" width="600px">
      <el-form :model="questionForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发起）">
          <el-select v-model="questionForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题内容">
          <el-input v-model="questionForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-radio-group v-model="questionForm.type">
            <el-radio :value="1">单选题</el-radio>
            <el-radio :value="2">多选题</el-radio>
            <el-radio :value="3">问答题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="questionForm.type !== 3" label="选项">
          <div v-for="(opt, idx) in questionForm.options" :key="idx" class="option-row">
            <el-input v-model="opt.label" placeholder="选项标签" style="width: 80px" />
            <el-input v-model="opt.content" placeholder="选项内容" />
            <el-button type="danger" link @click="questionForm.options.splice(idx, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button text type="primary" @click="questionForm.options.push({ label: '', content: '' })">
            + 添加选项
          </el-button>
        </el-form-item>
        <el-form-item v-if="questionForm.type !== 3" label="正确答案">
          <el-input v-model="questionForm.correctAnswer" placeholder="如：A" />
        </el-form-item>
        <el-form-item label="答题时限（秒）">
          <el-input-number v-model="questionForm.duration" :min="10" :max="600" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="questionForm.points" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuestionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateQuestion">发起提问</el-button>
      </template>
    </el-dialog>

    <!-- 作业对话框 -->
    <el-dialog v-model="showHomeworkDialog" title="发布作业" width="600px">
      <el-form :model="homeworkForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发布）">
          <el-select v-model="homeworkForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题">
          <el-input v-model="homeworkForm.title" />
        </el-form-item>
        <el-form-item label="作业内容">
          <el-input v-model="homeworkForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="章节">
          <el-input v-model="homeworkForm.chapter" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="homeworkForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="homeworkForm.totalPoints" :min="1" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHomeworkDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateHomework">发布作业</el-button>
      </template>
    </el-dialog>

    <!-- 手动加分对话框 -->
    <el-dialog v-model="showAddPointsDialog" title="手动加分" width="400px">
      <el-form :model="addPointsForm" label-position="top">
        <el-form-item label="积分">
          <el-input-number v-model="addPointsForm.points" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="addPointsForm.description" placeholder="如：课堂表现优秀" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddPointsDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingPoints" @click="handleAddPoints">确认加分</el-button>
      </template>
    </el-dialog>

    <!-- 签到活动详情对话框 -->
    <el-dialog v-model="showActivityDetailDialog" title="签到详情" width="700px">
      <div v-if="activityDetail" class="activity-detail">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane label="已签到" name="signed">
            <el-table :data="activityDetail.signedStudents" max-height="300">
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="userName" label="用户名" />
              <el-table-column prop="signTime" label="签到时间">
                <template #default="{ row }">
                  {{ row.signTime ? new Date(row.signTime).toLocaleString() : '-' }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="未签到" name="unsigned">
            <el-table :data="activityDetail.unsignedStudents" max-height="300">
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, QuestionFilled, Document, Files, Plus, Upload, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useCourseStore } from '../stores/course'
import { getCourseStudents } from '../api/course'
import { createAttendance, getCourseActivities, getActivityDetails, getStudentActivities, signIn as studentSignIn } from '../api/attendance'
import { createQuestion, getCourseQuestions, closeQuestion, type Question } from '../api/question'
import { getCoursePointsRanking, addPointsForUsers, type PointsRankingRecord } from '../api/points'
import { createHomework, getCourseHomeworks, type Homework } from '../api/homework'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()

const courseId = Number(route.params.id)
const course = computed(() => courseStore.currentCourse)

// 班级列表
const courseClasses = computed(() => {
  if (!course.value) return []
  return course.value.classIds.map((id, i) => ({ id, name: course.value!.classNames[i] || '' }))
})

const activeTab = ref('overview')
const students = ref<any[]>([])
const loadingStudents = ref(false)

// 提问列表
const questions = ref<Question[]>([])
const loadingQuestions = ref(false)

// 积分排行
const pointsRanking = ref<PointsRankingRecord[]>([])

// 学生加分
const selectedStudentIds = ref<number[]>([])
const showAddPointsDialog = ref(false)
const addingPoints = ref(false)
const addPointsForm = reactive({
  points: 5,
  description: ''
})

// 学生列表+积分合并展示
const studentsWithPoints = computed(() => {
  return students.value.map(s => {
    const ranking = pointsRanking.value.find(r => r.userId === s.id)
    return { ...s, coursePoints: ranking?.points ?? 0 }
  })
})

// 作业列表
const homeworks = ref<Homework[]>([])
const loadingHomeworks = ref(false)

// 签到
const showSignInDialog = ref(false)
const signInForm = reactive({
  duration: 15,
  location: '',
  classIds: [] as number[]
})

// 签到活动列表（教师端）
const activities = ref<any[]>([])
const loadingActivities = ref(false)

// 学生待签到活动
const studentActivities = ref<any[]>([])
const loadingStudentActivities = ref(false)

// 签到活动详情
const showActivityDetailDialog = ref(false)
const activityDetail = ref<any>(null)
const activeDetailTab = ref('signed')

// 提问
const showQuestionDialog = ref(false)
const questionForm = reactive({
  content: '',
  type: 1,
  options: [
    { label: 'A', content: '' },
    { label: 'B', content: '' },
    { label: 'C', content: '' },
    { label: 'D', content: '' }
  ],
  correctAnswer: '',
  duration: 60,
  points: 5,
  classIds: [] as number[]
})

// 作业
const showHomeworkDialog = ref(false)
const homeworkForm = reactive({
  title: '',
  content: '',
  chapter: '',
  deadline: '',
  totalPoints: 100,
  classIds: [] as number[]
})

const uploadUrl = '/api/v1/files/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

onMounted(async () => {
  await courseStore.fetchCourseById(courseId)
  if (authStore.isTeacher) {
    loadStudents()
    loadPointsRanking()
    loadActivities()
    loadQuestions()
  } else {
    loadStudentActivities()
    loadQuestions()
  }
  loadHomeworks()
})

const loadStudents = async () => {
  loadingStudents.value = true
  try {
    students.value = await getCourseStudents(courseId)
  } finally {
    loadingStudents.value = false
  }
}

const loadPointsRanking = async () => {
  try {
    pointsRanking.value = await getCoursePointsRanking(courseId)
  } catch (e) {
    // ignore
  }
}

const loadQuestions = async () => {
  loadingQuestions.value = true
  try {
    questions.value = await getCourseQuestions(courseId)
  } catch (e) {
    // ignore
  } finally {
    loadingQuestions.value = false
  }
}

const handleStudentSelectionChange = (rows: any[]) => {
  selectedStudentIds.value = rows.map(r => r.id)
}

const handleAddPoints = async () => {
  if (selectedStudentIds.value.length === 0) return
  if (!addPointsForm.description) {
    ElMessage.warning('请填写加分说明')
    return
  }
  addingPoints.value = true
  try {
    await addPointsForUsers({
      userIds: selectedStudentIds.value,
      courseId,
      points: addPointsForm.points,
      description: addPointsForm.description
    })
    ElMessage.success('加分成功')
    showAddPointsDialog.value = false
    addPointsForm.points = 5
    addPointsForm.description = ''
    selectedStudentIds.value = []
    loadPointsRanking()
  } catch (e: any) {
    ElMessage.error(e.message || '加分失败')
  } finally {
    addingPoints.value = false
  }
}

const handleCloseQuestion = async (q: Question) => {
  try {
    await closeQuestion(q.id)
    ElMessage.success('提问已结束')
    loadQuestions()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const loadHomeworks = async () => {
  loadingHomeworks.value = true
  try {
    const res = await getCourseHomeworks(courseId)
    homeworks.value = res.records
  } catch (e) {
    // ignore
  } finally {
    loadingHomeworks.value = false
  }
}

// 加载签到活动列表（教师端）
const loadActivities = async () => {
  loadingActivities.value = true
  try {
    activities.value = await getCourseActivities(courseId)
  } catch (e) {
    // ignore
  } finally {
    loadingActivities.value = false
  }
}

// 加载学生待签到活动
const loadStudentActivities = async () => {
  loadingStudentActivities.value = true
  try {
    studentActivities.value = await getStudentActivities()
  } catch (e) {
    // ignore
  } finally {
    loadingStudentActivities.value = false
  }
}

// 查看签到活动详情
const viewActivityDetails = async (activity: any) => {
  try {
    activityDetail.value = await getActivityDetails(activity.id)
    showActivityDetailDialog.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

// 学生签到
const handleStudentSignIn = async (activity: any) => {
  try {
    await studentSignIn({
      courseId: activity.courseId,
      activityId: activity.id
    })
    ElMessage.success('签到成功！')
    loadStudentActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '签到失败')
  }
}

const handleCreateAttendance = async () => {
  try {
    await createAttendance({
      courseId,
      duration: signInForm.duration,
      location: signInForm.location || undefined,
      classIds: signInForm.classIds.length > 0 ? signInForm.classIds : undefined
    })
    ElMessage.success('签到已发起')
    showSignInDialog.value = false
    signInForm.classIds = []
    signInForm.location = ''
    loadActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '发起签到失败')
  }
}

const handleCreateQuestion = async () => {
  try {
    await createQuestion({
      courseId,
      content: questionForm.content,
      type: questionForm.type,
      options: questionForm.type !== 3 ? questionForm.options.filter(o => o.content) : undefined,
      correctAnswer: questionForm.type !== 3 ? questionForm.correctAnswer : undefined,
      duration: questionForm.duration,
      points: questionForm.points,
      classIds: questionForm.classIds.length > 0 ? questionForm.classIds : undefined
    })
    ElMessage.success('提问已发起')
    showQuestionDialog.value = false
    questionForm.classIds = []
    loadQuestions()
  } catch (e: any) {
    ElMessage.error(e.message || '发起提问失败')
  }
}

const handleCreateHomework = async () => {
  try {
    await createHomework({
      courseId,
      title: homeworkForm.title,
      content: homeworkForm.content,
      chapter: homeworkForm.chapter,
      deadline: homeworkForm.deadline,
      totalPoints: homeworkForm.totalPoints,
      classIds: homeworkForm.classIds.length > 0 ? homeworkForm.classIds : undefined
    })
    ElMessage.success('作业已发布')
    showHomeworkDialog.value = false
    homeworkForm.classIds = []
    homeworkForm.title = ''
    homeworkForm.content = ''
    homeworkForm.chapter = ''
    homeworkForm.deadline = ''
    homeworkForm.totalPoints = 100
    loadHomeworks()
  } catch (e) {
    // error
  }
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}
</script>

<style scoped>
.course-detail {
  width: 100%;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.header-info h2 {
  font-size: 28px;
  font-weight: 600;
  color: #3d3225;
  margin: 12px 0 8px;
}

.header-info p {
  color: #8b7355;
  margin: 0;
}

.course-tabs {
  background: white;
  border-radius: 16px;
  padding: 24px;
}

.overview-content {
  display: grid;
  gap: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  padding: 16px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: #faf8f5;
  border-radius: 12px;
}

.info-item .label {
  color: #8b7355;
  font-size: 14px;
}

.info-item .value {
  color: #3d3225;
  font-size: 16px;
  font-weight: 500;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 20px;
  background: #faf8f5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  background: #f5f0e8;
  transform: translateY(-2px);
}

.action-item span {
  color: #3d3225;
  font-size: 14px;
}

.tab-content {
  min-height: 300px;
  padding: 16px 0;
}

.toolbar {
  margin-bottom: 16px;
}

.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.homework-list {
  display: grid;
  gap: 16px;
}

.homework-card {
  margin-bottom: 0;
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.homework-header h3 {
  margin: 0;
  font-size: 16px;
  color: #3d3225;
}

.homework-content {
  color: #666;
  font-size: 14px;
  margin: 0 0 12px;
}

.homework-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.attendance-list {
  min-height: 200px;
}

.activity-list {
  display: grid;
  gap: 16px;
}

.activity-card {
  cursor: pointer;
  transition: all 0.2s;
}

.activity-card:hover {
  transform: translateY(-2px);
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
  color: #3d3225;
}

.activity-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #3d3225;
}

.stat-label {
  font-size: 12px;
  color: #8b7355;
}

.activity-time {
  font-size: 13px;
  color: #999;
}

.student-activity {
  border-left: 4px solid #409eff;
}

.signin-btn {
  margin-top: 12px;
  width: 100%;
}

.question-list {
  display: grid;
  gap: 16px;
}

.question-card {
  margin-bottom: 0;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.question-time {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.question-content {
  font-size: 15px;
  color: #303133;
  margin: 0 0 10px;
}

.question-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.option-tag {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 13px;
  color: #606266;
}

.question-stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}
</style>
