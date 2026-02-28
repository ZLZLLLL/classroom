import request from './request'

// 签到请求体
export interface SignInForm {
  courseId: number
  activityId: number
  latitude?: number
  longitude?: number
}

// 教师发起签到
export interface CreateAttendanceForm {
  courseId: number
  duration: number
  location?: string
  latitude?: number
  longitude?: number
  classIds?: number[]
}

// 签到活动
export interface AttendanceActivity {
  id: number
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
  duration: number
  location: string
  status: number
  createTime: string
  totalStudents: number
  signedCount: number
  unsignedCount: number
}

// 签到记录
export interface AttendanceRecord {
  id: number
  courseId: number
  courseName: string
  userId: number
  userName: string
  signInTime: string
  status: number
  latitude?: number
  longitude?: number
}

// 签到统计
export interface AttendanceStatistics {
  total: number
  signed: number
  absent: number
  rate: number
}

// 签到活动详情
export interface AttendanceActivityDetail extends AttendanceActivity {
  signedStudents: {
    userId: number
    userName: string
    realName: string
    signInTime: string
  }[]
  unsignedStudents: {
    userId: number
    userName: string
    realName: string
  }[]
}

// 学生签到
export function signIn(data: SignInForm) {
  return request.post<AttendanceRecord>('/attendance/signIn', data)
}

// 教师发起签到
export function createAttendance(data: CreateAttendanceForm) {
  return request.post<AttendanceActivity>('/attendance/create', data)
}

// 获取课程签到活动列表
export function getCourseActivities(courseId: number) {
  return request.get<AttendanceActivity[]>(`/attendance/course/${courseId}/activities`)
}

// 获取签到活动详情
export function getActivityDetails(activityId: number) {
  return request.get<AttendanceActivityDetail>(`/attendance/activity/${activityId}/details`)
}

// 获取学生待签到活动
export function getStudentActivities() {
  return request.get<AttendanceActivity[]>('/attendance/student/activities')
}

// 获取今日签到状态
export function getTodayAttendance(courseId: number) {
  return request.get<any>(`/attendance/course/${courseId}/today`)
}

// 获取签到统计
export function getAttendanceStatistics(courseId: number) {
  return request.get<AttendanceStatistics>(`/attendance/course/${courseId}/statistics`)
}

// 获取所有签到统计
export function getAllAttendanceStatistics() {
  return request.get<any[]>('/attendance/statistics')
}
