<template>
  <div class="ranking-page">
    <div class="page-header">
      <h2>积分排行</h2>
      <p class="desc">查看课程积分排名</p>
    </div>

    <el-card shadow="never">
      <el-select v-model="selectedCourse" placeholder="请选择课程" style="width: 300px; margin-bottom: 20px;">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>

      <el-table :data="rankingList" v-loading="loading">
        <el-table-column prop="rank" label="排名" width="80">
          <template #default="{ $index }">
            <span class="rank" :class="rankBadgeClass($index)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学号" min-width="140">
          <template #default="{ row }">
            {{ row.studentNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="180">
          <template #default="{ row }">
            <div class="student-info">
              <el-avatar :size="36" :src="row.avatar">{{ (row.realName || row.userName || '-')?.charAt(0) }}</el-avatar>
              <span>{{ row.realName || row.userName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分">
          <template #default="{ row }">
            <span class="points">{{ row.points }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getCourseList } from '../api/course'
import { getCoursePointsRanking } from '../api/points'

const loading = ref(false)
const courses = ref<any[]>([])
const selectedCourse = ref<number | null>(null)
const rankingList = ref<any[]>([])

const rankBadgeClass = (index: number) => ({
  'rank-first': index === 0,
  'rank-second': index === 1,
  'rank-third': index === 2
})


onMounted(async () => {
  try {
    const res = await getCourseList({ page: 1, size: 100 })
    courses.value = res.records
    if (courses.value.length > 0) {
      selectedCourse.value = courses.value[0].id
    }
  } catch (e) {
    // ignore
  }
})

watch(selectedCourse, async (val) => {
  if (val) {
    loading.value = true
    try {
      rankingList.value = await getCoursePointsRanking(val)
    } catch (e) {
      rankingList.value = []
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 600; color: #3d3225; margin: 0 0 4px; }
.desc { color: #8b7355; font-size: 14px; margin: 0; }

.rank {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  font-weight: 600;
}

.rank-first { background: linear-gradient(135deg, #ffd700, #ffb700); color: #fff; }
.rank-second { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); color: #fff; }
.rank-third { background: linear-gradient(135deg, #cd7f32, #b06020); color: #fff; }

.student-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.points {
  font-weight: 600;
  color: #d4a574;
  font-size: 16px;
}
</style>
