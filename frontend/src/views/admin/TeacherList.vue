<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="工号或姓名"
        clearable
        style="width: 220px"
        @keyup.enter="loadTeachers"
      />
      <el-button type="primary" @click="loadTeachers">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增教师</el-button>
    </div>

    <el-table v-loading="loading" :data="teachers" stripe>
      <el-table-column prop="teacherNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column prop="title" label="职称" width="120" />
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
          <el-button link type="danger" @click="removeTeacher(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑教师' : '新增教师'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="工号" required>
          <el-input v-model="form.teacherNo" placeholder="同时作为登录用户名" />
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
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="form.title" />
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
        <el-button type="primary" :loading="saving" @click="saveTeacher">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addTeacher,
  deleteTeacher,
  getTeachers,
  resetTeacherPassword,
  updateTeacher
} from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const teachers = ref([])
const keyword = ref('')
const emptyForm = {
  id: null,
  teacherNo: '',
  name: '',
  gender: '男',
  phone: '',
  title: '',
  status: 1
}
const form = reactive({ ...emptyForm })

async function loadTeachers() {
  loading.value = true
  try {
    teachers.value = await getTeachers({ keyword: keyword.value })
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  keyword.value = ''
  loadTeachers()
}

function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveTeacher() {
  saving.value = true
  try {
    if (form.id) {
      await updateTeacher(form.id, form)
    } else {
      await addTeacher(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadTeachers()
  } finally {
    saving.value = false
  }
}

function resetPassword(row) {
  ElMessageBox.confirm(`确定将 ${row.name} 的密码重置为 123456 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await resetTeacherPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  }).catch(() => {})
}

function removeTeacher(row) {
  ElMessageBox.confirm(`确定删除教师 ${row.name} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteTeacher(row.id)
    ElMessage.success('删除成功')
    await loadTeachers()
  }).catch(() => {})
}

onMounted(loadTeachers)
</script>
