import request from './request'

export interface Course {
  id: number
  name: string
  description: string
  coverUrl: string
  teacherId: number
  teacherName: string
  classIds: number[]
  classNames: string[]
  studentCount: number
  createTime: string
}

export interface CourseForm {
  name: string
  description: string
  coverUrl?: string
  classIds: number[]
}

// 创建课程
export function createCourse(data: CourseForm) {
  return request.post<Course>('/courses', data)
}

// 获取课程列表
export function getCourseList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<{ records: Course[]; total: number; size: number; current: number; pages: number }>('/courses', { params })
}

// 获取课程详情
export function getCourseById(id: number) {
  return request.get<Course>(`/courses/${id}`)
}

// 更新课程
export function updateCourse(id: number, data: CourseForm) {
  return request.put<Course>(`/courses/${id}`, data)
}

// 删除课程
export function deleteCourse(id: number) {
  return request.delete(`/courses/${id}`)
}

// 添加班级到课程
export function addClassToCourse(courseId: number, classId: number) {
  return request.post(`/courses/${courseId}/classes`, classId)
}

// 从课程移除班级
export function removeClassFromCourse(courseId: number, classId: number) {
  return request.delete(`/courses/${courseId}/classes/${classId}`)
}

// 获取课程学生列表
export function getCourseStudents(courseId: number) {
  return request.get<any[]>(`/courses/${courseId}/students`)
}
