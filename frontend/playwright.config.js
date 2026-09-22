import { defineConfig } from '@playwright/test'

// E2E 配置。
// 运行前需要先启动后端与前端（例如用 start.bat，或手动启动）：
//   backend:  java -jar target/student-score-1.0.0.jar
//   frontend: npm run dev（端口需与 E2E_BASE_URL 一致）
// 默认地址 http://localhost:15173，可用 E2E_BASE_URL 覆盖。
export default defineConfig({
  testDir: './e2e',
  timeout: 40_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:15173',
    headless: true,
    locale: 'zh-CN',
    screenshot: 'only-on-failure'
  }
})
