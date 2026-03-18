<template>
  <div class="admin-users-page page-full">
    <div class="page-header">
      <h2>用户管理</h2>
      <p class="desc">管理员可管理教师/学生并重置密码</p>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="roleFilter" style="width: 140px" @change="handleSearch">
          <el-option label="全部角色" :value="0" />
          <el-option label="教师" :value="1" />
          <el-option label="学生" :value="2" />
        </el-select>
        <el-input v-model="keyword" placeholder="按姓名/用户名搜索" clearable style="width: 280px" @change="handleSearch" @clear="handleSearch" />
      </div>

      <el-table :data="users" v-loading="loading">
        <el-table-column prop="realName" label="姓名" min-width="120">
          <template #default="{ row }">
            {{ row.realName || row.username }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'warning' : 'success'">
              {{ row.role === 1 ? '教师' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="140">
          <template #default="{ row }">
            {{ row.className || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openResetDialog(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="showResetDialog" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="目标用户">
          <el-input :model-value="resetTargetText" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetDialog = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getManageableUsers, resetUserPassword, updateUserStatus } from '@/api/user'
import type { UserInfo } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const users = ref<UserInfo[]>([])
const total = ref(0)
const pageSize = 12
const currentPage = ref(1)
const roleFilter = ref(0)
const keyword = ref('')

const showResetDialog = ref(false)
const resetting = ref(false)
const resetUserId = ref<number | null>(null)
const resetTargetText = ref('')
const newPassword = ref('')

onMounted(() => {
  if (!authStore.isAdmin) {
    ElMessage.warning('仅管理员可访问该页面')
    router.replace('/dashboard')
    return
  }
  fetchUsers()
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getManageableUsers({
      page: currentPage.value,
      size: pageSize,
      role: roleFilter.value === 0 ? undefined : roleFilter.value,
      keyword: keyword.value || undefined
    })
    users.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

const openResetDialog = (row: UserInfo) => {
  resetUserId.value = row.id
  resetTargetText.value = `${row.realName || row.username} (${row.username})`
  newPassword.value = ''
  showResetDialog.value = true
}

const handleResetPassword = async () => {
  if (!resetUserId.value) return
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }

  resetting.value = true
  try {
    await resetUserPassword(resetUserId.value, newPassword.value)
    ElMessage.success('密码重置成功')
    showResetDialog.value = false
  } catch {
    // handled by interceptor
  } finally {
    resetting.value = false
  }
}

const handleToggleStatus = async (row: UserInfo) => {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, targetStatus as 0 | 1)
    ElMessage.success(targetStatus === 0 ? '用户已封禁' : '用户已解封')
    await fetchUsers()
  } catch {
    // handled by interceptor
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 24px; color: #3d3225; }
.desc { margin: 0; color: #8b7355; font-size: 14px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>



