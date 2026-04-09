import request from './request'

// 回答
export interface Answer {
  id: number
  questionId: number
  userId: number
  studentNo?: string
  realName?: string
  userName: string
  userAvatar: string
  content: string
  isCorrect: number // 0-未评判 1-正确 2-错误
  likeCount: number
  createTime: string
}

// 学生回答
export function submitAnswer(questionId: number, content: string) {
  return request.post<Answer>('/answers', { questionId, content })
}

// 获取问题回答列表
export function getQuestionAnswers(questionId: number) {
  return request.get<Answer[]>(`/answers/question/${questionId}`)
}

// 获取我的回答
export function getMyAnswer(questionId: number) {
  return request.get<Answer | null>(`/answers/my/${questionId}`)
}

// 教师阅卷/打分
export function reviewAnswer(data: { answerId: number; correct: boolean; score: number }) {
  return request.post<Answer>('/answers/review', data)
}

// AI评分建议（简答题）
export interface AiGradeSuggestion {
  suggestedScore: number
  feedback: string
  criteriaSummary: string
  confidence: string
}

// AI评分建议（简答题）
export function suggestAnswerGrade(answerId: number) {
  return request.post<AiGradeSuggestion>('/answers/review-suggestion', { answerId })
}
