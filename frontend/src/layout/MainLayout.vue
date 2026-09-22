<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="brand-full">学生成绩管理系统</span>
        <span class="brand-short">成绩</span>
      </div>
      <el-menu
        router
        :default-active="route.path"
        background-color="#263445"
        text-color="#c8d3df"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title">{{ route.meta.title }}</div>
        <div class="user-area">
          <span class="user-name">{{ roleName }}：{{ user?.realName }}</span>
          <el-button link type="primary" @click="router.push('/change-password')">
            <el-icon><Key /></el-icon>
            修改密码
          </el-button>
          <el-button link type="danger" @click="logout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Calendar,
  Collection,
  DataAnalysis,
  DocumentChecked,
  Key,
  Odometer,
  Reading,
  School,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { clearUser, getUser } from '../auth'

const route = useRoute()
const router = useRouter()
const user = getUser()

const roleMenus = {
  ADMIN: [
    { title: '首页概览', path: '/admin/dashboard', icon: Odometer },
    { title: '学生管理', path: '/admin/students', icon: User },
    { title: '教师管理', path: '/admin/teachers', icon: School },
    { title: '班级管理', path: '/admin/classes', icon: Collection },
    { title: '课程管理', path: '/admin/courses', icon: Reading },
    { title: '学期管理', path: '/admin/semesters', icon: Calendar },
    { title: '成绩管理', path: '/admin/scores', icon: DocumentChecked }
  ],
  TEACHER: [
    { title: '我的课程', path: '/teacher/courses', icon: Reading },
    { title: '成绩录入与查询', path: '/teacher/grades', icon: DocumentChecked },
    { title: '班级成绩统计', path: '/teacher/statistics', icon: DataAnalysis }
  ],
  STUDENT: [
    { title: '个人信息', path: '/student/profile', icon: User },
    { title: '我的课程', path: '/student/courses', icon: Reading },
    { title: '我的成绩', path: '/student/scores', icon: DocumentChecked },
    { title: '成绩统计', path: '/student/statistics', icon: DataAnalysis }
  ]
}

const roleNames = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const menus = computed(() => roleMenus[user?.role] || [])
const roleName = computed(() => roleNames[user?.role] || '用户')

function logout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    type: 'warning'
  }).then(() => {
    clearUser()
    router.replace('/login')
  })
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #263445;
}

.brand {
  height: 64px;
  padding: 0 18px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  line-height: 64px;
  white-space: nowrap;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.aside :deep(.el-menu-item.is-active) {
  background: #409eff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.user-area {
  display: flex;
  gap: 18px;
  align-items: center;
  color: #606266;
}

.brand-short {
  display: none;
}

/* 移动端：侧边栏收成图标条，菜单文字与用户名隐藏，避免挤压内容区 */
@media (max-width: 768px) {
  .aside {
    width: 64px !important;
  }

  .brand {
    padding: 0;
    font-size: 14px;
    text-align: center;
  }

  .brand-full {
    display: none;
  }

  .brand-short {
    display: inline;
  }

  .aside :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }

  .aside :deep(.el-menu-item span) {
    display: none;
  }

  .header {
    padding: 0 12px;
  }

  .page-title {
    font-size: 16px;
  }

  .user-area {
    gap: 10px;
  }

  .user-name {
    display: none;
  }
}
</style>
