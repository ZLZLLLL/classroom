import request from './request'
import axios from 'axios'

// 文件信息
export interface FileInfo {
  id: number
  fileName: string
  // 后端当前返回字段为 filePath；fileUrl 为可直接访问链接
  filePath: string
  fileUrl?: string
  fileSize: number
  courseId: number
  userId: number
  type: number // 1-课件 2-作业 3-共享资料
  createTime: string
}

export type UploadCategory = 'materials' | 'homework-submit' | 'course-cover' | 'avatar' | 'other'

// 上传文件
export function uploadFile(
  file: File,
  options?: {
    courseId?: number
    type?: number
    category?: UploadCategory
    persist?: boolean
  }
) {
  const formData = new FormData()
  formData.append('file', file)
  if (options?.courseId) formData.append('courseId', String(options.courseId))
  formData.append('type', String(options?.type ?? 3))
  formData.append('category', options?.category || 'materials')
  formData.append('persist', String(options?.persist ?? true))


  return request.post<FileInfo>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取课程文件列表
export function getCourseFiles(courseId: number, type?: number) {
  return request.get<FileInfo[]>(`/files/course/${courseId}`, { params: { type } })
}

// 获取文件信息
export function getFileById(id: number) {
  return request.get<FileInfo>(`/files/${id}`)
}

// 下载文件
export async function downloadFile(id: number, fileName?: string) {
  const token = localStorage.getItem('token')
  const res = await axios.get(`/api/v1/files/${id}/download`, {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined
  })

  const blobUrl = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = fileName || 'download'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(blobUrl)
}

// 预览文件
export async function previewFile(id: number) {
  const token = localStorage.getItem('token')
  const res = await axios.get(`/api/v1/files/${id}/preview`, {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined
  })

  const blobUrl = URL.createObjectURL(res.data)
  window.open(blobUrl, '_blank')
}



