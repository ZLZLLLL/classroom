<template>
  <div class="exam-page">
    <div class="page-header">
      <h2>{{ authStore.isTeacher ? '考试管理' : '我的考试' }}</h2>
      <p class="desc">{{ authStore.isTeacher ? '创建、发布与批改考试' : '参加考试并查看成绩' }}</p>
      <div v-if="authStore.isTeacher" class="header-actions">
        <el-button type="primary" @click="openCreateDialog">创建考试</el-button>
      </div>
    </div>

    <el-card shadow="never" v-loading="loading">
      <template v-if="authStore.isTeacher">
        <el-table :data="teacherExams" size="small" @row-click="openExamDetail">
          <el-table-column prop="title" label="考试标题" min-width="200" />
          <el-table-column prop="courseName" label="课程" min-width="160" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row)">{{ statusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="180">
            <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column prop="totalPoints" label="总分" width="90" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="openExamDetail(row)">详情</el-button>
              <el-button type="primary" link :disabled="row.status !== 1" @click.stop="handlePublish(row)">发布</el-button>
              <el-button type="primary" link @click.stop="openSubmitList(row)">查看已提交答卷</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!teacherExams.length" description="暂无考试" />
      </template>

      <template v-else>
        <div class="student-toolbar">
          <el-select v-model="selectedCourseId" placeholder="选择课程" style="width: 240px;" @change="loadStudentExams">
            <el-option label="全部课程" :value="0" />
            <el-option v-for="course in studentCourses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </div>
        <el-table :data="studentExams" size="small" @row-click="handleStudentRowClick">
          <el-table-column prop="title" label="考试标题" min-width="200" />
          <el-table-column prop="courseName" label="课程" min-width="160" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTag(row)">{{ statusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="180">
            <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column prop="totalPoints" label="总分" width="90" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" link :disabled="!canStudentEnter(row)" @click.stop="openExamDetail(row)">{{ studentActionText(row) }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!studentExams.length" description="暂无考试" />
      </template>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="创建考试" width="900px">
      <el-form label-width="100px" :model="createForm">
        <el-form-item label="课程">
          <el-select v-model="createForm.courseId" placeholder="选择课程" style="width: 320px;" @change="handleCourseChange">
            <el-option v-for="course in teacherCourses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="请输入考试标题" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="考试说明" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="createForm.startTime" type="datetime" placeholder="开始时间" style="width: 200px" />
          <span style="margin: 0 8px">-</span>
          <el-date-picker v-model="createForm.endTime" type="datetime" placeholder="截止时间" style="width: 200px" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="createForm.duration" :min="0" />
          <span style="margin-left: 8px">分钟(0表示不限制)</span>
        </el-form-item>
        <el-form-item label="总分">
          <el-text>自动计算：{{ calculatedTotalPoints }} 分</el-text>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="createForm.classIds" multiple placeholder="限制班级(可选)" style="width: 420px;">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>

        <el-divider>题目列表</el-divider>
        <div v-for="(q, index) in createForm.questions" :key="index" class="question-item">
          <div class="question-header">
            <strong>题目 {{ index + 1 }}</strong>
            <el-button type="danger" link @click="removeQuestion(index)">删除</el-button>
          </div>
          <el-form-item label="题型">
            <el-select v-model="q.type" style="width: 200px;">
              <el-option :value="1" label="单选" />
              <el-option :value="2" label="多选" />
              <el-option :value="3" label="填空" />
              <el-option :value="4" label="简答" />
            </el-select>
          </el-form-item>
          <el-form-item label="题干">
            <el-input v-model="q.content" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="q.points" :min="0" />
          </el-form-item>
          <template v-if="q.type === 1 || q.type === 2">
            <el-form-item label="选项">
              <div class="option-list">
                <div v-for="(opt, optIndex) in q.options" :key="optIndex" class="option-row">
                  <el-input v-model="opt.label" placeholder="A" style="width: 80px" />
                  <el-input v-model="opt.content" placeholder="选项内容" style="flex: 1" />
                  <el-button type="danger" link @click="removeOption(q, optIndex)">删除</el-button>
                </div>
                <el-button size="small" @click="addOption(q)">添加选项</el-button>
              </div>
            </el-form-item>
            <el-form-item label="答案">
              <el-input v-model="q.correctAnswer" placeholder="单选示例: A 多选示例: A,B" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="参考答案">
              <el-input v-model="q.correctAnswer" placeholder="可选" />
            </el-form-item>
            <el-form-item label="解析">
              <el-input v-model="q.explanation" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
          </template>
        </div>
        <el-button size="small" @click="addQuestion">添加题目</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateExam">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="85%" :with-header="false">
      <div class="exam-detail" v-if="currentExamDetail">
        <div class="detail-header">
          <div>
            <h2>{{ currentExamDetail.exam.title }}</h2>
            <div class="detail-meta">
              <el-tag :type="statusTag(currentExamDetail.exam)">{{ statusText(currentExamDetail.exam) }}</el-tag>
              <span>课程：{{ currentExamDetail.exam.courseName }}</span>
              <span>总分：{{ currentExamDetail.exam.totalPoints }}</span>
              <span v-if="currentExamDetail.exam.endTime">截止：{{ formatTime(currentExamDetail.exam.endTime) }}</span>
            </div>
          </div>
          <div class="detail-actions">
            <el-button v-if="!authStore.isTeacher" type="primary" :loading="starting" :disabled="isMySubmitLocked || !canStudentEnter(currentExamDetail.exam)" @click="startCurrentExam">开始考试</el-button>
            <el-button @click="detailVisible = false">关闭</el-button>
          </div>
        </div>

        <el-card shadow="never">
          <div class="detail-desc">{{ currentExamDetail.exam.description || '暂无说明' }}</div>
        </el-card>

        <el-card shadow="never" class="question-card">
          <div v-for="(q, index) in currentExamDetail.questions" :key="q.id" class="question-block">
            <div class="question-title">{{ index + 1 }}. {{ q.content }} ({{ q.points || 0 }}分)</div>
            <div v-if="authStore.isTeacher" class="teacher-answer">
              <div v-if="q.options?.length" class="option-preview">
                <div v-for="opt in q.options" :key="opt.label">{{ opt.label }}. {{ opt.content }}</div>
              </div>
              <div v-if="q.correctAnswer">答案：{{ q.correctAnswer }}</div>
              <div v-if="q.explanation">解析：{{ q.explanation }}</div>
            </div>
            <div v-else class="student-answer">
              <template v-if="q.type === 1">
                <el-radio-group v-model="studentAnswers[q.id]" :disabled="isMySubmitLocked">
                  <el-radio v-for="opt in q.options" :key="opt.label" :label="opt.label">{{ opt.label }}. {{ opt.content }}</el-radio>
                </el-radio-group>
              </template>
              <template v-else-if="q.type === 2">
                <el-checkbox-group v-model="multiAnswers[q.id]" :disabled="isMySubmitLocked">
                  <el-checkbox v-for="opt in q.options" :key="opt.label" :label="opt.label">{{ opt.label }}. {{ opt.content }}</el-checkbox>
                </el-checkbox-group>
              </template>
              <template v-else-if="q.type === 3">
                <el-input v-model="studentAnswers[q.id]" :disabled="isMySubmitLocked" placeholder="请输入答案" />
              </template>
              <template v-else>
                <el-input v-model="studentAnswers[q.id]" :disabled="isMySubmitLocked" type="textarea" :rows="3" placeholder="请输入答案" />
              </template>
              <div v-if="getSubmitAnswer(q.id)?.feedback" class="feedback-box">教师评语：{{ getSubmitAnswer(q.id)?.feedback }}</div>
            </div>
          </div>
        </el-card>

        <div v-if="!authStore.isTeacher" class="student-actions">
          <el-button :loading="saving" :disabled="isMySubmitLocked" @click="saveProgress">保存进度</el-button>
          <el-button type="primary" :loading="submitting" :disabled="isMySubmitLocked" @click="submitCurrentExam">提交考试</el-button>
          <div v-if="mySubmit" class="submit-info">
            <span>已提交：{{ mySubmit.submitTime ? formatTime(mySubmit.submitTime) : '-' }}</span>
            <span>总分：{{ mySubmit.totalScore ?? '-' }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="submitListVisible" size="80%" :with-header="false">
      <div class="submit-list">
        <div class="detail-header">
          <div>
            <h2>考试提交</h2>
            <div class="detail-meta">
              <span>{{ selectedExam?.title }}</span>
              <span>提交数：{{ submitList.length }}</span>
            </div>
          </div>
          <el-button @click="submitListVisible = false">关闭</el-button>
        </div>
        <el-table :data="submitList" size="small" @row-click="openSubmitDetail">
          <el-table-column prop="userName" label="学生" min-width="120" />
          <el-table-column prop="submitTime" label="提交时间" width="180">
            <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 3 ? 'success' : 'warning'">{{ row.status === 3 ? '已批改' : '待批改' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!submitList.length" description="暂无提交" />
      </div>
    </el-drawer>

    <el-drawer v-model="submitDetailVisible" size="90%" :with-header="false">
      <div v-if="submitDetail" class="submit-detail">
        <div class="detail-header">
          <div>
            <h2>阅卷：{{ submitDetail.userName }}</h2>
            <div class="detail-meta">
              <span>总分：{{ submitDetail.totalScore ?? '-' }}</span>
            </div>
          </div>
          <div>
            <el-button @click="submitDetailVisible = false">关闭</el-button>
            <el-button type="primary" :loading="grading" :disabled="!hasSubjectiveAnswers" @click="submitGrade">提交评分</el-button>
          </div>
        </div>
        <el-card shadow="never" class="question-card">
          <div v-for="answer in submitDetail.answers" :key="answer.id" class="question-block">
            <div class="question-title">{{ answer.questionContent }} ({{ answer.questionPoints || 0 }}分)</div>
            <div class="student-answer">学生答案：{{ answer.content || '-' }}</div>
            <div class="score-row">
              <el-input-number v-if="answer.questionType === 4" v-model="gradeMap[answer.id]" :min="0" :max="answer.questionPoints || 0" />
              <span v-else>客观题得分：{{ answer.score ?? 0 }}</span>
              <el-button v-if="answer.questionType === 4" size="small" plain :loading="aiGradingId === answer.id" @click="requestAiGrade(answer)">AI评分建议</el-button>
            </div>
            <el-input v-if="answer.questionType === 4" v-model="feedbackMap[answer.id]" type="textarea" :rows="2" placeholder="反馈(可选)" />
            <el-alert v-if="aiSuggestionMap[answer.id]" type="info" :closable="false" show-icon :title="`AI建议得分：${aiSuggestionMap[answer.id].suggestedScore}`">
              <template #default>
                <div>短评：{{ aiSuggestionMap[answer.id].feedback || '-' }}</div>
                <div>要点：{{ aiSuggestionMap[answer.id].criteriaSummary || '-' }}</div>
                <el-button size="small" type="success" plain @click="applyAiSuggestion(answer.id)">应用建议</el-button>
              </template>
            </el-alert>
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { getCourseList, getMyCourses, getCourseById, type Course } from '../api/course'
import {
  createExam,
  publishExam,
  getTeacherExams,
  getStudentExams,
  getExamDetail,
  startExam,
  saveExamProgress,
  submitExam,
  getMyExamSubmit,
  getExamSubmits,
  getExamSubmitDetail,
  gradeExam,
  aiGradeExamAnswer,
  type Exam,
  type ExamDetail,
  type ExamSubmit,
  type ExamAnswer,
  type AiGradeSuggestionResponse
} from '../api/exam'

const authStore = useAuthStore()
const route = useRoute()

const loading = ref(false)
const creating = ref(false)
const starting = ref(false)
const saving = ref(false)
const submitting = ref(false)
const grading = ref(false)
const aiGradingId = ref<number | null>(null)

const teacherExams = ref<Exam[]>([])
const studentExams = ref<Exam[]>([])
const teacherCourses = ref<Course[]>([])
const studentCourses = ref<Course[]>([])
const selectedCourseId = ref(0)
const courseClasses = ref<Array<{ id: number; name: string }>>([])

const createDialogVisible = ref(false)
const detailVisible = ref(false)
const submitListVisible = ref(false)
const submitDetailVisible = ref(false)

const currentExamDetail = ref<ExamDetail | null>(null)
const selectedExam = ref<Exam | null>(null)

const submitList = ref<ExamSubmit[]>([])
const submitDetail = ref<ExamSubmit | null>(null)

const studentAnswers = reactive<Record<number, string>>({})
const multiAnswers = reactive<Record<number, string[]>>({})
const mySubmit = ref<ExamSubmit | null>(null)

const gradeMap = reactive<Record<number, number>>({})
const feedbackMap = reactive<Record<number, string>>({})
const aiSuggestionMap = reactive<Record<number, AiGradeSuggestionResponse>>({})

const isMySubmitLocked = computed(() => (mySubmit.value?.status ?? 0) >= 2)
const hasSubjectiveAnswers = computed(() =>
  (submitDetail.value?.answers || []).some((answer) => answer.questionType === 4)
)

const createForm = reactive({
  courseId: undefined as number | undefined,
  title: '',
  description: '',
  startTime: null as Date | null,
  endTime: null as Date | null,
  duration: 0,
  classIds: [] as number[],
  questions: [] as Array<{
    type: number
    content: string
    options: Array<{ label: string; content: string }>
    correctAnswer: string
    explanation: string
    points: number
  }>
})

const calculatedTotalPoints = computed(() =>
  (createForm.questions || []).reduce((sum, q) => sum + (q.points || 0), 0)
)

const resolveExamStatus = (exam?: Exam) => {
  if (!exam) return 0
  if (exam.status === 1) return 1
  const now = Date.now()
  const start = exam.startTime ? new Date(exam.startTime).getTime() : 0
  const end = exam.endTime ? new Date(exam.endTime).getTime() : Number.POSITIVE_INFINITY
  if (start && now < start) return 4
  if (now > end) return 3
  return 2
}

const statusText = (exam?: Exam) => {
  const status = resolveExamStatus(exam)
  if (status === 1) return '未发布'
  if (status === 4) return '暂未开始'
  if (status === 2) return '进行中'
  if (status === 3) return '已结束'
  return '未知'
}

const statusTag = (exam?: Exam) => {
  const status = resolveExamStatus(exam)
  if (status === 1 || status === 4) return 'info'
  if (status === 2) return 'success'
  if (status === 3) return 'warning'
  return ''
}

const canStudentEnter = (exam?: Exam) => resolveExamStatus(exam) === 2

const studentActionText = (exam?: Exam) => {
  const status = resolveExamStatus(exam)
  if (status === 4) return '暂未开始'
  if (status === 3) return '已结束'
  return '进入'
}

const handleStudentRowClick = (exam: Exam) => {
  if (!canStudentEnter(exam)) return
  openExamDetail(exam)
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const getErrorMessage = (error: any) => {
  return error?.response?.data?.message || error?.message || '操作失败，请稍后重试'
}

const showApiError = (error: any) => {
  ElMessage.error(getErrorMessage(error))
}

const resetStudentAnswerState = () => {
  Object.keys(studentAnswers).forEach((key) => delete studentAnswers[Number(key)])
  Object.keys(multiAnswers).forEach((key) => delete multiAnswers[Number(key)])
}

const openCreateDialog = () => {
  createDialogVisible.value = true
  if (!createForm.questions.length) {
    addQuestion()
  }
}

const addQuestion = () => {
  createForm.questions.push({
    type: 1,
    content: '',
    options: [{ label: 'A', content: '' }, { label: 'B', content: '' }],
    correctAnswer: '',
    explanation: '',
    points: 5
  })
}

const removeQuestion = (index: number) => {
  createForm.questions.splice(index, 1)
}

const addOption = (question: any) => {
  const nextLabel = String.fromCharCode(65 + question.options.length)
  question.options.push({ label: nextLabel, content: '' })
}

const removeOption = (question: any, index: number) => {
  question.options.splice(index, 1)
}

const handleCourseChange = async (courseId: number) => {
  if (!courseId) return
  const course = await getCourseById(courseId)
  courseClasses.value = (course.classIds || []).map((id, idx) => ({
    id,
    name: course.classNames?.[idx] || `班级${id}`
  }))
  createForm.classIds = course.classIds || []
}

const handleCreateExam = async () => {
  if (!createForm.courseId || !createForm.title || !createForm.questions.length) {
    ElMessage.warning('请完善考试信息')
    return
  }
  creating.value = true
  try {
    await createExam({
      courseId: createForm.courseId,
      title: createForm.title,
      description: createForm.description,
      startTime: createForm.startTime ? createForm.startTime.toISOString() : undefined,
      endTime: createForm.endTime ? createForm.endTime.toISOString() : undefined,
      duration: createForm.duration || 0,
      totalPoints: calculatedTotalPoints.value,
      classIds: createForm.classIds,
      questions: createForm.questions.map((q, idx) => ({
        type: q.type,
        content: q.content,
        options: q.options,
        correctAnswer: q.correctAnswer,
        explanation: q.explanation,
        points: q.points,
        sortOrder: idx + 1
      }))
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    await loadTeacherExams()
  } catch (error) {
    showApiError(error)
  } finally {
    creating.value = false
  }
}

const openExamDetail = async (exam: Exam) => {
  try {
    selectedExam.value = exam
    resetStudentAnswerState()
    mySubmit.value = null
    currentExamDetail.value = await getExamDetail(exam.id)
    detailVisible.value = true
    if (!authStore.isTeacher) {
      await loadMySubmit(exam.id)
    }
  } catch (error) {
    showApiError(error)
  }
}

const startCurrentExam = async () => {
  if (!currentExamDetail.value) return
  starting.value = true
  try {
    await startExam(currentExamDetail.value.exam.id)
    ElMessage.success('已开始考试')
  } catch (error) {
    showApiError(error)
  } finally {
    starting.value = false
  }
}

const saveProgress = async () => {
  if (!currentExamDetail.value) return
  saving.value = true
  try {
    await saveExamProgress(currentExamDetail.value.exam.id, buildSubmitPayload())
    ElMessage.success('已保存')
  } catch (error) {
    showApiError(error)
  } finally {
    saving.value = false
  }
}

const submitCurrentExam = async () => {
  if (!currentExamDetail.value) return
  submitting.value = true
  try {
    await submitExam(currentExamDetail.value.exam.id, buildSubmitPayload())
    ElMessage.success('已提交')
    await loadMySubmit(currentExamDetail.value.exam.id)
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}

const buildSubmitPayload = () => {
  const answers = currentExamDetail.value?.questions.map((q) => {
    if (q.type === 2) {
      return { questionId: q.id, content: (multiAnswers[q.id] || []).join(',') }
    }
    return { questionId: q.id, content: studentAnswers[q.id] || '' }
  }) || []
  return { answers }
}

const loadMySubmit = async (examId: number) => {
  try {
    mySubmit.value = await getMyExamSubmit(examId)
    if (mySubmit.value?.answers?.length) {
      mySubmit.value.answers.forEach((answer) => {
        if (answer.questionType === 2) {
          multiAnswers[answer.questionId] = (answer.content || '').split(',').filter(Boolean)
        } else {
          studentAnswers[answer.questionId] = answer.content || ''
        }
      })
    }
  } catch (error) {
    showApiError(error)
  }
}

const handlePublish = async (exam: Exam) => {
  try {
    await publishExam(exam.id)
    ElMessage.success('已发布')
    await loadTeacherExams()
  } catch (error) {
    showApiError(error)
  }
}

const openSubmitList = async (exam: Exam) => {
  try {
    selectedExam.value = exam
    const res = await getExamSubmits(exam.id)
    submitList.value = res || []
    submitListVisible.value = true
  } catch (error) {
    showApiError(error)
  }
}

const openSubmitDetail = async (submit: ExamSubmit) => {
  try {
    submitDetail.value = await getExamSubmitDetail(submit.id)
    submitDetailVisible.value = true
    if (submitDetail.value?.answers) {
      submitDetail.value.answers.forEach((answer) => {
        gradeMap[answer.id] = answer.score || 0
        feedbackMap[answer.id] = answer.feedback || ''
      })
    }
  } catch (error) {
    showApiError(error)
  }
}

const submitGrade = async () => {
  if (!submitDetail.value || !selectedExam.value) return
  const subjectiveAnswers = (submitDetail.value.answers || []).filter((answer) => answer.questionType === 4)
  if (!subjectiveAnswers.length) {
    ElMessage.info('该答卷无主观题，无需提交评分')
    return
  }
  grading.value = true
  try {
    const payload = {
      answers: subjectiveAnswers
        .map((answer) => ({
          answerId: answer.id,
          score: gradeMap[answer.id] || 0,
          feedback: feedbackMap[answer.id],
          correct: (gradeMap[answer.id] || 0) > 0
        }))
    }
    await gradeExam(selectedExam.value.id, payload)
    ElMessage.success('评分已提交')
    submitDetailVisible.value = false
    await openSubmitList(selectedExam.value)
  } catch (error) {
    showApiError(error)
  } finally {
    grading.value = false
  }
}

const requestAiGrade = async (answer: ExamAnswer) => {
  aiGradingId.value = answer.id
  try {
    const items = await aiGradeExamAnswer(answer.id)
    const first = (items || []).find((item) => item.answerId === answer.id)?.suggestion
    if (!first) {
      ElMessage.warning('AI未返回有效评分建议，请稍后重试')
      return
    }
    aiSuggestionMap[answer.id] = first
  } catch (error) {
    showApiError(error)
  } finally {
    aiGradingId.value = null
  }
}

const getSubmitAnswer = (questionId: number) => {
  return (mySubmit.value?.answers || []).find((answer) => answer.questionId === questionId)
}

const applyAiSuggestion = (answerId: number) => {
  const suggestion = aiSuggestionMap[answerId]
  if (!suggestion) return
  gradeMap[answerId] = suggestion.suggestedScore || 0
  feedbackMap[answerId] = suggestion.feedback || ''
}

const loadTeacherExams = async () => {
  loading.value = true
  try {
    const res = await getTeacherExams()
    teacherExams.value = res?.records || []
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

const loadStudentExams = async () => {
  loading.value = true
  try {
    const res = await getStudentExams({ courseId: selectedCourseId.value || undefined })
    studentExams.value = res?.records || []
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  try {
    if (authStore.isTeacher) {
      const res = await getCourseList({ page: 1, size: 200 })
      teacherCourses.value = res?.records || []
    } else {
      const res = await getMyCourses()
      studentCourses.value = res || []
    }
  } catch (error) {
    showApiError(error)
  }
}

onMounted(async () => {
  await loadCourses()
  if (authStore.isTeacher) {
    await loadTeacherExams()
  } else {
    await loadStudentExams()
  }

  const examId = route.query.examId ? Number(route.query.examId) : 0
  if (examId) {
    const examList = authStore.isTeacher ? teacherExams.value : studentExams.value
    const match = examList.find((item) => item.id === examId)
    if (match && (authStore.isTeacher || canStudentEnter(match))) {
      openExamDetail(match)
    }
  }
})
</script>

<style scoped>
.exam-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-actions {
  margin-top: 12px;
}

.student-toolbar {
  margin-bottom: 12px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-meta {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.detail-desc {
  white-space: pre-wrap;
}

.question-card {
  margin-top: 16px;
}

.question-block {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.question-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.option-preview {
  margin-bottom: 8px;
}

.student-actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.submit-info {
  color: #666;
}

.question-item {
  padding: 12px 0;
  border-bottom: 1px dashed #ddd;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.score-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 8px 0;
}

.feedback-box {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f7fbff;
  border: 1px solid #d9ecff;
  color: #2c5a86;
}
</style>


