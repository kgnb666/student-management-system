import request from './request'

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getStudents(params) {
  return request.get('/admin/students', { params })
}

export function addStudent(data) {
  return request.post('/admin/students', data)
}

export function updateStudent(id, data) {
  return request.put(`/admin/students/${id}`, data)
}

export function deleteStudent(id) {
  return request.delete(`/admin/students/${id}`)
}

export function resetStudentPassword(id) {
  return request.post(`/admin/students/${id}/reset-password`)
}

export function approveStudent(id) {
  return request.post(`/admin/students/${id}/approve`)
}

export function getTeachers(params) {
  return request.get('/admin/teachers', { params })
}

export function addTeacher(data) {
  return request.post('/admin/teachers', data)
}

export function updateTeacher(id, data) {
  return request.put(`/admin/teachers/${id}`, data)
}

export function deleteTeacher(id) {
  return request.delete(`/admin/teachers/${id}`)
}

export function resetTeacherPassword(id) {
  return request.post(`/admin/teachers/${id}/reset-password`)
}

export function getClasses() {
  return request.get('/admin/classes')
}

export function addClass(data) {
  return request.post('/admin/classes', data)
}

export function updateClass(id, data) {
  return request.put(`/admin/classes/${id}`, data)
}

export function deleteClass(id) {
  return request.delete(`/admin/classes/${id}`)
}

export function getSemesters() {
  return request.get('/admin/semesters')
}

export function addSemester(data) {
  return request.post('/admin/semesters', data)
}

export function updateSemester(id, data) {
  return request.put(`/admin/semesters/${id}`, data)
}

export function deleteSemester(id) {
  return request.delete(`/admin/semesters/${id}`)
}

export function setCurrentSemester(id) {
  return request.put(`/admin/semesters/${id}/current`)
}

export function getCourses(params) {
  return request.get('/admin/courses', { params })
}

export function addCourse(data) {
  return request.post('/admin/courses', data)
}

export function updateCourse(id, data) {
  return request.put(`/admin/courses/${id}`, data)
}

export function deleteCourse(id) {
  return request.delete(`/admin/courses/${id}`)
}

export function getCourseStudents(courseId) {
  return request.get(`/admin/courses/${courseId}/students`)
}

export function assignCourseStudents(courseId, studentIds) {
  return request.put(`/admin/courses/${courseId}/students`, { studentIds })
}

export function unlockCourseScores(courseId) {
  return request.post(`/admin/courses/${courseId}/unlock`)
}

export function getScores(params) {
  return request.get('/admin/scores', { params })
}

export function getScoreLogs(params) {
  return request.get('/admin/score-logs', { params })
}

export function addScore(data) {
  return request.post('/admin/scores', data)
}

export function updateScore(id, data) {
  return request.put(`/admin/scores/${id}`, data)
}

export function deleteScore(id) {
  return request.delete(`/admin/scores/${id}`)
}
