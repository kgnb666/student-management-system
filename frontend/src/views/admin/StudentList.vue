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
        <el-option
          v-for="item in classes"
          :key="item.id"
          :label="item.className"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="正常" :value="1" />
        <el-option label="待审核" :value="2" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="searchStudents">查询</el-button>
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
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 2" link type="success" @click="approveStudentRow(row)">
            审核通过
          </el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
          <el-button link type="danger" @click="removeStudent(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="pagination.total > 0"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      :current-page="pagination.page"
      :page-size="pagination.size"
      :page-sizes="[10, 20, 50]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

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
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="班级" required>
          <el-select v-model="form.classId" style="width: 100%">
            <el-option
              v-for="item in classes"
              :key="item.id"
              :label="item.className"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="待审核" :value="2" />
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
import { useRoute } from 'vue-router'
import {
  addStudent,
  approveStudent,
  deleteStudent,
  getClasses,
  getStudents,
  resetStudentPassword,
  updateStudent
} from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const route = useRoute()
const students = ref([])
const classes = ref([])
const query = reactive({
  keyword: '',
  classId: null,
  status: null
})
const pagination = reactive({ page: 1, size: 10, total: 0 })
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
    const data = await getStudents({ ...query, page: pagination.page, size: pagination.size })
    students.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

function searchStudents() {
  pagination.page = 1
  loadStudents()
}

function handlePageChange(page) {
  pagination.page = page
  loadStudents()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadStudents()
}

async function loadClasses() {
  classes.value = await getClasses()
}

function resetQuery() {
  query.keyword = ''
  query.classId = null
  query.status = null
  pagination.page = 1
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
  })
    .then(async () => {
      await resetStudentPassword(row.id)
      ElMessage.success('密码已重置为 123456')
    })
    .catch(() => {})
}

function removeStudent(row) {
  ElMessageBox.confirm(`确定删除学生 ${row.name} 吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deleteStudent(row.id)
      ElMessage.success('删除成功')
      await loadStudents()
    })
    .catch(() => {})
}

function statusText(status) {
  if (status === 1) {
    return '正常'
  }
  if (status === 2) {
    return '待审核'
  }
  return '停用'
}

function statusTagType(status) {
  if (status === 1) {
    return 'success'
  }
  if (status === 2) {
    return 'warning'
  }
  return 'info'
}

function approveStudentRow(row) {
  ElMessageBox.confirm(`确定审核通过 ${row.name}（${row.studentNo}）的注册申请吗？`, '审核确认', {
    type: 'warning'
  })
    .then(async () => {
      await approveStudent(row.id)
      ElMessage.success('已审核通过，该学生可以登录了')
      await loadStudents()
    })
    .catch(() => {})
}

onMounted(() => {
  // 支持从首页概览的「待审核注册」跳转过来时自动过滤
  if (route.query.status !== undefined && route.query.status !== '') {
    query.status = Number(route.query.status)
  }
  loadClasses()
  loadStudents()
})
</script>
