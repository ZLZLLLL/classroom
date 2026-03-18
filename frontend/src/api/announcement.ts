import request from './request'

export interface SystemAnnouncement {
  id: number
  title: string
  content: string
  publisherId: number
  publisherName?: string
  createTime: string
}

export function getRecentAnnouncements(size: number = 10) {
  return request.get<SystemAnnouncement[]>('/announcements/recent', { params: { size } })
}

