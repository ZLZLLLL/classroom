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
            <div class="chat-layout">
              <div class="session-list">
                <div class="session-header">
                  <span>历史会话</span>
                  <el-button size="small" @click="startNewSession">新会话</el-button>
                </div>
                <el-scrollbar height="320px">
                  <div
                    v-for="session in chatSessions"
                    :key="session.sessionId"
                    class="session-item"
                    :class="{ active: currentSessionId === session.sessionId }"
                    @click="selectSession(session.sessionId)"
                  >
                    <div class="session-title">{{ session.title || '新会话' }}</div>
                    <div class="session-time">{{ formatTime(session.lastTime) }}</div>
                  </div>
                  <div v-if="chatSessions.length === 0" class="empty-tip">暂无历史会话</div>
                </el-scrollbar>
              </div>

              <div class="chat-main">
                <el-scrollbar ref="messageScrollbarRef" height="320px" class="message-list">
                  <div v-if="chatMessages.length === 0" class="empty-tip">开始新会话，向 AI 提问吧</div>
                  <div
                    v-for="item in chatMessages"
                    :key="item.id"
                    class="message-item"
                    :class="item.role"
                  >
                    <div class="bubble">
                      <div class="bubble-role">{{ item.role === 'user' ? '你' : 'AI' }}</div>
                      <div class="bubble-content">{{ item.content }}</div>
                    </div>
                  </div>
                </el-scrollbar>

                <el-input
                  v-model="chatQuestion"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入您的问题..."
                  @keydown="handleChatInputKeydown"
                />
                <div class="chat-actions">
                  <span class="input-tip">Enter 发送，Shift+Enter 换行</span>
                  <el-button type="primary" :loading="loading" @click="handleChat">发送</el-button>
                </div>
              </div>
            </div>
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

        <div v-if="answer && activeFeature !== 'chat'" class="ai-answer">
          <div class="answer-label">回复：</div>
          <div class="answer-content">{{ answer }}</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ChatDotRound, QuestionFilled, Document, TrendCharts, ChatLineRound } from '@element-plus/icons-vue'
import {
  aiChat,
  aiExplain,
  aiGetSessionMessages,
  aiGetSessions,
  aiHomeworkHelp,
  aiLearningAdvice,
  type AiChatMessage,
  type AiChatSession
} from '../api/ai'

const features = [
  { key: 'chat', label: '智能问答', icon: ChatLineRound, desc: '回答课程相关问题' },
  { key: 'explain', label: '知识点解析', icon: QuestionFilled, desc: '解释概念和原理' },
  { key: 'homework', label: '作业辅导', icon: Document, desc: '指导完成作业' },
  { key: 'advice', label: '学习建议', icon: TrendCharts, desc: '个性化学习指导' }
]

const activeFeature = ref('chat')
const loading = ref(false)
const answer = ref('')

const chatQuestion = ref('')
const explainTopic = ref('')
const homeworkContent = ref('')
const homeworkQuestion = ref('')
const chatSessions = ref<AiChatSession[]>([])
type ChatMessageItem = {
  id: string
  role: 'user' | 'assistant'
  content: string
}

const chatMessages = ref<ChatMessageItem[]>([])
const currentSessionId = ref('')
const messageScrollbarRef = ref<any>(null)

onMounted(async () => {
  await refreshSessions()
})

watch(activeFeature, async (feature) => {
  if (feature === 'chat') {
    await refreshSessions()
  }
})

async function refreshSessions() {
  const sessions = await aiGetSessions()
  chatSessions.value = sessions
  if (!currentSessionId.value && sessions.length > 0) {
    currentSessionId.value = sessions[0].sessionId
    await loadSessionMessages(currentSessionId.value)
  }
}

function startNewSession() {
  currentSessionId.value = ''
  chatMessages.value = []
  chatQuestion.value = ''
}

async function selectSession(sessionId: string) {
  currentSessionId.value = sessionId
  await loadSessionMessages(sessionId)
}

async function loadSessionMessages(sessionId: string) {
  if (!sessionId) {
    chatMessages.value = []
    return
  }
  const history = await aiGetSessionMessages(sessionId)
  const messages: ChatMessageItem[] = []
  history.forEach((item: AiChatMessage) => {
    messages.push({
      id: `${item.id}-q`,
      role: 'user',
      content: item.question || ''
    })
    messages.push({
      id: `${item.id}-a`,
      role: 'assistant',
      content: item.answer || ''
    })
  })
  chatMessages.value = messages
  scrollToBottom()
}

function handleChatInputKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    if (!loading.value) {
      handleChat()
    }
  }
}

function scrollToBottom() {
  setTimeout(() => {
    if (messageScrollbarRef.value?.setScrollTop) {
      messageScrollbarRef.value.setScrollTop(999999)
    }
  }, 0)
}

function formatTime(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function handleChat() {
  const question = chatQuestion.value.trim()
  if (!question) return

  loading.value = true
  answer.value = ''
  const userMessage: ChatMessageItem = {
    id: `${Date.now()}-u`,
    role: 'user',
    content: question
  }
  const pendingAssistant: ChatMessageItem = {
    id: `${Date.now()}-a`,
    role: 'assistant',
    content: '正在思考中...'
  }

  chatMessages.value.push(userMessage, pendingAssistant)
  chatQuestion.value = ''
  scrollToBottom()

  try {
    const res = await aiChat(question, currentSessionId.value || undefined)
    currentSessionId.value = res.sessionId

    const pending = chatMessages.value.find((m) => m.id === pendingAssistant.id)
    if (pending) {
      pending.content = res.answer
    }

    await refreshSessions()
    await loadSessionMessages(currentSessionId.value)
  } catch (e) {
    const pending = chatMessages.value.find((m) => m.id === pendingAssistant.id)
    if (pending) {
      pending.content = '请求失败，请稍后重试。'
    }
  } finally {
    loading.value = false
    scrollToBottom()
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

.chat-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
}

.session-list {
  border: 1px solid #eee2d2;
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #5a4a3a;
  margin-bottom: 8px;
}

.session-item {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  background: #faf8f5;
}

.session-item.active {
  background: #f5f0e8;
  border: 1px solid #d4a574;
}

.session-title {
  font-size: 13px;
  color: #3d3225;
}

.session-time {
  font-size: 12px;
  color: #8b7355;
}

.chat-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-list {
  border: 1px solid #eee2d2;
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.message-item {
  margin-bottom: 10px;
  display: flex;
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 80%;
  border-radius: 10px;
  padding: 10px 12px;
  background: #f7f3ee;
}

.message-item.user .bubble {
  background: #e8f3ff;
}

.bubble-role {
  font-size: 12px;
  font-weight: 600;
  color: #7a6a59;
  margin-bottom: 4px;
}

.bubble-content {
  color: #5a4a3a;
  white-space: pre-wrap;
}

.chat-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-tip {
  color: #9a8b7b;
  font-size: 12px;
}

.empty-tip {
  color: #9a8b7b;
  font-size: 12px;
  text-align: center;
  padding: 12px 0;
}

@media (max-width: 900px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
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
