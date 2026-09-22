import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getRegisterClasses() {
  return request.get('/auth/classes')
}

export function changePassword(data) {
  return request.post('/auth/change-password', data)
}
