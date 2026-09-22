import { beforeEach, describe, expect, it } from 'vitest'
import { clearUser, getToken, getUser, homeByRole, saveUser } from './auth'

describe('auth 登录态工具', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('保存后可以读取用户信息与 token', () => {
    saveUser({ token: 'token-1', role: 'ADMIN', realName: '系统管理员' })

    expect(getUser()).toEqual({ token: 'token-1', role: 'ADMIN', realName: '系统管理员' })
    expect(getToken()).toBe('token-1')
  })

  it('未登录时返回 null 与空 token', () => {
    expect(getUser()).toBeNull()
    expect(getToken()).toBe('')
  })

  it('本地数据损坏时返回 null 并清理脏数据', () => {
    localStorage.setItem('student_score_user', '{not-json')

    expect(getUser()).toBeNull()
    expect(localStorage.getItem('student_score_user')).toBeNull()
  })

  it('清除登录态后 token 为空', () => {
    saveUser({ token: 'token-1' })

    clearUser()

    expect(getUser()).toBeNull()
    expect(getToken()).toBe('')
  })

  it('按角色返回对应首页', () => {
    expect(homeByRole('ADMIN')).toBe('/admin/dashboard')
    expect(homeByRole('TEACHER')).toBe('/teacher/courses')
    expect(homeByRole('STUDENT')).toBe('/student/profile')
    expect(homeByRole(undefined)).toBe('/student/profile')
  })
})
