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

export interface ManageUserParams {
  page?: number
  size?: number
  role?: number
  keyword?: string
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

// 管理员获取用户列表（教师/学生）
export function getManageableUsers(params: ManageUserParams) {
  return request.get<PageResult<UserInfo>>('/users/manage', { params })
}

// 管理员重置密码
export function resetUserPassword(id: number, newPassword: string) {
  return request.post(`/users/${id}/reset-password`, { newPassword })
}

// 管理员封禁/解封用户
export function updateUserStatus(id: number, status: 0 | 1) {
  return request.put(`/users/${id}/status`, { status })
}

