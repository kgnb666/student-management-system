<template>
  <div class="change-page">
    <el-card class="change-card" shadow="never">
      <div class="change-title">
        <h1>修改密码</h1>
        <p v-if="forced">首次登录请先修改初始密码，修改后需重新登录</p>
        <p v-else>修改成功后需要重新登录</p>
      </div>

      <el-form :model="form" label-width="90px" @keyup.enter="handleSubmit">
        <el-form-item label="原密码" required>
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="至少 6 位"
          />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button v-if="!forced" @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '../api/auth'
import { clearUser, getUser } from '../auth'

const router = useRouter()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const forced = computed(() => getUser()?.needChangePassword === 1)

async function handleSubmit() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (form.newPassword.length < 6) {
    ElMessage.warning('密码不能少于 6 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    await changePassword(form)
    clearUser()
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.change-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1f4e79 0%, #3d7ebd 55%, #dce9f7 100%);
}

.change-card {
  width: 480px;
  padding: 10px;
  border: none;
  border-radius: 12px;
}

.change-title {
  margin-bottom: 20px;
  text-align: center;
}

.change-title h1 {
  margin: 0 0 8px;
  font-size: 22px;
}

.change-title p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 600px) {
  .change-card {
    width: 100%;
    margin: 16px;
  }
}
</style>
