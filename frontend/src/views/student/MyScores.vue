<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>学期：</span>
      <el-select
        v-model="semesterId"
        clearable
        placeholder="全部学期"
        style="width: 230px"
        @change="searchScores"
      >
        <el-option
          v-for="item in semesters"
          :key="item.id"
          :label="item.semesterName"
          :value="item.id"
        />
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
          <el-tag
            v-if="row.finalScore !== null"
            :type="row.finalScore >= 60 ? 'success' : 'danger'"
          >
            {{ row.finalScore >= 60 ? '及格' : '不及格' }}
          </el-tag>
          <span v-else>-</span>
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
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getMyScores, getSemesters } from '../../api/student'

const loading = ref(false)
const scores = ref([])
const semesters = ref([])
const semesterId = ref(null)
const pagination = reactive({ page: 1, size: 10, total: 0 })

async function loadScores() {
  loading.value = true
  try {
    const data = await getMyScores({
      semesterId: semesterId.value,
      page: pagination.page,
      size: pagination.size
    })
    scores.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

function searchScores() {
  pagination.page = 1
  loadScores()
}

function handlePageChange(page) {
  pagination.page = page
  loadScores()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadScores()
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
