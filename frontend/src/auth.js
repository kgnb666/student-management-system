const USER_KEY = 'student_score_user'

export function saveUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getUser() {
  const value = localStorage.getItem(USER_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value)
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function clearUser() {
  localStorage.removeItem(USER_KEY)
}

export function getToken() {
  return getUser()?.token || ''
}

export function homeByRole(role) {
  if (role === 'ADMIN') {
    return '/admin/students'
  }
  if (role === 'TEACHER') {
    return '/teacher/courses'
  }
  return '/student/profile'
}
