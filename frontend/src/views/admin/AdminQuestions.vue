<template>
  <div class="admin-questions-page page-full">
    <div class="page-header">
      <h2>问题管理</h2>
      <p class="desc">默认仅展示问题，点击后查看回答详情</p>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-table :data="questions" stripe>
        <el-table-column prop="courseName" label="课程" min-width="140" />
        <el-table-column prop="content" label="问题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ getTypeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="answerCount" label="回答数" width="100" />
        <el-table-column prop="createTime" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="showDetail" title="问题详情" size="55%">
      <div v-if="detail" class="detail-wrap">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="课程">{{ detail.courseName }}</el-descriptions-item>
          <el-descriptions-item label="问题">{{ detail.content }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ getTypeText(detail.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status === 1 ? '进行中' : '已结束' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="answer-block">
          <h4>回答详情</h4>
          <el-table :data="answers" v-loading="detailLoading" max-height="420">
            <el-table-column label="学生" min-width="180">
              <template #default="{ row }">{{ formatStudentDisplay(row) }}</template>
            </el-table-column>
            <el-table-column prop="content" label="回答" min-width="240" show-overflow-tooltip />
            <el-table-column prop="score" label="得分" width="90" />
            <el-table-column prop="likeCount" label="点赞数" width="90" />
            <el-table-column prop="createTime" label="回答时间" min-width="160">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminQuestions, getQuestionById } from '@/api/question'
import { getQuestionAnswers } from '@/api/answer'
import { getAnswerLikeCount } from '@/api/like'

const loading = ref(false)
const detailLoading = ref(false)
const questions = ref<any[]>([])
const showDetail = ref(false)
const detail = ref<any>(null)
const answers = ref<any[]>([])

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

const getTypeText = (type: number) => {
  if (type === 1) return '单选'
  if (type === 2) return '多选'
  if (type === 3) return '填空'
  if (type === 4) return '简答'
  return '-'
}

const formatStudentDisplay = (row: any) => {
  const no = row.studentNo || ''
  const name = row.realName || row.userName || '-'
  return no ? `${no} ${name}` : name
}

const fetchQuestions = async () => {
  loading.value = true
  try {
    questions.value = await getAdminQuestions()
  } catch {
    questions.value = []
  } finally {
    loading.value = false
  }
}

const openDetail = async (questionId: number) => {
  showDetail.value = true
  detailLoading.value = true
  try {
    const [question, answerList] = await Promise.all([
      getQuestionById(questionId),
      getQuestionAnswers(questionId)
    ])
    detail.value = question
    answers.value = await Promise.all((answerList || []).map(async (item: any) => ({
      ...item,
      likeCount: await getAnswerLikeCount(item.id)
    })))
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(fetchQuestions)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 24px; color: #3d3225; }
.desc { margin: 0; color: #8b7355; font-size: 14px; }
.answer-block { margin-top: 16px; }
.answer-block h4 { margin: 0 0 10px; color: #3d3225; }
</style>

