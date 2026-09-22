<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>选择课程：</span>
      <el-select v-model="courseId" filterable style="width: 250px" @change="loadStudents">
        <el-option
          v-for="item in courses"
          :key="item.id"
          :label="`${item.courseName}（${item.semesterName}）`"
          :value="item.id"
        />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索学号或姓名" clearable style="width: 200px" />
      <div class="spacer"></div>
      <el-tag :type="locked ? 'success' : 'warning'">
        {{ locked ? '成绩已提交' : '录入中' }}
      </el-tag>
      <el-tag type="info">已录入 {{ scoredCount }} / {{ rows.length }}</el-tag>
      <el-button :disabled="!courseId" @click="exportScores">导出成绩</el-button>
      <el-button :disabled="!courseId" @click="openLogs">变更记录</el-button>
      <el-upload
        :show-file-list="false"
        :auto-upload="false"
        accept=".xlsx"
        :on-change="handleImport"
      >
        <el-button :disabled="!courseId || locked">导入成绩</el-button>
      </el-upload>
      <el-button type="primary" :loading="saving" :disabled="!courseId || locked" @click="saveAll">
        保存成绩
      </el-button>
      <el-button
        type="success"
        :loading="submitting"
        :disabled="!courseId || locked"
        @click="submitScores"
      >
        提交成绩
      </el-button>
    </div>

    <el-alert
      v-if="locked"
      type="success"
      :closable="false"
      show-icon
      title="成绩已提交锁定，教师无法再修改。如需修改请联系管理员解锁。"
      style="margin-bottom: 12px"
    />

    <el-table v-loading="loading" :data="filteredRows" stripe>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column prop="className" label="班级" min-width="170" />
      <el-table-column label="平时成绩（30%）" width="170">
        <template #default="{ row }">
          <el-input-number
            v-model="row.usualScore"
            :min="0"
            :max="100"
            :precision="1"
            :disabled="locked"
            controls-position="right"
          />
        </template>
      </el-table-column>
      <el-table-column label="期末成绩（70%）" width="170">
        <template #default="{ row }">
          <el-input-number
            v-model="row.examScore"
            :min="0"
            :max="100"
            :precision="1"
            :disabled="locked"
            controls-position="right"
          />
        </template>
      </el-table-column>
      <el-table-column label="总评成绩" width="110">
        <template #default="{ row }">
          <span :class="{ danger: row.finalScore !== null && row.finalScore < 60 }">
            {{ formatFinal(row) }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!loading && !rows.length"
      description="该课程还没有学生，请先在课程管理中维护学生名单"
    />

    <el-dialog v-model="errorDialogVisible" title="导入结果" width="560px">
      <p>成功导入 {{ importResult.successCount }} 条，失败 {{ importResult.failCount }} 条。</p>
      <ul class="import-errors">
        <li v-for="(item, index) in importResult.errors" :key="index">{{ item }}</li>
      </ul>
      <template #footer>
        <el-button type="primary" @click="errorDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <ScoreChangeLogDrawer
      v-model="logVisible"
      scope="teacher"
      :course-id="courseId"
      :title="`${currentCourse?.courseName || ''} 成绩变更记录`"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  exportCourseScores,
  getCourseStudents,
  getMyCourses,
  importCourseScores,
  saveScores,
  submitCourseScores
} from '../../api/teacher'
import ScoreChangeLogDrawer from '../../components/ScoreChangeLogDrawer.vue'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const errorDialogVisible = ref(false)
const logVisible = ref(false)
const courses = ref([])
const rows = ref([])
const courseId = ref(null)
const keyword = ref('')
const importResult = ref({ successCount: 0, failCount: 0, errors: [] })

const currentCourse = computed(() => courses.value.find((item) => item.id === courseId.value))
const locked = computed(() => currentCourse.value?.scoreStatus === 1)

const filteredRows = computed(() => {
  const value = keyword.value.trim()
  if (!value) {
    return rows.value
  }
  return rows.value.filter(
    (row) => row.studentNo.includes(value) || row.studentName.includes(value)
  )
})

const scoredCount = computed(
  () => rows.value.filter((row) => row.usualScore !== null && row.examScore !== null).length
)

function formatFinal(row) {
  if (row.usualScore === null || row.examScore === null) {
    return '-'
  }
  return (row.usualScore * 0.3 + row.examScore * 0.7).toFixed(1)
}

async function loadCourses() {
  courses.value = await getMyCourses()
}

async function loadStudents() {
  if (!courseId.value) {
    return
  }
  loading.value = true
  try {
    rows.value = await getCourseStudents(courseId.value)
  } finally {
    loading.value = false
  }
}

async function saveAll() {
  const invalid = rows.value.some((row) => {
    const values = [row.usualScore, row.examScore].filter((value) => value !== null)
    return values.some((value) => value < 0 || value > 100)
  })
  if (invalid) {
    ElMessage.warning('成绩必须在 0 到 100 之间')
    return
  }

  saving.value = true
  try {
    await saveScores(
      courseId.value,
      rows.value.map((row) => ({
        studentId: row.studentId,
        usualScore: row.usualScore,
        examScore: row.examScore
      }))
    )
    ElMessage.success('成绩保存成功')
    await loadStudents()
  } finally {
    saving.value = false
  }
}

async function submitScores() {
  try {
    await ElMessageBox.confirm(
      '提交后该课程成绩将被锁定，教师无法再修改，需要管理员解锁。确定提交吗？',
      '提交确认',
      { type: 'warning', confirmButtonText: '确定提交', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await submitCourseScores(courseId.value)
    ElMessage.success('成绩已提交锁定')
    await loadCourses()
    await loadStudents()
  } finally {
    submitting.value = false
  }
}

async function exportScores() {
  const blob = await exportCourseScores(courseId.value)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentCourse.value?.courseName || '课程'}成绩单.xlsx`
  link.click()
  URL.revokeObjectURL(url)
}

async function handleImport(uploadFile) {
  const file = uploadFile?.raw
  if (!file) {
    return
  }
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.warning('只支持 .xlsx 格式的成绩单')
    return
  }
  importResult.value = await importCourseScores(courseId.value, file)
  if (importResult.value.failCount > 0) {
    errorDialogVisible.value = true
    ElMessage.warning(
      `导入完成：成功 ${importResult.value.successCount} 条，失败 ${importResult.value.failCount} 条`
    )
  } else {
    ElMessage.success(`导入成功 ${importResult.value.successCount} 条`)
  }
  await loadStudents()
}

function openLogs() {
  logVisible.value = true
}

onMounted(async () => {
  await loadCourses()
  const queryCourseId = Number(route.query.courseId)
  courseId.value = courses.value.some((item) => item.id === queryCourseId)
    ? queryCourseId
    : courses.value[0]?.id
  await loadStudents()
})
</script>

<style scoped>
.danger {
  color: #f56c6c;
  font-weight: 600;
}

.toolbar :deep(.el-input-number) {
  width: 130px;
}

.import-errors {
  max-height: 240px;
  padding-left: 20px;
  margin: 0;
  overflow-y: auto;
  color: #f56c6c;
  line-height: 1.8;
}
</style>
