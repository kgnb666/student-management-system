import { expect, test } from '@playwright/test'
import { login } from './helpers'

test.describe('管理员首页概览', () => {
  test('登录后进入首页概览并显示统计数据', async ({ page }) => {
    await login(page, 'admin', 'admin123')

    await expect(page).toHaveURL(/\/admin\/dashboard$/)
    await expect(page.getByText('首页概览').first()).toBeVisible()
    await expect(page.getByText('学生总数')).toBeVisible()
    await expect(page.locator('.stat-card').first()).toContainText('8')
    await expect(page.getByText('当前学期：2025-2026学年第二学期')).toBeVisible()
    await expect(page.getByText('当前学期成绩分布')).toBeVisible()
  })

  test('侧边栏可以进入学生管理并显示分页', async ({ page }) => {
    await login(page, 'admin', 'admin123')
    await page.getByRole('menuitem', { name: '学生管理' }).click()

    await expect(page).toHaveURL(/\/admin\/students$/)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    await expect(page.locator('.el-pagination')).toBeVisible()
  })
})
