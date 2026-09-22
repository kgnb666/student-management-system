<template>
  <el-drawer v-model="visible" :title="title" size="720px" @open="loadLogs">
    <el-table v-loading="loading" :data="logs" stripe>
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column label="操作人" width="140">
        <template #default="{ row }">
          {{ row.operatorName || '-' }}
          <el-tag v-if="row.operatorRole" size="small" type="info">{{
            roleText(row.operatorRole)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)" size="small">{{
            actionText(row.action)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="学号" width="110">
        <template #default="{ row }">{{ row.studentNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="变更前（平时/期末/总评）" min-width="170">
        <template #default="{ row }">{{ formatScores(row, 'before') }}</template>
      </el-table-column>
      <el-table-column label="变更后（平时/期末/总评）" min-width="170">
        <template #default="{ row }">{{ formatScores(row, 'after') }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140">
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      :current-page="page"
      :page-size="size"
      @current-change="handlePageChange"
    />

    <el-empty v-if="!loading && !logs.length" description="暂无变更记录" />
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { getScoreLogs } from '../api/admin'
import { getCourseScoreLogs } from '../api/teacher'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [Number, String], default: null },
  studentId: { type: [Number, String], default: null },
  title: { type: String, default: '成绩变更记录' },
  scope: { type: String, default: 'admin' }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const logs = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

async function loadLogs() {
  if (!props.courseId) {
    return
  }
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    const data =
      props.scope === 'teacher'
        ? await getCourseScoreLogs(props.courseId, params)
        : await getScoreLogs({
            ...params,
            courseId: props.courseId,
            studentId: props.studentId || undefined
          })
    logs.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handlePageChange(value) {
  page.value = value
  loadLogs()
}

function formatScores(row, prefix) {
  const usual = row[`${prefix}UsualScore`]
  const exam = row[`${prefix}ExamScore`]
  const final = row[`${prefix}FinalScore`]
  if (usual === null && exam === null && final === null) {
    return '-'
  }
  return `${usual ?? '-'} / ${exam ?? '-'} / ${final ?? '-'}`
}

function actionText(action) {
  return (
    {
      CREATE: '新增',
      UPDATE: '修改',
      DELETE: '删除',
      SUBMIT: '提交锁定',
      UNLOCK: '解锁'
    }[action] || action
  )
}

function actionTagType(action) {
  if (action === 'DELETE') {
    return 'danger'
  }
  if (action === 'SUBMIT' || action === 'UNLOCK') {
    return 'warning'
  }
  return 'primary'
}

function roleText(role) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || role
}
</script>
