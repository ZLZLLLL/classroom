import request from './request'

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  realName?: string
  studentNo?: string
  role: number
  classId?: number
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  studentNo?: string
  role: number
  classId?: number
  className?: string
  avatar?: string
  phone?: string
  email?: string
  status: number
}

export interface LoginResult {
  token: string
  user: UserInfo
}

// 登录
export function login(data: LoginForm) {
  return request.post<LoginResult>('/auth/login', data)
}

// 注册
export function register(data: RegisterForm) {
  return request.post<UserInfo>('/auth/register', data)
}
