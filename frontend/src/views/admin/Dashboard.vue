<template>
  <el-card v-loading="loading" class="page-card" shadow="never">
    <div class="toolbar">
      <el-tag type="info">当前学期：{{ dashboard.currentSemesterName || '未设置' }}</el-tag>
      <div class="spacer"></div>
      <el-button @click="loadDashboard">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.studentCount }}</div>
        <div class="stat-label">学生总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.teacherCount }}</div>
        <div class="stat-label">教师总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.classCount }}</div>
        <div class="stat-label">班级总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.courseCount }}</div>
        <div class="stat-label">课程总数</div>
      </div>
      <div
        class="stat-card"
        :class="{ clickable: dashboard.pendingStudentCount > 0 }"
        @click="goPendingStudents"
      >
        <div class="stat-value pending">{{ dashboard.pendingStudentCount }}</div>
        <div class="stat-label">待审核注册</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.averageScore }}</div>
        <div class="stat-label">当前学期平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.passRate }}%</div>
        <div class="stat-label">当前学期及格率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.excellentRate }}%</div>
        <div class="stat-label">当前学期优秀率</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>当前学期成绩分布</template>
      <ScoreDistributionChart :distribution="statistics.distribution" />
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard } from '../../api/admin'
import ScoreDistributionChart from '../../components/ScoreDistributionChart.vue'

const router = useRouter()
const loading = ref(false)
const dashboard = ref({
  studentCount: 0,
  teacherCount: 0,
  classCount: 0,
  courseCount: 0,
  pendingStudentCount: 0,
  currentSemesterId: null,
  currentSemesterName: '',
  scoreStatistics: null
})

const statistics = computed(
  () =>
    dashboard.value.scoreStatistics || {
      averageScore: 0,
      maxScore: 0,
      minScore: 0,
      passRate: 0,
      excellentRate: 0,
      distribution: []
    }
)

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
  } finally {
    loading.value = false
  }
}

function goPendingStudents() {
  if (dashboard.value.pendingStudentCount > 0) {
    router.push({ path: '/admin/students', query: { status: 2 } })
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.stat-card.clickable {
  cursor: pointer;
  border-color: #e6a23c;
}

.stat-value.pending {
  color: #e6a23c;
}
</style>
