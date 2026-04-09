import request from './request'

export interface AiChatResponse {
  sessionId: string
  question: string
  answer: string
}

export interface AiChatStreamHandlers {
  onInit?: (payload: { sessionId: string; question: string }) => void
  onChunk?: (chunk: string) => void
  onDone?: (payload: AiChatResponse) => void
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

export async function aiChatStream(question: string, sessionId: string | undefined, handlers: AiChatStreamHandlers = {}) {
  const token = localStorage.getItem('token') || ''
  const response = await fetch('/api/v1/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({ question, sessionId })
  })

  if (!response.ok || !response.body) {
    throw new Error('流式请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalPayload: AiChatResponse | null = null

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

    let eventEnd = buffer.indexOf('\n\n')
    while (eventEnd >= 0) {
      const rawEvent = buffer.slice(0, eventEnd)
      buffer = buffer.slice(eventEnd + 2)
      eventEnd = buffer.indexOf('\n\n')

      const lines = rawEvent.split('\n')
      let eventName = 'message'
      const dataLines: string[] = []

      lines.forEach((line) => {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim())
        }
      })

      if (!dataLines.length) {
        continue
      }

      const dataText = dataLines.join('')
      let payload: any
      try {
        payload = JSON.parse(dataText)
      } catch {
        payload = { content: dataText }
      }

      if (eventName === 'init') {
        handlers.onInit?.(payload)
      } else if (eventName === 'chunk') {
        handlers.onChunk?.(payload.content || '')
      } else if (eventName === 'done') {
        finalPayload = payload
        handlers.onDone?.(payload)
      } else if (eventName === 'error') {
        throw new Error(payload.message || 'AI流式调用失败')
      }
    }
  }

  if (!finalPayload) {
    throw new Error('AI响应中断，请重试')
  }
  return finalPayload
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
