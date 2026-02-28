import request from './request'

// 作业
export interface Homework {
  id: number
  courseId: number
  courseName: string
  title: string
  content: string
  chapter: string
  deadline: string
  totalPoints: number
  submitCount: number
  createTime: string
}

// 作业提交
export interface HomeworkSubmit {
  id: number
  homeworkId: number
  userId: number
  userName: string
  content: string
  filePath: string
  score: number
  feedback: string
  status: number // 1-待批改 2-已批改 3-逾期
  submitTime: string
  gradeTime: string
}

// 创建作业
export function createHomework(data: {
  courseId: number
  title: string
  content: string
  chapter?: string
  deadline: string
  totalPoints: number
  classIds?: number[]
}) {
  return request.post<Homework>('/homeworks', data)
}

// 获取课程作业列表
export function getCourseHomeworks(courseId: number, params?: { page?: number; size?: number }) {
  return request.get<{ records: Homework[]; total: number }>(`/homeworks/course/${courseId}`, { params })
}

// 获取教师所有作业
export function getTeacherHomeworks(params?: { page?: number; size?: number }) {
  return request.get<{ records: Homework[]; total: number }>('/homeworks/teacher', { params })
}

// 获取作业详情
export function getHomeworkById(id: number) {
  return request.get<Homework>(`/homeworks/${id}`)
}

// 更新作业
export function updateHomework(id: number, data: any) {
  return request.put<Homework>(`/homeworks/${id}`, data)
}

// 删除作业
export function deleteHomework(id: number) {
  return request.delete(`/homeworks/${id}`)
}

// 提交作业
export function submitHomework(homeworkId: number, content: string, filePath?: string) {
  return request.post<HomeworkSubmit>(`/homework-submits/homework/${homeworkId}`, { content, filePath })
}

// 获取作业提交列表(教师)
export function getHomeworkSubmits(homeworkId: number) {
  return request.get<HomeworkSubmit[]>(`/homework-submits/homework/${homeworkId}`)
}

// 获取我的提交
export function getMyHomeworkSubmit(homeworkId: number) {
  return request.get<HomeworkSubmit | null>(`/homework-submits/homework/${homeworkId}/my`)
}

// 批改作业
export function gradeHomework(submitId: number, score: number, feedback: string) {
  return request.put<HomeworkSubmit>(`/homework-submits/${submitId}/grade`, { score, feedback })
}
