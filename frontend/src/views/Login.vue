<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="system-title">
        <h1>学生成绩管理系统</h1>
        <p>课程作业 / 课程设计演示项目</p>
      </div>

      <el-card shadow="never" class="login-card">
        <el-form :model="form" @keyup.enter="handleLogin">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" clearable />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="密码"
              show-password
            />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-button" @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <div class="demo-accounts">
          <div class="demo-title">演示账号</div>
          <div>管理员：admin / admin123</div>
          <div>教师：t001 / 123456</div>
          <div>学生：2023001 / 123456</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { homeByRole, saveUser } from '../auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await login(form)
    saveUser(user)
    router.replace(homeByRole(user.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1f4e79 0%, #3d7ebd 55%, #dce9f7 100%);
}

.login-panel {
  display: grid;
  grid-template-columns: 380px 400px;
  gap: 60px;
  align-items: center;
  padding: 40px;
}

.system-title {
  color: #fff;
}

.system-title h1 {
  margin: 0 0 18px;
  font-size: 40px;
  line-height: 1.25;
}

.system-title p {
  color: rgba(255, 255, 255, 0.78);
  font-size: 16px;
}

.login-card {
  border: none;
  border-radius: 12px;
}

.login-button {
  width: 100%;
}

.demo-accounts {
  padding-top: 20px;
  margin-top: 24px;
  color: #606266;
  font-size: 13px;
  line-height: 1.9;
  border-top: 1px solid #ebeef5;
}

.demo-title {
  margin-bottom: 4px;
  color: #303133;
  font-weight: 600;
}

@media (max-width: 900px) {
  .login-panel {
    grid-template-columns: 1fr;
    gap: 20px;
    width: 100%;
    max-width: 480px;
  }

  .system-title h1 {
    font-size: 30px;
  }
}
</style>
