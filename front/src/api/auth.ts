import { http } from './http'

export function login(data: { username: string; password: string }) {
  return http.post('/auth/login', data)
}

export function register(data: { username: string; password: string; email?: string }) {
  return http.post('/auth/register', data)
}

export function refreshToken() {
  return http.post('/auth/refresh')
}

export function getUser() {
  return http.get('/user')
}


