import request from './request'

// AI智能问答
export function aiChat(question: string) {
  return request.post<{ question: string; answer: string }>('/ai/chat', { question })
}

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
