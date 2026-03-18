import request from './request'

// 本课程下我的积分总分（从明细聚合）
export async function getCourseMyPointsTotal(courseId: number) {
  const records = await getCourseMyPointsRecords(courseId)
  return records.reduce((sum: number, r: any) => sum + (Number(r.points) || 0), 0)
}

// 本课程下我的积分明细
export function getCourseMyPointsRecords(courseId: number) {
  return request.get<any[]>(`/points/course/${courseId}/my/records`)
}

