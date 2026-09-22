<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <span>学期：</span>
      <el-select
        v-model="semesterId"
        clearable
        placeholder="全部学期"
        style="width: 230px"
        @change="loadCourses"
      >
        <el-option
          v-for="item in semesters"
          :key="item.id"
          :label="item.semesterName"
          :value="item.id"
        />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="courses" stripe>
      <el-table-column prop="courseCode" label="课程编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="hours" label="学时" width="80" />
      <el-table-column prop="courseType" label="类型" width="90" />
      <el-table-column prop="teacherName" label="授课教师" width="110" />
      <el-table-column prop="semesterName" label="学期" min-width="200" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyCourses, getSemesters } from '../../api/student'

const loading = ref(false)
const courses = ref([])
const semesters = ref([])
const semesterId = ref(null)

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await getMyCourses({ semesterId: semesterId.value })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  semesters.value = await getSemesters()
  semesterId.value = semesters.value.find((item) => item.isCurrent === 1)?.id || null
  await loadCourses()
})
</script>
