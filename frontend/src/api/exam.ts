import request from './request'

export interface Exam {
  id: number
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
  title: string
  description: string
  startTime: string
  endTime: string
  duration: number
  totalPoints: number
  status: number
  createTime: string
  updateTime: string
}

export interface ExamQuestionOption {
  label: string
  content: string
}

export interface ExamQuestion {
  id: number
  examId: number
  type: number
  content: string
  options: ExamQuestionOption[]
  correctAnswer?: string
  explanation?: string
  points: number
  sortOrder: number
}

export interface ExamDetail {
  exam: Exam
  questions: ExamQuestion[]
}

export interface ExamAnswer {
  id: number
  submitId: number
  examId: number
  questionId: number
  questionContent: string
  questionType: number
  questionPoints: number
  content: string
  isCorrect: number
  score: number
  feedback: string
}

export interface ExamSubmit {
  id: number
  examId: number
  userId: number
  userName: string
  status: number
  submitTime: string
  autoSubmit: number
  totalScore: number
  objectiveScore: number
  subjectiveScore: number
  answers?: ExamAnswer[]
}

export interface ExamNotice {
  id: number
  examId: number
  courseId: number
  courseName: string
  title: string
  content: string
  createTime: string
}

export interface ExamCreateQuestion {
  type: number
  content: string
  options?: ExamQuestionOption[]
  correctAnswer?: string
  explanation?: string
  points?: number
  sortOrder?: number
}

export interface ExamCreateForm {
  courseId: number
  title: string
  description?: string
  startTime?: string
  endTime?: string
  duration?: number
  totalPoints?: number
  classIds?: number[]
  questions: ExamCreateQuestion[]
}

export interface ExamSubmitPayload {
  answers: Array<{ questionId: number; content: string }>
}

export interface ExamGradePayload {
  answers: Array<{ answerId: number; score: number; feedback?: string; correct?: boolean }>
}

export interface AiGradeSuggestionResponse {
  suggestedScore: number
  feedback: string
  criteriaSummary: string
  confidence: string
}

// Teacher
export function createExam(data: ExamCreateForm) {
  return request.post<Exam>('/exams', data)
}

export function publishExam(id: number) {
  return request.put<Exam>(`/exams/${id}/publish`)
}

export function getTeacherExams(params?: { page?: number; size?: number }) {
  return request.get<{ records: Exam[]; total: number }>('/exams/teacher', { params })
}

// Student
export function getStudentExams(params?: { page?: number; size?: number; courseId?: number }) {
  return request.get<{ records: Exam[]; total: number }>('/exams/student', { params })
}

export function getExamDetail(id: number) {
  return request.get<ExamDetail>(`/exams/${id}`)
}

export function startExam(examId: number) {
  return request.post<ExamSubmit>(`/exam-submits/${examId}/start`)
}

export function saveExamProgress(examId: number, data: ExamSubmitPayload) {
  return request.put<ExamSubmit>(`/exam-submits/${examId}/progress`, data)
}

export function submitExam(examId: number, data: ExamSubmitPayload) {
  return request.post<ExamSubmit>(`/exam-submits/${examId}/submit`, data)
}

export function getMyExamSubmit(examId: number) {
  return request.get<ExamSubmit | null>(`/exam-submits/${examId}/my`)
}

export function getExamSubmits(examId: number) {
  return request.get<ExamSubmit[]>(`/exam-submits/exam/${examId}`)
}

export function getExamSubmitDetail(submitId: number) {
  return request.get<ExamSubmit>(`/exam-submits/detail/${submitId}`)
}

export function gradeExam(examId: number, data: ExamGradePayload) {
  return request.post(`/exam-submits/${examId}/grade`, data)
}

export function aiGradeExamAnswer(answerId: number) {
  return request.post<AiGradeSuggestionResponse>('/exam-submits/ai-grade', { answerId })
}

export function getExamNotices() {
  return request.get<ExamNotice[]>('/exam-notices/my')
}

