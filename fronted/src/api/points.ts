import request from './request'

// 积分排行记录
export interface PointsRankingRecord {
  userId: number
  userName: string
  realName: string
  avatar: string
  points: number
}

// 获取我的积分
export function getMyPoints() {
  return request.get<number>('/points/my')
}

// 获取课程积分排行
export function getCoursePointsRanking(courseId: number) {
  return request.get<PointsRankingRecord[]>(`/points/course/${courseId}/ranking`)
}

// 教师手动给学生加分
export function addPointsForUsers(data: {
  userIds: number[]
  courseId: number
  points: number
  description: string
}) {
  return request.post<void>('/points/add', data)
}
