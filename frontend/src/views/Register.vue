<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-title">
        <h1>学生注册</h1>
        <p>注册后使用学号登录系统</p>
      </div>

      <el-card shadow="never">
        <el-form :model="form" label-width="90px">
          <el-form-item label="学号" required>
            <el-input v-model="form.studentNo" placeholder="请输入学号" />
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="班级" required>
            <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
              <el-option
                v-for="item in classes"
                :key="item.id"
                :label="item.className"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="密码" required>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="至少 6 位"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" required>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
            />
          </el-form-item>
        </el-form>

        <div class="actions">
          <el-button @click="router.push('/login')">返回登录</el-button>
          <el-button type="primary" :loading="submitting" @click="handleRegister">注册</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRegisterClasses, register } from '../api/auth'

const router = useRouter()
const submitting = ref(false)
const classes = ref([])
const form = reactive({
  studentNo: '',
  name: '',
  gender: '男',
  phone: '',
  classId: null,
  password: '',
  confirmPassword: ''
})

async function handleRegister() {
  if (!form.studentNo || !form.name || !form.classId) {
    ElMessage.warning('请填写学号、姓名和班级')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码不能少于 6 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    await register(form)
    ElMessage.success('注册申请已提交，请等待管理员审核通过后再登录')
    router.replace({ path: '/login', query: { username: form.studentNo } })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  classes.value = await getRegisterClasses()
})
</script>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1f4e79 0%, #3d7ebd 55%, #dce9f7 100%);
}

.register-card {
  width: 520px;
  padding: 30px;
}

.register-title {
  margin-bottom: 20px;
  color: #fff;
  text-align: center;
}

.register-title h1 {
  margin: 0 0 8px;
}

.register-title p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 600px) {
  .register-card {
    width: 100%;
    padding: 16px;
  }
}
</style>
