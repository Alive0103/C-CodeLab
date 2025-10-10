import { http } from './http'

// 用户代码历史记录相关API
export function getMyCodeSnippets(page = 0, size = 10) {
  return http.get(`/user/code-snippets?page=${page}&size=${size}`)
}

export function getMyExecutionRecords(page = 0, size = 10) {
  return http.get(`/user/execution-records?page=${page}&size=${size}`)
}

export function deleteCodeSnippet(id: number) {
  return http.delete(`/user/code-snippets/${id}`)
}

export function deleteExecutionRecord(id: number) {
  return http.delete(`/user/execution-records/${id}`)
}

// 用户信息管理相关API
export function getUserProfile() {
  return http.get('/user/profile')
}

export function updateProfile(data: { email?: string }) {
  return http.put('/user/profile', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.put('/user/password', data)
}
