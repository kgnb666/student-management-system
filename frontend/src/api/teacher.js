import request from './request'

export function getMyCourses() {
  return request.get('/teacher/courses')
}

export function getCourseStudents(courseId) {
  return request.get(`/teacher/courses/${courseId}/students`)
}

export function getCourseStatistics(courseId, classId) {
  return request.get(`/teacher/courses/${courseId}/statistics`, {
    params: { classId }
  })
}

export function saveScores(courseId, scores) {
  return request.post('/teacher/scores/batch', { courseId, scores })
}

export function submitCourseScores(courseId) {
  return request.post(`/teacher/courses/${courseId}/submit`)
}

export function exportCourseScores(courseId) {
  return request.get(`/teacher/courses/${courseId}/scores/export`, { responseType: 'blob' })
}

export function importCourseScores(courseId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/teacher/courses/${courseId}/scores/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
}

export function getCourseScoreLogs(courseId, params) {
  return request.get(`/teacher/courses/${courseId}/score-logs`, { params })
}
