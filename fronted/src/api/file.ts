import request from './request'

// 文件信息
export interface FileInfo {
  id: number
  fileName: string
  fileUrl: string
  fileSize: number
  courseId: number
  userId: number
  type: number // 1-课件 2-作业 3-共享资料
  createTime: string
}

// 上传文件
export function uploadFile(file: File, courseId?: number, type: number = 3) {
  const formData = new FormData()
  formData.append('file', file)
  if (courseId) formData.append('courseId', String(courseId))
  formData.append('type', String(type))

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
export function downloadFile(id: number) {
  return request.get<Blob>(`/files/${id}/download`, { responseType: 'blob' })
}

// 删除文件
export function deleteFile(id: number) {
  return request.delete(`/files/${id}`)
}
