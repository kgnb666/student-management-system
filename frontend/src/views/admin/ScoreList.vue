<template>
  <el-card class="page-card" shadow="never">
    <div class="toolbar">
      <el-select v-model="query.semesterId" placeholder="全部学期" clearable style="width: 210px">
        <el-option
          v-for="item in semesters"
          :key="item.id"
          :label="item.semesterName"
          :value="item.id"
        />
      </el-select>
      <el-select
        v-model="query.courseId"
        placeholder="全部课程"
        clearable
        filterable
        remote
        :remote-method="searchCourses"
        :loading="courseLoading"
        style="width: 190px"
      >
        <el-option
          v-for="item in courses"
          :key="item.id"
          :label="item.courseName"
          :value="item.id"
        />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="学号、姓名或课程"
        clearable
        style="width: 200px"
        @keyup.enter="loadScores"
      />
      <el-button type="primary" @click="searchScores">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openAdd">新增成绩</el-button>
    </div>

    <el-table v-loading="loading" :data="scores" stripe>
      <el-table-column prop="semesterName" label="学期" min-width="190" />
      <el-table-column prop="courseName" label="课程" min-width="140" />
      <el-table-column prop="studentNo" label="学号" width="110" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column prop="className" label="班级" min-width="150" />
      <el-table-column prop="usualScore" label="平时成绩" width="100" />
      <el-table-column prop="examScore" label="期末成绩" width="100" />
      <el-table-column prop="finalScore" label="总评成绩" width="100">
        <template #default="{ row }">
          <span :class="{ danger: row.finalScore < 60 }">{{ row.finalScore ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="updaterName" label="最后修改人" width="110">
        <template #default="{ row }">{{ row.updaterName ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="info" @click="openLogs(row)">变更记录</el-button>
          <el-button link type="danger" @click="removeScore(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑成绩' : '新增成绩'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程" required>
          <el-select
            v-model="form.courseId"
            filterable
            :disabled="!!form.id"
            style="width: 100%"
            @change="loadDialogStudents"
          >
            <el-option
              v-for="item in courses"
              :key="item.id"
              :label="item.courseName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生" required>
          <el-select v-model="form.studentId" filterable :disabled="!!form.id" style="width: 100%">
            <el-option
              v-for="item in dialogStudents"
              :key="item.studentId"
              :label="`${item.studentNo} ${item.studentName}`"
              :value="item.studentId"
            />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="form.id"
          type="info"
          :closable="false"
          show-icon
          title="编辑成绩时不允许更换课程和学生，避免成绩被移动到其他课程"
          style="margin-bottom: 12px"
        />
        <el-form-item label="平时成绩">
          <el-input-number
            v-model="form.usualScore"
            :min="0"
            :max="100"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="期末成绩">
          <el-input-number
            v-model="form.examScore"
            :min="0"
            :max="100"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总评成绩">
          <el-input :model-value="finalScore" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScore">保存</el-button>
      </template>
    </el-dialog>

    <ScoreChangeLogDrawer
      v-model="logVisible"
      :course-id="logCourseId"
      :student-id="logStudentId"
      :title="logTitle"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addScore,
  deleteScore,
  getCourseStudents,
  getCourses,
  getScores,
  getSemesters,
  updateScore
} from '../../api/admin'
import ScoreChangeLogDrawer from '../../components/ScoreChangeLogDrawer.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const scores = ref([])
const courses = ref([])
const courseLoading = ref(false)
const semesters = ref([])
const dialogStudents = ref([])
const logVisible = ref(false)
const logCourseId = ref(null)
const logStudentId = ref(null)
const logTitle = ref('成绩变更记录')
const query = reactive({
  semesterId: null,
  courseId: null,
  keyword: ''
})
const pagination = reactive({ page: 1, size: 10, total: 0 })
const emptyForm = {
  id: null,
  courseId: null,
  studentId: null,
  usualScore: null,
  examScore: null
}
const form = reactive({ ...emptyForm })

const finalScore = computed(() => {
  if (form.usualScore === null || form.examScore === null) {
    return ''
  }
  return (form.usualScore * 0.3 + form.examScore * 0.7).toFixed(1)
})

async function loadScores() {
  loading.value = true
  try {
    const data = await getScores({ ...query, page: pagination.page, size: pagination.size })
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

/**
 * 课程下拉改为远程搜索：默认只取前 20 条，输入关键字时再查询，
 * 避免课程数量增长后一次性加载全量数据。
 */
async function searchCourses(keyword = '') {
  courseLoading.value = true
  try {
    const data = await getCourses({ keyword, page: 1, size: 20 })
    courses.value = data.records
  } finally {
    courseLoading.value = false
  }
}

async function loadOptions() {
  semesters.value = await getSemesters()
  await searchCourses()
}

async function loadDialogStudents() {
  form.studentId = null
  dialogStudents.value = form.courseId ? await getCourseStudents(form.courseId) : []
}

function resetQuery() {
  query.semesterId = null
  query.courseId = null
  query.keyword = ''
  pagination.page = 1
  loadScores()
}

async function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

async function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    courseId: row.courseId,
    studentId: row.studentId,
    usualScore: row.usualScore,
    examScore: row.examScore
  })
  dialogStudents.value = await getCourseStudents(row.courseId)
  // 编辑时课程不可更换，但下拉需要有对应选项才能正确回显
  if (!courses.value.some((item) => item.id === row.courseId)) {
    courses.value = [{ id: row.courseId, courseName: row.courseName }, ...courses.value]
  }
  dialogVisible.value = true
}

async function saveScore() {
  saving.value = true
  try {
    if (form.id) {
      await updateScore(form.id, form)
    } else {
      await addScore(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadScores()
  } finally {
    saving.value = false
  }
}

function removeScore(row) {
  ElMessageBox.confirm(`确定删除 ${row.studentName} 的 ${row.courseName} 成绩吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      await deleteScore(row.id)
      ElMessage.success('删除成功')
      await loadScores()
    })
    .catch(() => {})
}

function openLogs(row) {
  logCourseId.value = row.courseId
  logStudentId.value = row.studentId
  logTitle.value = `${row.studentName} - ${row.courseName} 变更记录`
  logVisible.value = true
}

onMounted(async () => {
  await loadOptions()
  await loadScores()
})
</script>

<style scoped>
.danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
