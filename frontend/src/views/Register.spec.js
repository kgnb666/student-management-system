import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const registerMock = vi.fn()
const classesMock = vi.fn()
const warningMock = vi.fn()
const successMock = vi.fn()

vi.mock('../api/auth', () => ({
  register: (...args) => registerMock(...args),
  getRegisterClasses: (...args) => classesMock(...args)
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { ...actual.ElMessage, warning: warningMock, success: successMock, error: vi.fn() }
  }
})

const Register = (await import('./Register.vue')).default

const stubs = {
  'el-card': true,
  'el-form': true,
  'el-form-item': true,
  'el-input': true,
  'el-radio-group': true,
  'el-radio': true,
  'el-select': true,
  'el-option': true,
  'el-button': true
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', component: { template: '<div />' } },
      { path: '/register', component: { template: '<div />' } }
    ]
  })
}

async function mountRegister() {
  const wrapper = mount(Register, {
    global: { plugins: [createTestRouter()], stubs }
  })
  await wrapper.vm.$nextTick()
  return wrapper
}

describe('注册表单校验', () => {
  beforeEach(() => {
    registerMock.mockReset()
    warningMock.mockReset()
    successMock.mockReset()
    classesMock.mockReset()
    classesMock.mockResolvedValue([])
  })

  it('未填写学号、姓名或班级时提示且不提交', async () => {
    const wrapper = await mountRegister()

    await wrapper.vm.handleRegister()

    expect(warningMock).toHaveBeenCalledWith('请填写学号、姓名和班级')
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('密码少于 6 位时提示且不提交', async () => {
    const wrapper = await mountRegister()
    Object.assign(wrapper.vm.form, {
      studentNo: 'S100',
      name: '张三',
      classId: 1,
      password: '123',
      confirmPassword: '123'
    })

    await wrapper.vm.handleRegister()

    expect(warningMock).toHaveBeenCalledWith('密码不能少于 6 位')
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('两次密码不一致时提示且不提交', async () => {
    const wrapper = await mountRegister()
    Object.assign(wrapper.vm.form, {
      studentNo: 'S100',
      name: '张三',
      classId: 1,
      password: '123456',
      confirmPassword: '654321'
    })

    await wrapper.vm.handleRegister()

    expect(warningMock).toHaveBeenCalledWith('两次输入的密码不一致')
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('校验通过时提交注册并跳转登录页', async () => {
    registerMock.mockResolvedValue({})
    const wrapper = await mountRegister()
    Object.assign(wrapper.vm.form, {
      studentNo: 'S100',
      name: '张三',
      classId: 1,
      password: '123456',
      confirmPassword: '123456'
    })

    await wrapper.vm.handleRegister()

    expect(registerMock).toHaveBeenCalledTimes(1)
    expect(successMock).toHaveBeenCalledWith('注册申请已提交，请等待管理员审核通过后再登录')
  })
})
