<template>
  <div class="classes-page page-full">
    <div class="page-header">
      <div class="header-left">
        <h2>班级管理</h2>
        <p class="desc">管理您的教学班级</p>
      </div>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon>
        创建班级
      </el-button>
    </div>

    <el-table :data="classList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="班级名称" />
      <el-table-column prop="grade" label="年级" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="studentCount" label="学生人数" width="120" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link @click="editClass(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showDialog"
      :title="editingClass ? '编辑班级' : '创建班级'"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="form.name" placeholder="如：计算机2101" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="如：2021" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="如：计算机科学与技术" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getClassList, createClass, updateClass, deleteClass, type ClassInfo } from '../api/class'

const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const editingClass = ref<ClassInfo | null>(null)
const classList = ref<ClassInfo[]>([])

const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  grade: '',
  major: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

onMounted(fetchClasses)

async function fetchClasses() {
  loading.value = true
  try {
    classList.value = await getClassList()
  } finally {
    loading.value = false
  }
}

function editClass(cls: ClassInfo) {
  editingClass.value = cls
  form.name = cls.name
  form.grade = cls.grade
  form.major = cls.major
  form.description = cls.description
  showDialog.value = true
}

async function handleDelete(cls: ClassInfo) {
  try {
    await ElMessageBox.confirm(`确定要删除班级「${cls.name}」吗？`, '提示', { type: 'warning' })
    await deleteClass(cls.id)
    ElMessage.success('删除成功')
    fetchClasses()
  } catch (e) {
    // cancel
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (editingClass.value) {
          await updateClass(editingClass.value.id, form)
          ElMessage.success('更新成功')
        } else {
          await createClass(form)
          ElMessage.success('创建成功')
        }
        showDialog.value = false
        resetForm()
        fetchClasses()
      } finally {
        submitting.value = false
      }
    }
  })
}

function resetForm() {
  editingClass.value = null
  form.name = ''
  form.grade = ''
  form.major = ''
  form.description = ''
}
</script>

<style scoped>
.classes-page {
  width: 100%;
  min-height: 100%;
}
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }
</style>
