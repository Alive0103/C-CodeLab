import { http } from './http'

export function saveSnippet(data: { title: string; codeContent: string; language: 'c'; isPublic: boolean }) {
  return http.post('/code/save', data)
}

export function runCode(data: { code: string; title?: string }) {
  return http.post('/code/run', data)
}

export function listResults() {
  return http.get('/result/list')
}


