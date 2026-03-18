<template>
  <div class="questions-page">
    <div class="page-header">
      <h2>{{ authStore.isTeacher ? '提问管理' : '课堂问答' }}</h2>
      <p class="desc">{{ authStore.isTeacher ? '发布和管理课堂提问' : '参与课堂互动问答' }}</p>
    </div>

    <!-- 学生端：选择课程 -->
    <div v-if="!authStore.isTeacher">
      <el-card shadow="never" class="course-select-card">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadQuestions" style="width: 300px;">
          <el-option label="全部课程" :value="0" />
          <el-option v-for="course in myCourses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
      </el-card>

      <el-card shadow="never" v-loading="loading">
        <div v-if="questions.length > 0" class="question-list">
          <div v-for="q in questions" :key="q.id" class="question-item">
            <div class="question-header">
              <span class="course-name">课程: {{ q.courseName || getCourseName(q.courseId) }}</span>
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

            <!-- 学生回答区域 -->
            <div v-if="q.status === 1" class="answer-section">
              <div v-if="!q.myAnswer">
                <div v-if="q.type === 1 || q.type === 2">
                  <el-radio-group v-if="q.type === 1" v-model="q.selectedAnswer">
                    <el-radio v-for="opt in q.options" :key="opt.label" :value="opt.label">
                      {{ opt.label }}. {{ opt.content }}
                    </el-radio>
                  </el-radio-group>
                  <el-checkbox-group v-else v-model="q.selectedAnswers">
                    <el-checkbox v-for="opt in q.options" :key="opt.label" :value="opt.label">
                      {{ opt.label }}. {{ opt.content }}
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
                <div v-else-if="q.type === 3">
                  <!-- 填空 -->
                  <el-input v-model="q.fillAnswer" placeholder="请输入答案" />
                </div>
                <div v-else-if="q.type === 4">
                  <!-- 简答 -->
                  <el-input v-model="q.fillAnswer" type="textarea" :rows="3" placeholder="请输入答案" />
                </div>
                <el-button type="primary" @click="submitAnswer(q)" style="margin-top: 10px;">提交答案</el-button>
              </div>
              <div v-else class="my-answer">
                <el-tag type="success">已回答</el-tag>
                <span>我的答案: {{ q.myAnswer }}</span>
              </div>
            </div>

            <!-- 回答统计 -->
            <div class="answer-stats">
              <span>回答人数: {{ q.answerCount || 0 }}</span>
              <span v-if="q.correctRate">正确率: {{ q.correctRate }}%</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无提问" />
      </el-card>
    </div>

    <!-- 教师端 -->
    <el-card v-else shadow="never" v-loading="loading">
      <div v-if="questions.length > 0" class="question-list">
        <div v-for="q in questions" :key="q.id" class="question-item">
          <div class="question-header">
            <span class="course-name">课程: {{ q.courseName || getCourseName(q.courseId) }}</span>
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
import { getTeacherQuestions, getActiveQuestion, submitAnswer as submitAnswerApi } from '../api/question'
import { getMyCourses } from '../api/course'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const questions = ref<any[]>([])
const myCourses = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)

const getCourseName = (courseId: number) => {
  const course = myCourses.value.find(c => c.id === courseId)
  return course?.name || `课程#${courseId}`
}

const normalizeQuestion = (q: any) => {
  const item = { ...q }
  if (item.options && typeof item.options === 'string') {
    try {
      item.options = JSON.parse(item.options)
    } catch {
      item.options = []
    }
  }
  if (!Array.isArray(item.options)) {
    item.options = []
  }

  item.selectedAnswer = ''
  item.selectedAnswers = []
  item.fillAnswer = ''

  if (item.myAnswer) {
    if (item.type === 1) {
      item.selectedAnswer = item.myAnswer
    } else if (item.type === 2) {
      item.selectedAnswers = String(item.myAnswer)
        .replace(/，/g, ',')
        .split(/[\s,]+/)
        .filter(Boolean)
    } else {
      item.fillAnswer = item.myAnswer
    }
  }

  return item
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadQuestions = async () => {
  if (!authStore.isTeacher && !selectedCourseId.value) {
    questions.value = []
    return
  }

  loading.value = true
  try {
    if (authStore.isTeacher) {
      const teacherQuestions = await getTeacherQuestions()
      questions.value = teacherQuestions.map((q: any) => normalizeQuestion(q))
    } else {
      // 学生获取进行中的提问
      if (selectedCourseId.value) {
        const res = await getActiveQuestion(selectedCourseId.value)
        if (res) {
          questions.value = [normalizeQuestion(res)]
        } else {
          questions.value = []
        }
      } else {
        questions.value = []
      }
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

const loadMyCourses = async () => {
  try {
    const res = await getMyCourses()
    myCourses.value = res || []
    // 加载完课程后自动加载提问
    if (!authStore.isTeacher && myCourses.value.length > 0) {
      selectedCourseId.value = myCourses.value[0].id
      loadQuestions()
    }
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const submitAnswer = async (q: any) => {
  const answer = q.type === 1
    ? (q.selectedAnswer || '')
    : q.type === 2
      ? (Array.isArray(q.selectedAnswers) ? q.selectedAnswers.join(',') : '')
      : (q.fillAnswer || '')

  if (!answer) {
    ElMessage.warning('请输入答案')
    return
  }

  try {
    await submitAnswerApi(q.id, answer)
    ElMessage.success('提交成功')
    q.myAnswer = answer
    q.answerCount = (q.answerCount || 0) + 1
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
}

onMounted(() => {
  if (authStore.isTeacher) {
    loadQuestions()
  } else {
    loadMyCourses()
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.course-select-card { margin-bottom: 16px; }

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

.answer-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.my-answer {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #67c23a;
}

.answer-stats {
  margin-top: 10px;
  font-size: 12px;
  color: #999;
}
</style>
