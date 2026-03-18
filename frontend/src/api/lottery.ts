import request from './request'

// 随机点名：返回被抽中的学生列表
export function drawLottery(courseId: number, count: number) {
  return request.post<any[]>(`/lottery/course/${courseId}/draw`, { count })
}

