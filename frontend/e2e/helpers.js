import { expect } from '@playwright/test'

// E2E 公共操作：登录与退出。
export async function login(page, username, password) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').waitFor({ state: 'visible' })
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  // 登录成功后一定离开登录页，失败时在这里报错，避免后续断言被误导
  await expect(page).not.toHaveURL(/\/login$/, { timeout: 15_000 })
}

export async function logout(page) {
  await page.getByRole('button', { name: '退出登录' }).click()
  await page.getByRole('button', { name: '确定' }).click()
}
