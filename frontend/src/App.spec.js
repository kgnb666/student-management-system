import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from './App.vue'
import Login from './views/Login.vue'

/**
 * 启动冒烟测试：验证 main.js 取消 ElementPlus 全量注册后，
 * 模板中的 el-* 组件仍能被解析渲染，el-config-provider 正常工作。
 */
describe('应用启动', () => {
  it('登录页在配置提供者内正常渲染', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        { path: '/login', component: Login }
      ]
    })
    router.push('/login')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('学生成绩管理系统')
    // el-card 被解析为真实组件（而非未注册的原始标签）
    expect(wrapper.find('.el-card').exists()).toBe(true)
    expect(wrapper.find('el-card').exists()).toBe(false)
  })
})
