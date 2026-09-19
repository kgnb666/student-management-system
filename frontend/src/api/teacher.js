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

export function getScores(params) {
  return request.get('/teacher/scores', { params })
}

export function saveScores(courseId, scores) {
  return request.post('/teacher/scores/batch', { courseId, scores })
}
