import request from './request'

export function getProfile() {
  return request.get('/student/profile')
}

export function getSemesters() {
  return request.get('/student/semesters')
}

export function getMyCourses(params) {
  return request.get('/student/courses', { params })
}

export function getMyScores(params) {
  return request.get('/student/scores', { params })
}

export function getMyStatistics(params) {
  return request.get('/student/statistics', { params })
}

export function getMyGpa(params) {
  return request.get('/student/gpa', { params })
}
