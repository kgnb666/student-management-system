<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增学期</el-button>
    </div>

    <el-table v-loading="loading" :data="semesters" stripe>
      <el-table-column prop="semesterName" label="学期名称" min-width="220" />
      <el-table-column prop="startDate" label="开始日期" width="130" />
      <el-table-column prop="endDate" label="结束日期" width="130" />
      <el-table-column label="当前学期" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.isCurrent === 1" type="success">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.isCurrent !== 1" link type="success" @click="setCurrent(row)">
            设为当前
          </el-button>
          <el-button link type="danger" @click="removeSemester(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学期' : '新增学期'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学期名称" required>
          <el-input v-model="form.semesterName" placeholder="例如：2025-2026学年第一学期" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSemester">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addSemester,
  deleteSemester,
  getSemesters,
  setCurrentSemester,
  updateSemester
} from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const semesters = ref([])
const emptyForm = {
  id: null,
  semesterName: '',
  startDate: '',
  endDate: ''
}
const form = reactive({ ...emptyForm })

async function loadSemesters() {
  loading.value = true
  try {
    semesters.value = await getSemesters()
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveSemester() {
  saving.value = true
  try {
    if (form.id) {
      await updateSemester(form.id, form)
    } else {
      await addSemester(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadSemesters()
  } finally {
    saving.value = false
  }
}

function setCurrent(row) {
  ElMessageBox.confirm(`确定将 ${row.semesterName} 设为当前学期吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await setCurrentSemester(row.id)
    ElMessage.success('设置成功')
    await loadSemesters()
  }).catch(() => {})
}

function removeSemester(row) {
  ElMessageBox.confirm(`确定删除 ${row.semesterName} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteSemester(row.id)
    ElMessage.success('删除成功')
    await loadSemesters()
  }).catch(() => {})
}

onMounted(loadSemesters)
</script>
