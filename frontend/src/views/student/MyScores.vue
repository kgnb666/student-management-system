<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>学期：</span>
      <el-select v-model="semesterId" clearable placeholder="全部学期" style="width: 230px" @change="loadScores">
        <el-option v-for="item in semesters" :key="item.id" :label="item.semesterName" :value="item.id" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="scores" stripe>
      <el-table-column prop="semesterName" label="学期" min-width="200" />
      <el-table-column prop="courseCode" label="课程编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="usualScore" label="平时成绩" width="100" />
      <el-table-column prop="examScore" label="期末成绩" width="100" />
      <el-table-column prop="finalScore" label="总评成绩" width="100">
        <template #default="{ row }">
          <span :class="{ danger: row.finalScore !== null && row.finalScore < 60 }">
            {{ row.finalScore ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="是否及格" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.finalScore !== null" :type="row.finalScore >= 60 ? 'success' : 'danger'">
            {{ row.finalScore >= 60 ? '及格' : '不及格' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyScores, getSemesters } from '../../api/student'

const loading = ref(false)
const scores = ref([])
const semesters = ref([])
const semesterId = ref(null)

async function loadScores() {
  loading.value = true
  try {
    scores.value = await getMyScores({ semesterId: semesterId.value })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  semesters.value = await getSemesters()
  semesterId.value = semesters.value.find((item) => item.isCurrent === 1)?.id || null
  await loadScores()
})
</script>

<style scoped>
.danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
