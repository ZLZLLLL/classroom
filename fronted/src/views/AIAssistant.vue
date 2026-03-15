<template>
  <div class="ai-assistant">
    <div class="page-header">
      <h2>AI 助手</h2>
      <p class="desc">智能问答、学习辅导</p>
    </div>

    <div class="ai-container">
      <el-card shadow="never" class="ai-card">
        <template #header>
          <div class="card-header">
            <el-icon :size="24" color="#d4a574"><ChatDotRound /></el-icon>
            <span>课堂小助手</span>
          </div>
        </template>

        <div class="ai-features">
          <div
            v-for="feature in features"
            :key="feature.key"
            class="feature-item"
            :class="{ active: activeFeature === feature.key }"
            @click="activeFeature = feature.key"
          >
            <el-icon :size="28"><component :is="feature.icon" /></el-icon>
            <span>{{ feature.label }}</span>
            <p>{{ feature.desc }}</p>
          </div>
        </div>

        <div v-if="activeFeature !== ''" class="ai-form">
          <template v-if="activeFeature === 'chat'">
            <el-input
              v-model="chatQuestion"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题..."
            />
            <el-button type="primary" :loading="loading" @click="handleChat">
              提问
            </el-button>
          </template>

          <template v-else-if="activeFeature === 'explain'">
            <el-input
              v-model="explainTopic"
              placeholder="请输入要了解的知识点..."
            />
            <el-button type="primary" :loading="loading" @click="handleExplain">
              解析
            </el-button>
          </template>

          <template v-else-if="activeFeature === 'homework'">
            <el-input
              v-model="homeworkContent"
              type="textarea"
              :rows="4"
              placeholder="请输入作业内容..."
            />
            <el-input
              v-model="homeworkQuestion"
              placeholder="请输入您的问题..."
              style="margin-top: 12px"
            />
            <el-button type="primary" :loading="loading" @click="handleHomework">
              获取帮助
            </el-button>
          </template>

          <template v-else-if="activeFeature === 'advice'">
            <el-button type="primary" :loading="loading" @click="handleAdvice">
              获取学习建议
            </el-button>
          </template>
        </div>

        <div v-if="answer" class="ai-answer">
          <div class="answer-label">回复：</div>
          <div class="answer-content">{{ answer }}</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ChatDotRound, QuestionFilled, Document, TrendCharts, ChatLineRound } from '@element-plus/icons-vue'
import { aiChat, aiExplain, aiHomeworkHelp, aiLearningAdvice } from '../api/ai'

const features = [
  { key: 'chat', label: '智能问答', icon: ChatLineRound, desc: '回答课程相关问题' },
  { key: 'explain', label: '知识点解析', icon: QuestionFilled, desc: '解释概念和原理' },
  { key: 'homework', label: '作业辅导', icon: Document, desc: '指导完成作业' },
  { key: 'advice', label: '学习建议', icon: TrendCharts, desc: '个性化学习指导' }
]

const activeFeature = ref('')
const loading = ref(false)
const answer = ref('')

const chatQuestion = ref('')
const explainTopic = ref('')
const homeworkContent = ref('')
const homeworkQuestion = ref('')

async function handleChat() {
  if (!chatQuestion.value.trim()) return
  loading.value = true
  answer.value = ''
  try {
    const res = await aiChat(chatQuestion.value)
    answer.value = res.answer
  } finally {
    loading.value = false
  }
}

async function handleExplain() {
  if (!explainTopic.value.trim()) return
  loading.value = true
  answer.value = ''
  try {
    const res = await aiExplain(explainTopic.value)
    answer.value = res.explanation
  } finally {
    loading.value = false
  }
}

async function handleHomework() {
  if (!homeworkContent.value.trim()) return
  loading.value = true
  answer.value = ''
  try {
    const res = await aiHomeworkHelp(homeworkContent.value, homeworkQuestion.value)
    answer.value = res.help
  } finally {
    loading.value = false
  }
}

async function handleAdvice() {
  loading.value = true
  answer.value = ''
  try {
    const res = await aiLearningAdvice()
    answer.value = res.advice
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-assistant { max-width: 900px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.ai-card { border-radius: 16px; }

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #3d3225;
}

.ai-features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.feature-item {
  padding: 20px 16px;
  background: #faf8f5;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.feature-item:hover { background: #f5f0e8; }
.feature-item.active {
  border-color: #d4a574;
  background: #fff;
}

.feature-item span {
  display: block;
  margin-top: 8px;
  font-weight: 500;
  color: #3d3225;
}

.feature-item p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8b7355;
}

.ai-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.ai-answer {
  padding: 16px;
  background: #faf8f5;
  border-radius: 12px;
  border-left: 4px solid #d4a574;
}

.answer-label {
  font-weight: 600;
  color: #3d3225;
  margin-bottom: 8px;
}

.answer-content {
  color: #5a4a3a;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
