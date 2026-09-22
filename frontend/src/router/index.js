import { createRouter, createWebHistory } from 'vue-router'
import { getUser, homeByRole } from '../auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/change-password',
    name: 'changePassword',
    component: () => import('../views/ChangePassword.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: 'admin/dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { role: 'ADMIN', title: '首页概览' }
      },
      {
        path: 'admin/students',
        component: () => import('../views/admin/StudentList.vue'),
        meta: { role: 'ADMIN', title: '学生管理' }
      },
      {
        path: 'admin/teachers',
        component: () => import('../views/admin/TeacherList.vue'),
        meta: { role: 'ADMIN', title: '教师管理' }
      },
      {
        path: 'admin/classes',
        component: () => import('../views/admin/ClassList.vue'),
        meta: { role: 'ADMIN', title: '班级管理' }
      },
      {
        path: 'admin/courses',
        component: () => import('../views/admin/CourseList.vue'),
        meta: { role: 'ADMIN', title: '课程管理' }
      },
      {
        path: 'admin/semesters',
        component: () => import('../views/admin/SemesterList.vue'),
        meta: { role: 'ADMIN', title: '学期管理' }
      },
      {
        path: 'admin/scores',
        component: () => import('../views/admin/ScoreList.vue'),
        meta: { role: 'ADMIN', title: '成绩管理' }
      },
      {
        path: 'teacher/courses',
        component: () => import('../views/teacher/MyCourses.vue'),
        meta: { role: 'TEACHER', title: '我的课程' }
      },
      {
        path: 'teacher/grades',
        component: () => import('../views/teacher/GradeManage.vue'),
        meta: { role: 'TEACHER', title: '成绩录入与查询' }
      },
      {
        path: 'teacher/statistics',
        component: () => import('../views/teacher/CourseStatistics.vue'),
        meta: { role: 'TEACHER', title: '班级成绩统计' }
      },
      {
        path: 'student/profile',
        component: () => import('../views/student/Profile.vue'),
        meta: { role: 'STUDENT', title: '个人信息' }
      },
      {
        path: 'student/courses',
        component: () => import('../views/student/MyCourses.vue'),
        meta: { role: 'STUDENT', title: '我的课程' }
      },
      {
        path: 'student/scores',
        component: () => import('../views/student/MyScores.vue'),
        meta: { role: 'STUDENT', title: '我的成绩' }
      },
      {
        path: 'student/statistics',
        component: () => import('../views/student/MyStatistics.vue'),
        meta: { role: 'STUDENT', title: '成绩统计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const user = getUser()
  if (to.path === '/login' || to.path === '/register') {
    return user ? homeByRole(user.role) : true
  }
  if (!user) {
    return '/login'
  }
  // 管理员新建或重置密码后，必须先修改初始密码
  if (user.needChangePassword === 1) {
    return to.path === '/change-password' ? true : '/change-password'
  }
  if (to.path === '/') {
    return homeByRole(user.role)
  }
  if (to.meta.role && to.meta.role !== user.role) {
    return homeByRole(user.role)
  }
  return true
})

export default router
