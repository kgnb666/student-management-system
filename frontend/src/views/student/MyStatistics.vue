<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>学期：</span>
      <el-select
        v-model="semesterId"
        clearable
        placeholder="全部学期"
        style="width: 230px"
        @change="loadAll"
      >
        <el-option
          v-for="item in semesters"
          :key="item.id"
          :label="item.semesterName"
          :value="item.id"
        />
      </el-select>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ gpa.gpa }}</div>
        <div class="stat-label">平均绩点（4.0 制）</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.averageScore }}</div>
        <div class="stat-label">平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.maxScore }}</div>
        <div class="stat-label">最高分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.minScore }}</div>
        <div class="stat-label">最低分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.passRate }}%</div>
        <div class="stat-label">及格率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.excellentRate }}%</div>
        <div class="stat-label">优秀率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ gpa.earnedCredit }} / {{ gpa.totalCredit }}</div>
        <div class="stat-label">已获学分 / 总学分</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>成绩分布</template>
      <ScoreDistributionChart :distribution="statistics.distribution" />
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>课程成绩</template>
      <el-table v-loading="loading" :data="scores" stripe>
        <el-table-column prop="courseName" label="课程" min-width="160" />
        <el-table-column prop="usualScore" label="平时成绩" width="110" />
        <el-table-column prop="examScore" label="期末成绩" width="110" />
        <el-table-column prop="finalScore" label="总评成绩" width="110">
          <template #default="{ row }">
            <span :class="{ danger: row.finalScore !== null && row.finalScore < 60 }">
              {{ row.finalScore ?? '-' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyGpa, getMyScores, getMyStatistics, getSemesters } from '../../api/student'
import ScoreDistributionChart from '../../components/ScoreDistributionChart.vue'

const loading = ref(false)
const scores = ref([])
const semesters = ref([])
const semesterId = ref(null)
const statistics = ref({
  averageScore: 0,
  maxScore: 0,
  minScore: 0,
  passRate: 0,
  excellentRate: 0,
  distribution: []
})
const gpa = ref({
  gpa: 0,
  totalCredit: 0,
  earnedCredit: 0,
  courseCount: 0
})

async function loadAll() {
  loading.value = true
  try {
    const [scoreList, statisticData, gpaData] = await Promise.all([
      getMyScores({ semesterId: semesterId.value }),
      getMyStatistics({ semesterId: semesterId.value }),
      getMyGpa({ semesterId: semesterId.value })
    ])
    scores.value = scoreList
    statistics.value = statisticData
    gpa.value = gpaData
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  semesters.value = await getSemesters()
  semesterId.value = semesters.value.find((item) => item.isCurrent === 1)?.id || null
  await loadAll()
})
</script>

<style scoped>
.danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
