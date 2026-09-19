<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>课程：</span>
      <el-select v-model="courseId" filterable style="width: 250px" @change="handleCourseChange">
        <el-option
          v-for="item in courses"
          :key="item.id"
          :label="`${item.courseName}（${item.semesterName}）`"
          :value="item.id"
        />
      </el-select>
      <span>班级：</span>
      <el-select v-model="classId" clearable placeholder="全部班级" style="width: 190px" @change="loadStatistics">
        <el-option
          v-for="item in classOptions"
          :key="item.id"
          :label="item.className"
          :value="item.id"
        />
      </el-select>
    </div>

    <div class="stat-grid">
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
    </div>

    <el-card shadow="never">
      <template #header>成绩分布</template>
      <ScoreDistributionChart :distribution="statistics.distribution" />
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>学生成绩明细</template>
      <el-table v-loading="loading" :data="filteredStudents" stripe>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="110" />
        <el-table-column prop="className" label="班级" min-width="170" />
        <el-table-column prop="usualScore" label="平时成绩" width="100" />
        <el-table-column prop="examScore" label="期末成绩" width="100" />
        <el-table-column prop="finalScore" label="总评成绩" width="100">
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
import { computed, onMounted, ref } from 'vue'
import { getCourseStatistics, getCourseStudents, getMyCourses } from '../../api/teacher'
import ScoreDistributionChart from '../../components/ScoreDistributionChart.vue'

const loading = ref(false)
const courses = ref([])
const students = ref([])
const courseId = ref(null)
const classId = ref(null)
const statistics = ref({
  averageScore: 0,
  maxScore: 0,
  minScore: 0,
  passRate: 0,
  excellentRate: 0,
  distribution: []
})

const classOptions = computed(() => {
  const map = new Map()
  students.value.forEach((item) => {
    if (item.classId) {
      map.set(item.classId, { id: item.classId, className: item.className })
    }
  })
  return [...map.values()]
})

const filteredStudents = computed(() => {
  if (!classId.value) {
    return students.value
  }
  return students.value.filter((item) => item.classId === classId.value)
})

async function handleCourseChange() {
  classId.value = null
  await Promise.all([loadStudents(), loadStatistics()])
}

async function loadStudents() {
  loading.value = true
  try {
    students.value = courseId.value ? await getCourseStudents(courseId.value) : []
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  if (!courseId.value) {
    return
  }
  statistics.value = await getCourseStatistics(courseId.value, classId.value)
}

onMounted(async () => {
  courses.value = await getMyCourses()
  courseId.value = courses.value[0]?.id
  if (courseId.value) {
    await handleCourseChange()
  }
})
</script>

<style scoped>
.danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
