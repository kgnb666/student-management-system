import { expect, test } from '@playwright/test'
import { login, logout } from './helpers'

test.describe('教师提交成绩并锁定', () => {
  // 保证课程 1 处于未锁定状态，避免上一次失败留下的状态影响本次运行
  test.beforeEach(async ({ request }) => {
    const loginResponse = await request.post('/api/auth/login', {
      data: { username: 'admin', password: 'admin123' }
    })
    const body = await loginResponse.json()
    await request.post('/api/admin/courses/1/unlock', {
      headers: { Authorization: `Bearer ${body.data.token}` }
    })
  })

  test('教师提交后管理员可解锁', async ({ page }) => {
    // 1. 教师提交课程 1 的成绩
    await login(page, 't001', '123456')
    await expect(page).toHaveURL(/\/teacher\/courses$/)
    await page.goto('/teacher/grades?courseId=1')
    // 课程数据加载完成：提交按钮可用且未处于锁定状态
    await expect(page.getByRole('button', { name: '提交成绩' })).toBeEnabled()
    await expect(page.locator('.el-table__row').first()).toBeVisible()

    await page.getByRole('button', { name: '提交成绩' }).click()
    await page.getByRole('button', { name: '确定提交' }).click()
    await expect(page.getByText('成绩已提交', { exact: true })).toBeVisible()
    await expect(page.getByText('成绩已提交锁定，教师无法再修改')).toBeVisible()
    await expect(page.getByRole('button', { name: '保存成绩' })).toBeDisabled()

    await logout(page)

    // 2. 管理员解锁
    await login(page, 'admin', 'admin123')
    await page.goto('/admin/courses')
    const courseRow = page.locator('.el-table__row', { hasText: '数据库原理' })
    await expect(courseRow.getByText('已提交')).toBeVisible()

    await courseRow.getByRole('button', { name: '解锁成绩' }).click()
    await page.getByRole('button', { name: '确定' }).click()
    await expect(courseRow.getByText('录入中')).toBeVisible()
  })
})
