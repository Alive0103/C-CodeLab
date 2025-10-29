import { http } from './http'

export function getUserProfile() {
  return http.get('/user/profile')
}

export function updateProfile(data: { email: string }) {
  return http.put('/user/profile', data)
}

export function changePassword(data: {
  oldPassword: string;
  newPassword: string;
}) {
  return http.put('/user/password', data)
}

export function getMyCodeSnippets(page: number = 0, size: number = 10) {
  return http.get(`/user/code-snippets?page=${page}&size=${size}`)
}

export function getMyExecutionRecords(page: number = 0, size: number = 10) {
  return http.get(`/user/execution-records?page=${page}&size=${size}`)
}

export function deleteCodeSnippet(id: number) {
  return http.delete(`/user/code-snippets/${id}`)
}

export function deleteExecutionRecord(id: number) {
  return http.delete(`/user/execution-records/${id}`)
}

export function saveCodeSnippet(title: string, code: string, language: string, isPublic: boolean) {
  return http.post('/code/save', {
    title,
    code,
    language,
    isPublic
  })
}

export function runCode(code: string, title: string) {
  return http.post('/code/run', {
    code,
    title
  })
}