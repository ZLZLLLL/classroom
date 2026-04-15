<template>
  <div class="courses-page page-full">
    <div class="page-header">
      <div class="header-left">
        <h2>我的课程</h2>
        <p class="desc">管理您的课程内容</p>
      </div>
      <el-button v-if="authStore.canManageCourses" type="primary" @click="showCourseDialog = true">
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
          <div
            class="course-badge"
            v-if="authStore.canManageCourses"
            @click.stop="openClassStudentsDialog(course)"
          >
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
        <div v-if="authStore.canManageCourses" class="course-actions" @click.stop>
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
          <div class="cover-upload-row">
            <el-input v-model="form.coverUrl" readonly placeholder="请通过上传更换课程封面" />
            <el-upload :show-file-list="false" :http-request="handleCoverUpload" accept="image/*">
              <el-button :loading="uploadingCover">上传封面</el-button>
            </el-upload>
          </div>
          <div v-if="form.coverUrl" class="cover-preview">
            <img :src="form.coverUrl" alt="封面预览" />
          </div>
          <div v-else class="cover-preview-empty">暂无封面预览</div>
        </el-form-item>

        <el-form-item v-if="authStore.canManageCourses" label="关联班级" prop="classIds">
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

    <el-dialog
      v-model="showClassStudentsDialog"
      :title="`${selectedCourseForStudents?.name || ''} - 班级学生详情`"
      width="860px"
    >
      <div v-loading="loadingClassStudents" class="class-students-content">
        <el-empty
          v-if="!loadingClassStudents && classStudentsGroups.length === 0"
          description="暂无班级或学生"
        />

        <el-card
          v-for="group in classStudentsGroups"
          :key="group.classId"
          shadow="never"
          class="class-students-card"
        >
          <template #header>
            <div class="class-students-header">
              <div>
                <span class="class-name">{{ group.className }}</span>
                <span class="class-count">（{{ group.studentCount }} 人）</span>
              </div>
              <div class="class-lottery-actions">
                <el-input-number
                  v-model="classLotteryCount[group.classId]"
                  :min="1"
                  :max="Math.max(1, group.studentCount || 1)"
                  size="small"
                />
                <el-button
                  type="primary"
                  size="small"
                  :loading="lotteryLoadingClassId === group.classId"
                  @click="handleClassLottery(group.classId)"
                >随机点名</el-button>
              </div>
            </div>
          </template>

          <el-table :data="group.students" size="small" max-height="260">
            <el-table-column prop="realName" label="姓名" min-width="120">
              <template #default="{ row }">
                {{ row.realName || row.username || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="studentNo" label="学号" min-width="140" />
          </el-table>
        </el-card>
      </div>
      <template #footer>
        <el-button @click="showClassStudentsDialog = false">关闭</el-button>
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
import { getCourseClassStudents, getMyCourses, type Course, type CourseForm, type CourseClassStudents } from '../api/course'
import { drawLottery } from '../api/lottery'
import { uploadFile } from '../api/file'

const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()

const loading = ref(false)
const submitting = ref(false)
const uploadingCover = ref(false)
const coverUploadPromise = ref<Promise<void> | null>(null)
const coverUploadSeq = ref(0)
const showCourseDialog = ref(false)
const editingCourse = ref<Course | null>(null)

const studentPageCourses = ref<Course[]>([])
const studentTotal = ref(0)
const courses = computed(() => (authStore.canManageCourses ? courseStore.courses : studentPageCourses.value))
const total = computed(() => (authStore.canManageCourses ? courseStore.total : studentTotal.value))
const pageSize = 12
const currentPage = ref(1)
const keyword = ref('')

const classList = ref<ClassInfo[]>([])
const showClassStudentsDialog = ref(false)
const selectedCourseForStudents = ref<Course | null>(null)
const loadingClassStudents = ref(false)
const classStudentsGroups = ref<CourseClassStudents[]>([])
const classLotteryCount = reactive<Record<number, number>>({})
const lotteryLoadingClassId = ref<number | null>(null)
const MAX_COVER_SIZE = 10 * 1024 * 1024

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
    if (!authStore.canManageCourses) {
      const allCourses = await getMyCourses()
      const normalizedKeyword = keyword.value.trim().toLowerCase()
      const filteredCourses = !normalizedKeyword
        ? (allCourses || [])
        : (allCourses || []).filter(course =>
            (course.name || '').toLowerCase().includes(normalizedKeyword) ||
            (course.description || '').toLowerCase().includes(normalizedKeyword)
          )
      studentTotal.value = filteredCourses.length
      const start = (currentPage.value - 1) * pageSize
      studentPageCourses.value = filteredCourses.slice(start, start + pageSize)
      return
    }

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
        if (coverUploadPromise.value) {
          await coverUploadPromise.value
        }
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

const openClassStudentsDialog = async (course: Course) => {
  selectedCourseForStudents.value = course
  showClassStudentsDialog.value = true
  loadingClassStudents.value = true
  try {
    classStudentsGroups.value = await getCourseClassStudents(course.id)
    classStudentsGroups.value.forEach(group => {
      classLotteryCount[group.classId] = 1
    })
  } catch (e: any) {
    ElMessage.error(e.message || '获取班级学生详情失败')
    classStudentsGroups.value = []
  } finally {
    loadingClassStudents.value = false
  }
}

const handleClassLottery = async (classId: number) => {
  if (!selectedCourseForStudents.value) return
  const count = classLotteryCount[classId] || 1
  lotteryLoadingClassId.value = classId
  try {
    const selected = await drawLottery(selectedCourseForStudents.value.id, count, classId)
    const names = (selected || []).map((u: any) => u.realName || u.username).join('、')
    ElMessage.success(names ? `点名结果：${names}` : '未抽到学生')
  } catch (e: any) {
    ElMessage.error(e.message || '随机点名失败')
  } finally {
    lotteryLoadingClassId.value = null
  }
}

const handleCoverUpload = async (options: any) => {
  const file = options.file as File
  if (!file?.type?.startsWith('image/')) {
    ElMessage.warning('请上传图片格式的课程封面')
    return
  }
  if (file.size > MAX_COVER_SIZE) {
    ElMessage.warning('课程封面不能超过10MB')
    return
  }

  const currentUploadSeq = ++coverUploadSeq.value
  uploadingCover.value = true

  const task = (async () => {
    try {
      const uploaded = await uploadFile(file, {
        courseId: editingCourse.value?.id,
        type: 1,
        category: 'course-cover',
        persist: false
      })
      form.coverUrl = uploaded.filePath || uploaded.fileUrl || ''
      ElMessage.success('封面上传成功')
      options.onSuccess?.(uploaded)
    } catch (e) {
      ElMessage.error('封面上传失败')
      options.onError?.(e)
      throw e
    } finally {
      if (coverUploadSeq.value === currentUploadSeq) {
        uploadingCover.value = false
        coverUploadPromise.value = null
      }
    }
  })()

  coverUploadPromise.value = task
  await task
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
  cursor: pointer;
}

.course-badge:hover {
  background: rgba(0, 0, 0, 0.65);
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
  line-clamp: 2;
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

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.class-students-content {
  display: grid;
  gap: 12px;
}

.class-students-card {
  border: 1px solid #ebe5dc;
}

.class-students-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.class-name {
  font-weight: 600;
  color: #3d3225;
}

.class-count {
  color: #8b7355;
  font-size: 13px;
}

.class-lottery-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover-upload-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
}

.cover-preview {
  margin-top: 10px;
  width: 100%;
  max-width: 300px;
  border: 1px solid #e6dfd5;
  border-radius: 10px;
  overflow: hidden;
  background: #faf8f5;
}

.cover-preview img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.cover-preview-empty {
  margin-top: 10px;
  color: #a08d78;
  font-size: 13px;
}
</style>
