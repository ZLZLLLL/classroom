import request from './request'

// 点赞
export function like(targetUserId: number, courseId: number, type: number, targetId: number) {
  return request.post<any>('/likes', { targetUserId, courseId, type, targetId })
}

// 取消点赞
export function unlike(id: number) {
  return request.delete(`/likes/${id}`)
}

// 获取回答点赞数
export function getAnswerLikeCount(answerId: number) {
  return request.get<number>(`/likes/answer/${answerId}/count`)
}
