<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="48"><Reading /></el-icon>
        </div>
        <h1>课堂互动系统</h1>
        <p class="subtitle">让教学更高效，让学习更有趣</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="showRegister = true">立即注册</el-link>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog
      v-model="showRegister"
      title="用户注册"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio :value="1">教师</el-radio>
            <el-radio :value="2">学生</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="registerForm.role === 2" label="班级" prop="classId">
          <el-select v-model="registerForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="cls in classList"
              :key="cls.id"
              :label="cls.name"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">
          注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Reading } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { getClassList } from '../api/class'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  role: 2,
  classId: undefined as number | undefined
})

const registerRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const classList = ref<any[]>([])

onMounted(async () => {
  if (authStore.isLoggedIn) {
    router.push('/dashboard')
    return
  }
  try {
    classList.value = await getClassList()
  } catch (e) {
    // ignore
  }
})

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.login(form.username, form.password)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (e) {
        // error handled by interceptor
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      try {
        await authStore.register(registerForm)
        ElMessage.success('注册成功，请登录')
        showRegister.value = false
        form.username = registerForm.username
        form.password = ''
      } catch (e) {
        // error handled by interceptor
      } finally {
        registerLoading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f0e8 0%, #e8e0d5 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
}

.shape-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #d4a574 0%, #c4956a 100%);
  top: -200px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #8b7355 0%, #6d5a45 100%);
  bottom: -100px;
  left: -100px;
  animation: float 25s ease-in-out infinite reverse;
}

.shape-3 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #a08060 0%, #8b7355 100%);
  top: 50%;
  left: 20%;
  animation: float 18s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(20px, -20px) rotate(5deg); }
  50% { transform: translate(0, -40px) rotate(0deg); }
  75% { transform: translate(-20px, -20px) rotate(-5deg); }
}

.login-card {
  position: relative;
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow:
    0 4px 24px rgba(139, 115, 85, 0.1),
    0 1px 2px rgba(139, 115, 85, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(139, 115, 85, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(212, 165, 116, 0.3);
}

.login-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #3d3225;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.subtitle {
  color: #8b7355;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

.login-form :deep(.el-input__wrapper) {
  background: #faf8f5;
  border: 1px solid #e5dfd5;
  border-radius: 12px;
  box-shadow: none;
  padding: 4px 16px;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #d4a574;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #d4a574;
  box-shadow: 0 0 0 3px rgba(212, 165, 116, 0.15);
}

.login-form :deep(.el-input__inner) {
  color: #3d3225;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #a08060;
}

.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #d4a574 0%, #b8956a 100%);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(212, 165, 116, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  color: #8b7355;
  font-size: 14px;
}
</style>
