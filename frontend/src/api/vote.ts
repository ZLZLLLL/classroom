import request from './request'

export interface VoteOption {
  key: string
  content: string
  voteCount: number
  percentage: number
  voterNames?: string[]
}

export interface Vote {
  id: number
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
  title: string
  status: number
  type: number
  anonymous: number
  options: VoteOption[]
  totalVotes: number
  myOption?: string
  myOptions?: string[]
  createTime: string
  updateTime: string
}

export interface VoteCreateForm {
  courseId: number
  title: string
  classIds?: number[]
  type?: number
  anonymous?: boolean
  options: Array<{ key?: string; content: string }>
}

export function createVote(data: VoteCreateForm) {
  return request.post<Vote>('/votes', data)
}

export function getCourseVotes(courseId: number) {
  return request.get<Vote[]>(`/votes/course/${courseId}`)
}

export function submitVote(voteId: number, optionKey: string | string[]) {
  if (Array.isArray(optionKey)) {
    return request.post(`/votes/${voteId}/submit`, { optionKeys: optionKey })
  }
  return request.post(`/votes/${voteId}/submit`, { optionKey })
}

export function closeVote(voteId: number) {
  return request.put(`/votes/${voteId}/close`)
}


