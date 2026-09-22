<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="课程编号或名称"
        clearable
        style="width: 220px"
        @keyup.enter="loadCourses"
      />
      <el-select v-model="query.semesterId" placeholder="全部学期" clearable style="width: 210px">
        <el-option
          v-for="item in semesters"
          :key="item.id"
          :label="item.semesterName"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="query.teacherId" placeholder="全部教师" clearable style="width: 150px">
        <el-option v-for="item in teachers" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" @click="searchCourses">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增课程</el-button>
    </div>

    <el-table v-loading="loading" :data="courses" stripe>
      <el-table-column prop="courseCode" label="课程编号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="hours" label="学时" width="80" />
      <el-table-column prop="courseType" label="类型" width="90" />
      <el-table-column prop="semesterName" label="学期" min-width="190" />
      <el-table-column prop="teacherName" label="授课教师" width="110" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成绩状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.scoreStatus === 1 ? 'success' : 'warning'">
            {{ row.scoreStatus === 1 ? '已提交' : '录入中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="openStudents(row)">学生名单</el-button>
          <el-button v-if="row.scoreStatus === 1" link type="warning" @click="unlockScores(row)"
            >解锁成绩</el-button
          >
          <el-button link type="danger" @click="removeCourse(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程编号" required>
          <el-input v-model="form.courseCode" />
        </el-form-item>
        <el-form-item label="课程名称" required>
          <el-input v-model="form.courseName" />
        </el-form-item>
        <el-form-item label="学分" required>
          <el-input-number
            v-model="form.credit"
            :min="0.5"
            :max="10"
            :step="0.5"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="学时" required>
          <el-input-number v-model="form.hours" :min="1" :max="200" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="form.courseType" style="width: 100%">
            <el-option label="必修" value="必修" />
            <el-option label="选修" value="选修" />
          </el-select>
        </el-form-item>
        <el-form-item label="学期" required>
          <el-select v-model="form.semesterId" style="width: 100%">
            <el-option
              v-for="item in semesters"
              :key="item.id"
              :label="item.semesterName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" required>
          <el-select v-model="form.teacherId" style="width: 100%">
            <el-option
              v-for="item in teachers"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="studentDialogVisible"
      :title="`学生名单 - ${currentCourse?.courseName || ''}`"
      width="720px"
    >
      <el-alert
        v-if="affectedScoreCount > 0"
        type="warning"
        :closable="false"
        show-icon
        :title="`当前改动会移出 ${affectedScoreCount} 名已有成绩的学生，保存时将同步删除对应成绩`"
        style="margin-bottom: 12px"
      />
      <el-transfer
        v-model="selectedStudentIds"
        filterable
        :data="studentOptions"
        :titles="['未选学生', '课程学生']"
        :props="{ key: 'id', label: 'label' }"
      />
      <template #footer>
        <el-button @click="studentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStudents" @click="saveStudents"
          >保存名单</el-button
        >
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addCourse,
  assignCourseStudents,
  deleteCourse,
  getCourseStudents,
  getCourses,
  getSemesters,
  getStudents,
  getTeachers,
  unlockCourseScores,
  updateCourse
} from '../../api/admin'

const loading = ref(false)
const saving = ref(false)
const savingStudents = ref(false)
const dialogVisible = ref(false)
const studentDialogVisible = ref(false)
const courses = ref([])
const semesters = ref([])
const teachers = ref([])
const students = ref([])
const selectedStudentIds = ref([])
const originalStudentIds = ref([])
const scoredStudentIds = ref([])
const currentCourse = ref(null)
const query = reactive({
  keyword: '',
  semesterId: null,
  teacherId: null
})
const pagination = reactive({ page: 1, size: 10, total: 0 })
const emptyForm = {
  id: null,
  courseCode: '',
  courseName: '',
  credit: 2,
  hours: 32,
  courseType: '必修',
  semesterId: null,
  teacherId: null,
  status: 1
}
const form = reactive({ ...emptyForm })

const studentOptions = computed(() =>
  students.value.map((student) => ({
    id: student.id,
    label: `${student.studentNo} ${student.name}（${student.className}）`
  }))
)

/** 保存名单后会被连带删除的成绩条数。 */
const affectedScoreCount = computed(() => {
  const selected = new Set(selectedStudentIds.value)
  return scoredStudentIds.value.filter((id) => !selected.has(id)).length
})

async function loadOptions() {
  // 学生名单在打开名单弹窗时按需加载，避免进入课程管理页就拉取全部学生
  const [semesterList, teacherList] = await Promise.all([getSemesters(), getTeachers({})])
  semesters.value = semesterList
  teachers.value = teacherList
}

async function loadCourses() {
  loading.value = true
  try {
    const data = await getCourses({ ...query, page: pagination.page, size: pagination.size })
    courses.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

function searchCourses() {
  pagination.page = 1
  loadCourses()
}

function handlePageChange(page) {
  pagination.page = page
  loadCourses()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadCourses()
}

function resetQuery() {
  query.keyword = ''
  query.semesterId = null
  query.teacherId = null
  pagination.page = 1
  loadCourses()
}

function openAdd() {
  Object.assign(form, emptyForm)
  form.semesterId = semesters.value.find((item) => item.isCurrent === 1)?.id || null
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveCourse() {
  saving.value = true
  try {
    if (form.id) {
      await updateCourse(form.id, form)
    } else {
      await addCourse(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadCourses()
  } finally {
    saving.value = false
  }
}

async function openStudents(row) {
  currentCourse.value = row
  if (!students.value.length) {
    students.value = await getStudents({})
  }
  const assigned = await getCourseStudents(row.id)
  originalStudentIds.value = assigned.map((item) => item.studentId)
  scoredStudentIds.value = assigned
    .filter((item) => item.scoreId !== null && item.scoreId !== undefined)
    .map((item) => item.studentId)
  selectedStudentIds.value = [...originalStudentIds.value]
  studentDialogVisible.value = true
}

async function saveStudents() {
  const affected = affectedScoreCount.value
  if (affected > 0) {
    try {
      await ElMessageBox.confirm(
        `有 ${affected} 名被移出名单的学生已有成绩，保存后将同时删除这 ${affected} 条成绩记录，是否继续？`,
        '删除成绩确认',
        { type: 'warning', confirmButtonText: '继续保存', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  savingStudents.value = true
  try {
    const removedCount = await assignCourseStudents(
      currentCourse.value.id,
      selectedStudentIds.value
    )
    ElMessage.success(
      removedCount > 0 ? `学生名单已保存，同时删除了 ${removedCount} 条成绩` : '学生名单已保存'
    )
    studentDialogVisible.value = false
  } finally {
    savingStudents.value = false
  }
}

function removeCourse(row) {
  ElMessageBox.confirm(`确定删除课程 ${row.courseName} 吗？相关成绩也会删除。`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deleteCourse(row.id)
      ElMessage.success('删除成功')
      await loadCourses()
    })
    .catch(() => {})
}

function unlockScores(row) {
  ElMessageBox.confirm(
    `确定解锁 ${row.courseName} 的成绩吗？解锁后该课程教师可以继续修改成绩。`,
    '解锁确认',
    { type: 'warning' }
  )
    .then(async () => {
      await unlockCourseScores(row.id)
      ElMessage.success('成绩已解锁')
      await loadCourses()
    })
    .catch(() => {})
}

onMounted(async () => {
  await loadOptions()
  await loadCourses()
})
</script>
