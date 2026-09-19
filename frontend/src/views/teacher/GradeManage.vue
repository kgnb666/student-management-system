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
      <el-tag type="info">已录入 {{ scoredCount }} / {{ rows.length }}</el-tag>
      <el-button type="primary" :loading="saving" :disabled="!courseId" @click="saveAll">保存成绩</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredRows" stripe>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="110" />
      <el-table-column prop="className" label="班级" min-width="170" />
      <el-table-column label="平时成绩（30%）" width="170">
        <template #default="{ row }">
          <el-input-number v-model="row.usualScore" :min="0" :max="100" :precision="1" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column label="期末成绩（70%）" width="170">
        <template #default="{ row }">
          <el-input-number v-model="row.examScore" :min="0" :max="100" :precision="1" controls-position="right" />
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

    <el-empty v-if="!loading && !rows.length" description="该课程还没有学生，请先在课程管理中维护学生名单" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCourseStudents, getMyCourses, saveScores } from '../../api/teacher'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const courses = ref([])
const rows = ref([])
const courseId = ref(null)
const keyword = ref('')

const filteredRows = computed(() => {
  const value = keyword.value.trim()
  if (!value) {
    return rows.value
  }
  return rows.value.filter((row) =>
    row.studentNo.includes(value) || row.studentName.includes(value)
  )
})

const scoredCount = computed(() =>
  rows.value.filter((row) => row.usualScore !== null && row.examScore !== null).length
)

function formatFinal(row) {
  if (row.usualScore === null || row.examScore === null) {
    return '-'
  }
  return (row.usualScore * 0.3 + row.examScore * 0.7).toFixed(1)
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
    await saveScores(courseId.value, rows.value.map((row) => ({
      studentId: row.studentId,
      usualScore: row.usualScore,
      examScore: row.examScore
    })))
    ElMessage.success('成绩保存成功')
    await loadStudents()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  courses.value = await getMyCourses()
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
</style>
