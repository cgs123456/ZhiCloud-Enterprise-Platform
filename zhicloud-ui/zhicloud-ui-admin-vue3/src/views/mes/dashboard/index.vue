<template>
  <ContentWrap>
    <!-- 生产看板：概览指标 + 工单状态分布 + 近 7 日产量趋势（快照接口，兼容无 WS 场景） -->
    <div class="flex justify-end mb-10px">
      <el-button type="primary" @click="load" :loading="loading">
        <Icon icon="ep:refresh" class="mr-5px" /> 刷新
      </el-button>
    </div>

    <!-- 概览指标 -->
    <el-row :gutter="12" v-if="summary">
      <el-col :span="4" v-for="c in summaryCards" :key="c.label">
        <el-card shadow="hover" class="mb-10px">
          <div class="text-14px text-gray-500">{{ c.label }}</div>
          <div class="text-24px font-bold">{{ c.value ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <!-- 工单状态分布 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>工单状态分布</span></template>
          <el-table :data="workOrderStatus" size="small" stripe>
            <el-table-column label="状态" align="center" prop="statusName" />
            <el-table-column label="数量" align="center" prop="count" />
          </el-table>
        </el-card>
      </el-col>
      <!-- 近 7 日产量趋势 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>近 7 日产量趋势</span></template>
          <el-table :data="productionTrend" size="small" stripe>
            <el-table-column label="日期" align="center" prop="date" />
            <el-table-column label="产量" align="center" prop="quantity" />
            <el-table-column label="合格数" align="center" prop="qualifiedQuantity" />
            <el-table-column label="不合格数" align="center" prop="unqualifiedQuantity" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>
<script setup lang="ts">
import { DashboardApi } from '@/api/mes/dashboard'

defineOptions({ name: 'MesDashboard' })

const loading = ref(false)
const snapshot = ref<any>(null)
const summary = ref<any>(null)
const workOrderStatus = ref<any[]>([])
const productionTrend = ref<any[]>([])

const summaryCards = computed(() => {
  const s = summary.value || {}
  return [
    { label: '进行中工单', value: s.workOrderActiveCount },
    { label: '待开工工单', value: s.workOrderPrepareCount },
    { label: '已完工工单', value: s.workOrderFinishedCount },
    { label: '设备总数', value: s.machineryTotal },
    { label: '生产中设备', value: s.machineryProducing },
    { label: '安灯报警', value: s.andonActiveCount }
  ]
})

const load = async () => {
  loading.value = true
  try {
    const res: any = await DashboardApi.DashboardSnapshot()
    snapshot.value = res
    summary.value = res?.summary ?? null
    workOrderStatus.value = Array.isArray(res?.workOrderStatus) ? res.workOrderStatus : []
    productionTrend.value = Array.isArray(res?.productionTrend) ? res.productionTrend : []
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>
