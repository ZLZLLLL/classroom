import request from './request'

// 问题选项
export interface QuestionOption {
  label: string
  content: string
}

// 问题
export interface Question {
  id: number
  courseId: number
  courseName: string
  content: string
  type: number // 1-单选 2-多选 3-填空 4-简答
  options: QuestionOption[]
  correctAnswer: string
  explanation: string
  points: number
  duration: number
  status: number // 1-进行中 2-已结束
  createTime: string
  endTime: string
  answerCount: number
  correctCount: number

  // Student-side answer state (backend myAnswer + frontend temporary fields)
  myAnswer?: string
  selectedAnswer?: string
  selectedAnswers?: string[]
  fillAnswer?: string
}

// 问题表单
export interface QuestionForm {
  courseId: number
  content: string
  type: number
  options?: QuestionOption[]
  correctAnswer?: string
  explanation?: string
  points?: number
  duration?: number
  classIds?: number[]
}

// 教师发起提问
export function createQuestion(data: QuestionForm) {
  return request.post<Question>('/questions', data)
}

// 获取当前进行中的提问
export function getActiveQuestion(courseId: number) {
  return request.get<Question | null>(`/questions/active/${courseId}`)
}

// 获取课程提问历史
export function getCourseQuestions(courseId: number) {
  return request.get<Question[]>(`/questions/course/${courseId}`)
}

// 获取教师所有提问
export function getTeacherQuestions() {
  return request.get<Question[]>('/questions/teacher')
}

export function getAdminQuestions() {
  return request.get<Question[]>('/questions/admin/all')
}

// 获取提问详情
export function getQuestionById(id: number) {
  return request.get<Question>(`/questions/${id}`)
}

// 结束提问
export function closeQuestion(id: number) {
  return request.put<Question>(`/questions/${id}/close`)
}

// 学生提交回答
export function submitAnswer(questionId: number, content: string) {
  return request.post<any>('/answers', { questionId, content })
}
