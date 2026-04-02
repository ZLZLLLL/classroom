import request from './request'

export interface AiChatResponse {
  sessionId: string
  question: string
  answer: string
}

export interface AiChatSession {
  sessionId: string
  title: string
  lastQuestion: string
  lastAnswer: string
  lastTime: string
  messageCount: number
}

export interface AiChatMessage {
  id: string
  sessionId: string
  question: string
  answer: string
  createdAt: string
}

// AI智能问答
export function aiChat(question: string, sessionId?: string) {
  return request.post<AiChatResponse>('/ai/chat', { question, sessionId })
}

// AI会话列表
export function aiGetSessions() {
  return request.get<AiChatSession[]>('/ai/sessions')
}

// AI会话消息
export function aiGetSessionMessages(sessionId: string) {
  return request.get<AiChatMessage[]>(`/ai/sessions/${sessionId}/messages`)
}

// Keep explicit local references for IDEs that report file-level false positives on exported APIs.
void aiGetSessions
void aiGetSessionMessages

// AI知识点解析
export function aiExplain(topic: string) {
  return request.post<{ topic: string; explanation: string }>('/ai/explain', { topic })
}

// AI作业辅导
export function aiHomeworkHelp(homeworkContent: string, question?: string) {
  return request.post<{ homeworkContent: string; question: string; help: string }>('/ai/homework-help', {
    homeworkContent,
    question: question || '请帮我理解这道题'
  })
}

// AI学习建议
export function aiLearningAdvice() {
  return request.get<{ userId: string; advice: string }>('/ai/learning-advice')
}
