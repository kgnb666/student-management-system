<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增班级</el-button>
    </div>

    <el-table v-loading="loading" :data="classes" stripe>
      <el-table-column prop="className" label="班级名称" min-width="180" />
      <el-table-column prop="major" label="专业" min-width="180" />
      <el-table-column prop="gradeYear" label="年级" width="100" />
      <el-table-column label="班主任" width="120">
        <template #default="{ row }">
          {{ teacherName(row.headTeacherId) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeClass(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑班级' : '新增班级'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="班级名称" required>
          <el-input v-model="form.className" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" />
        </el-form-item>
        <el-form-item label="年级" required>
          <el-input-number v-model="form.gradeYear" :min="2000" :max="2100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" clearable style="width: 100%">
            <el-option v-for="item in teachers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addClass, deleteClass, getClasses, getTeachers, updateClass } from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const classes = ref([])
const teachers = ref([])
const emptyForm = {
  id: null,
  className: '',
  major: '',
  gradeYear: new Date().getFullYear(),
  headTeacherId: null
}
const form = reactive({ ...emptyForm })

async function loadClasses() {
  loading.value = true
  try {
    classes.value = await getClasses()
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  teachers.value = await getTeachers({})
}

function teacherName(id) {
  return teachers.value.find((item) => item.id === id)?.name || '未设置'
}

function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveClass() {
  saving.value = true
  try {
    if (form.id) {
      await updateClass(form.id, form)
    } else {
      await addClass(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadClasses()
  } finally {
    saving.value = false
  }
}

function removeClass(row) {
  ElMessageBox.confirm(`确定删除 ${row.className} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteClass(row.id)
    ElMessage.success('删除成功')
    await loadClasses()
  }).catch(() => {})
}

onMounted(() => {
  loadTeachers()
  loadClasses()
})
</script>
