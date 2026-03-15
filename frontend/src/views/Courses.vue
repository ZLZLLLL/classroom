<template>
  <div class="courses-page page-full">
    <div class="page-header">
      <div class="header-left">
        <h2>我的课程</h2>
        <p class="desc">管理您的课程内容</p>
      </div>
      <el-button v-if="authStore.isTeacher" type="primary" @click="showCourseDialog = true">
        <el-icon><Plus /></el-icon>
        创建课程
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索课程..."
        clearable
        @change="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 课程列表 -->
    <div v-loading="loading" class="course-grid">
      <div
        v-for="course in courses"
        :key="course.id"
        class="course-card"
        @click="goToCourse(course.id)"
      >
        <div class="course-cover">
          <img v-if="course.coverUrl" :src="course.coverUrl" alt="课程封面" />
          <div v-else class="cover-placeholder">
            <el-icon :size="48"><Reading /></el-icon>
          </div>
          <div class="course-badge" v-if="authStore.isTeacher">
            {{ course.studentCount }} 人
          </div>
        </div>
        <div class="course-info">
          <h3>{{ course.name }}</h3>
          <p class="course-desc">{{ course.description || '暂无描述' }}</p>
          <div class="course-meta">
            <span v-if="course.classNames?.length" class="classes">
              <el-icon><Collection /></el-icon>
              {{ course.classNames.slice(0, 2).join('、') }}
              <span v-if="course.classNames.length > 2">等{{ course.classNames.length }}个班级</span>
            </span>
          </div>
        </div>
        <div v-if="authStore.isTeacher" class="course-actions" @click.stop>
          <el-button type="primary" link @click="editCourse(course)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button type="danger" link @click="handleDelete(course)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchCourses"
      />
    </div>

    <!-- 课程对话框 -->
    <el-dialog
      v-model="showCourseDialog"
      :title="editingCourse ? '编辑课程' : '创建课程'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" />
        </el-form-item>

        <el-form-item label="课程描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程描述"
          />
        </el-form-item>

        <el-form-item label="课程封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="请输入封面图片URL" />
        </el-form-item>

        <el-form-item v-if="authStore.isTeacher" label="关联班级" prop="classIds">
          <el-select
            v-model="form.classIds"
            multiple
            placeholder="请选择班级"
            style="width: 100%"
          >
            <el-option
              v-for="cls in classList"
              :key="cls.id"
              :label="`${cls.name} (${cls.grade}级 ${cls.major})`"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCourseDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingCourse ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Reading, Collection, Edit, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useCourseStore } from '../stores/course'
import { getClassList, type ClassInfo } from '../api/class'
import type { Course, CourseForm } from '../api/course'

const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()

const loading = ref(false)
const submitting = ref(false)
const showCourseDialog = ref(false)
const editingCourse = ref<Course | null>(null)

const courses = computed(() => courseStore.courses)
const total = computed(() => courseStore.total)
const pageSize = 12
const currentPage = ref(1)
const keyword = ref('')

const classList = ref<ClassInfo[]>([])

const formRef = ref<FormInstance>()
const form = reactive<CourseForm>({
  name: '',
  description: '',
  coverUrl: '',
  classIds: []
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

onMounted(() => {
  fetchCourses()
  fetchClasses()
})

const fetchCourses = async () => {
  loading.value = true
  try {
    await courseStore.fetchCourses({
      page: currentPage.value,
      size: pageSize,
      keyword: keyword.value || undefined
    })
  } finally {
    loading.value = false
  }
}

const fetchClasses = async () => {
  try {
    classList.value = await getClassList()
  } catch (e) {
    // ignore
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchCourses()
}

const goToCourse = (id: number) => {
  router.push(`/courses/${id}`)
}

const editCourse = (course: Course) => {
  editingCourse.value = course
  form.name = course.name
  form.description = course.description
  form.coverUrl = course.coverUrl
  form.classIds = course.classIds
  showCourseDialog.value = true
}

const handleDelete = async (course: Course) => {
  try {
    await ElMessageBox.confirm(`确定要删除课程「${course.name}」吗？`, '提示', {
      type: 'warning'
    })
    await courseStore.removeCourse(course.id)
    ElMessage.success('删除成功')
  } catch (e) {
    // cancel
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingCourse.value) {
          await courseStore.editCourse(editingCourse.value.id, form)
          ElMessage.success('更新成功')
        } else {
          await courseStore.addCourse(form)
          ElMessage.success('创建成功')
        }
        showCourseDialog.value = false
        resetForm()
        fetchCourses()
      } finally {
        submitting.value = false
      }
    }
  })
}

const resetForm = () => {
  editingCourse.value = null
  form.name = ''
  form.description = ''
  form.coverUrl = ''
  form.classIds = []
}
</script>

<style scoped>
.courses-page {
  width: 100%;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 4px;
}

.desc {
  color: #8b7355;
  font-size: 14px;
  margin: 0;
}

.search-bar {
  margin-bottom: 24px;
}

.search-bar .el-input {
  max-width: 320px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e8e0d5;
  position: relative;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(61, 50, 37, 0.12);
}

.course-cover {
  height: 160px;
  background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);
  position: relative;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
}

.course-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.course-info {
  padding: 16px;
}

.course-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-desc {
  color: #8b7355;
  font-size: 13px;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px;
}

.course-meta {
  font-size: 12px;
  color: #a08060;
}

.classes {
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-actions {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
}

.course-actions .el-button {
  background: white;
  border-radius: 8px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
