import request from './request'

export interface AdminDashboardStats {
  totalRegistered: number
  teacherCount: number
  studentCount: number
  uptime: string
  version: string
}

export interface AnnouncementForm {
  title: string
  content: string
}

export function getAdminDashboardStats() {
  return request.get<AdminDashboardStats>('/admin/dashboard/stats')
}

export function publishSystemAnnouncement(data: AnnouncementForm) {
  return request.post('/admin/announcements', data)
}

