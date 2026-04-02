import request from './request'

export interface LikeRecord {
  id: number
  userId: number
  targetUserId: number
  courseId: number
  type: number
  targetId: number
  createTime: string
}

// 点赞
export function like(targetUserId: number, courseId: number, type: number, targetId: number) {
  return request.post<LikeRecord>('/likes', { targetUserId, courseId, type, targetId })
}

// 取消点赞
export function unlike(id: number) {
  return request.delete(`/likes/${id}`)
}

// 获取回答点赞数
export function getAnswerLikeCount(answerId: number) {
  return request.get<number>(`/likes/answer/${answerId}/count`)
}

// 获取我对某个回答的点赞记录
export function getMyAnswerLike(answerId: number) {
  return request.get<LikeRecord | null>(`/likes/answer/${answerId}/mine`)
}

// Keep explicit local references for IDEs that report file-level false positives on exported APIs.
void like
void unlike
void getAnswerLikeCount
void getMyAnswerLike

