<!-- OA 工作台门户 -->
<template>
  <div v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-16px">
      <el-col :span="4" v-for="card in cards" :key="card.prop">
        <ContentWrap>
          <div class="text-14px text-gray-500">{{ card.label }}</div>
          <div class="mt-8px text-24px font-bold">{{ dashboard[card.prop] ?? 0 }}</div>
        </ContentWrap>
      </el-col>
    </el-row>

    <!-- 最新公告 -->
    <ContentWrap title="最新公告">
      <el-table :data="dashboard.announcements || []" :show-overflow-tooltip="true" :stripe="true">
        <el-table-column align="center" label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column align="center" label="类型" prop="type" min-width="100" />
        <el-table-column align="center" label="状态" prop="status" min-width="100" />
        <el-table-column align="center" :formatter="dateFormatter" label="发布时间" prop="publishTime" width="170" />
      </el-table>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { OaPortalApi } from '@/api/oa/portal'

defineOptions({ name: 'OaPortal' })

const loading = ref(true)
const dashboard = ref<any>({})

const cards = [
  { label: '待办事项', prop: 'todoCount' },
  { label: '今日会议', prop: 'todayMeetingCount' },
  { label: '待处理报销', prop: 'pendingReimburseCount' },
  { label: '我的报销', prop: 'myReimburseCount' },
  { label: '我的公文', prop: 'myDocumentsCount' },
  { label: '已发布知识', prop: 'publishedKnowledgeCount' }
]

const getDashboard = async () => {
  loading.value = true
  try {
    dashboard.value = await OaPortalApi.getOaPortalDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(() => getDashboard())
</script>
