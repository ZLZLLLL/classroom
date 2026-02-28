import request from './request'
import { UserInfo } from './auth'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserUpdateForm {
  realName?: string
  classId?: number
  avatar?: string
  phone?: string
  email?: string
}

// 获取当前用户信息
export function getCurrentUser() {
  return request.get<UserInfo>('/users/me')
}

// 更新当前用户信息
export function updateCurrentUser(data: UserUpdateForm) {
  return request.put<UserInfo>('/users/me', data)
}

// 获取指定用户信息
export function getUserById(id: number) {
  return request.get<UserInfo>(`/users/${id}`)
}

// 获取学生列表
export function getStudentList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<PageResult<UserInfo>>('/users/students', { params })
}

// 获取班级学生列表
export function getStudentsByClass(classId: number) {
  return request.get<UserInfo[]>(`/users/studentsByClass/${classId}`)
}
