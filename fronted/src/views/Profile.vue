<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人资料</h2>
      <p class="desc">查看和修改个人信息</p>
    </div>

    <el-card shadow="never" class="profile-card">
      <el-form :model="form" label-position="top" style="max-width: 500px;">
        <el-form-item label="头像">
          <el-avatar :size="80" :src="form.avatar">{{ form.realName?.charAt(0) }}</el-avatar>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>

        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { updateCurrentUser } from '../api/user'

const authStore = useAuthStore()
const saving = ref(false)

const form = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  avatar: ''
})

onMounted(() => {
  if (authStore.user) {
    form.username = authStore.user.username
    form.realName = authStore.user.realName
    form.email = authStore.user.email || ''
    form.phone = authStore.user.phone || ''
    form.avatar = authStore.user.avatar || ''
  }
})

async function handleSave() {
  saving.value = true
  try {
    const updated = await updateCurrentUser({
      realName: form.realName,
      email: form.email,
      phone: form.phone
    })
    authStore.user = updated
    localStorage.setItem('user', JSON.stringify(updated))
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-header {
  margin-bottom: 32px;
}
.page-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 8px;
}
.desc {
  color: #8b7355;
  font-size: 14px;
  margin: 0;
}
.profile-card {
  border-radius: 16px;
  padding: 20px;
}
.profile-card :deep(.el-form-item) {
  margin-bottom: 24px;
}
.profile-card :deep(.el-form-item__label) {
  font-size: 15px;
  color: #3d3225;
}
.profile-card :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}
.profile-card :deep(.el-avatar) {
  width: 100px;
  height: 100px;
  font-size: 40px;
}
</style>
