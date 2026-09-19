<template>
  <el-card class="page-card" shadow="never">
    <el-table v-loading="loading" :data="courses" stripe>
      <el-table-column prop="courseCode" label="课程编号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="hours" label="学时" width="80" />
      <el-table-column prop="courseType" label="类型" width="90" />
      <el-table-column prop="semesterName" label="学期" min-width="200" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goGrades(row.id)">录入成绩</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCourses } from '../../api/teacher'

const router = useRouter()
const loading = ref(false)
const courses = ref([])

function goGrades(courseId) {
  router.push({ path: '/teacher/grades', query: { courseId } })
}

onMounted(async () => {
  loading.value = true
  try {
    courses.value = await getMyCourses()
  } finally {
    loading.value = false
  }
})
</script>
