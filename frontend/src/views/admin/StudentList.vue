<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="学号或姓名"
        clearable
        style="width: 220px"
        @keyup.enter="loadStudents"
      />
      <el-select v-model="query.classId" placeholder="全部班级" clearable style="width: 180px">
        <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
      </el-select>
      <el-button type="primary" @click="loadStudents">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增学生</el-button>
    </div>

    <el-table v-loading="loading" :data="students" stripe>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="birthDate" label="出生日期" width="120" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="className" label="班级" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
          <el-button link type="danger" @click="removeStudent(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学生' : '新增学生'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学号" required>
          <el-input v-model="form.studentNo" placeholder="同时作为登录用户名" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="班级" required>
          <el-select v-model="form.classId" style="width: 100%">
            <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStudent">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addStudent,
  deleteStudent,
  getClasses,
  getStudents,
  resetStudentPassword,
  updateStudent
} from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const students = ref([])
const classes = ref([])
const query = reactive({
  keyword: '',
  classId: null
})
const emptyForm = {
  id: null,
  studentNo: '',
  name: '',
  gender: '男',
  birthDate: '',
  phone: '',
  classId: null,
  status: 1
}
const form = reactive({ ...emptyForm })

async function loadStudents() {
  loading.value = true
  try {
    students.value = await getStudents(query)
  } finally {
    loading.value = false
  }
}

async function loadClasses() {
  classes.value = await getClasses()
}

function resetQuery() {
  query.keyword = ''
  query.classId = null
  loadStudents()
}

function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveStudent() {
  saving.value = true
  try {
    if (form.id) {
      await updateStudent(form.id, form)
    } else {
      await addStudent(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadStudents()
  } finally {
    saving.value = false
  }
}

function resetPassword(row) {
  ElMessageBox.confirm(`确定将 ${row.name} 的密码重置为 123456 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await resetStudentPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  }).catch(() => {})
}

function removeStudent(row) {
  ElMessageBox.confirm(`确定删除学生 ${row.name} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    await loadStudents()
  }).catch(() => {})
}

onMounted(() => {
  loadClasses()
  loadStudents()
})
</script>
