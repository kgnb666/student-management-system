import { expect, test } from '@playwright/test'
import { login } from './helpers'

test.describe('学生查看成绩与绩点', () => {
  test('成绩列表按学期展示且支持分页', async ({ page }) => {
    await login(page, '2023001', '123456')
    await expect(page).toHaveURL(/\/student\/profile$/)

    await page.getByRole('menuitem', { name: '我的成绩' }).click()
    await expect(page).toHaveURL(/\/student\/scores$/)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    await expect(page.locator('.el-pagination')).toBeVisible()
    await expect(page.getByText('数据库原理').first()).toBeVisible()
  })

  test('统计页显示平均绩点与学分', async ({ page }) => {
    await login(page, '2023001', '123456')
    await page.goto('/student/statistics')

    await expect(page.getByText('平均绩点（4.0 制）')).toBeVisible()
    await expect(page.getByText('已获学分 / 总学分')).toBeVisible()
    await expect(page.getByText('成绩分布')).toBeVisible()
  })
})
