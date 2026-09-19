<template>
  <el-card class="page-card" shadow="never">
    <template #header>个人基本信息</template>
    <el-descriptions v-loading="loading" :column="2" border>
      <el-descriptions-item label="学号">{{ profile.studentNo }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
      <el-descriptions-item label="性别">{{ profile.gender }}</el-descriptions-item>
      <el-descriptions-item label="出生日期">{{ profile.birthDate }}</el-descriptions-item>
      <el-descriptions-item label="班级">{{ profile.className }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ profile.phone }}</el-descriptions-item>
      <el-descriptions-item label="账号状态">
        <el-tag :type="profile.status === 1 ? 'success' : 'info'">
          {{ profile.status === 1 ? '正常' : '停用' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getProfile } from '../../api/student'

const loading = ref(false)
const profile = ref({})

onMounted(async () => {
  loading.value = true
  try {
    profile.value = await getProfile()
  } finally {
    loading.value = false
  }
})
</script>
