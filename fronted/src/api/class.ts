import request from './request'

export interface ClassInfo {
  id: number
  name: string
  grade: string
  major: string
  description: string
  studentCount: number
  createTime: string
}

// 创建班级
export function createClass(data: { name: string; grade: string; major: string; description?: string }) {
  return request.post<ClassInfo>('/classes', data)
}

// 获取班级列表
export function getClassList() {
  return request.get<ClassInfo[]>('/classes')
}

// 获取班级详情
export function getClassById(id: number) {
  return request.get<ClassInfo>(`/classes/${id}`)
}

// 更新班级
export function updateClass(id: number, data: { name: string; grade: string; major: string; description?: string }) {
  return request.put<ClassInfo>(`/classes/${id}`, data)
}

// 删除班级
export function deleteClass(id: number) {
  return request.delete(`/classes/${id}`)
}
