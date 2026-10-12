<template>
  <ContentWrap>
    <!-- 工单齐套分析：汇总 + 齐套明细 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="生产工单" prop="workOrderId">
        <el-input-number v-model="queryParams.workOrderId" :controls="false" placeholder="请输入生产工单" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 分析</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 汇总 -->
  <ContentWrap v-if="summary">
    <el-descriptions :column="4" border>
      <el-descriptions-item label="工单编号">{{ summary.workOrderCode ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="工单名称">{{ summary.workOrderName ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="产品">{{ summary.productCode }} {{ summary.productName }}</el-descriptions-item>
      <el-descriptions-item label="工单数量">{{ summary.workOrderQuantity ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="需求日期">{{ summary.requestDate ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="物料行数">{{ summary.totalLineCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="已齐套">{{ summary.fullyKittedCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="部分齐套">{{ summary.partialCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="缺料">{{ summary.shortageCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="齐套率">{{ summary.kittingRate ?? '-' }}%</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <!-- 齐套明细 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="rows" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="物料编码" align="center" prop="itemCode" />
      <el-table-column label="物料名称" align="center" prop="itemName" min-width="140" show-overflow-tooltip />
      <el-table-column label="规格" align="center" prop="specification" />
      <el-table-column label="BOM 用量" align="center" prop="bomQuantity" />
      <el-table-column label="需求数量" align="center" prop="requiredQuantity" />
      <el-table-column label="库存数量" align="center" prop="stockQuantity" />
      <el-table-column label="在途数量" align="center" prop="inTransitQuantity" />
      <el-table-column label="可用数量" align="center" prop="availableQuantity" />
      <el-table-column label="缺料数量" align="center" prop="shortageQuantity" />
      <el-table-column label="齐套状态" align="center" prop="kittingStatusName" />
      <el-table-column label="最早到货" align="center" prop="earliestArrivalDate" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { WorkOrderKittingApi } from '@/api/mes/pro/work-order-kitting'

defineOptions({ name: 'MesWorkOrderKitting' })

const message = useMessage()

const loading = ref(false)
const rows = ref<any[]>([])
const summary = ref<any>(null)
const queryParams = reactive({
  workOrderId: undefined as any
})

const handleQuery = async () => {
  if (!queryParams.workOrderId) {
    message.warning('请输入生产工单')
    return
  }
  loading.value = true
  try {
    const res: any = await WorkOrderKittingApi.WorkOrderKittingAnalyze(queryParams)
    summary.value = res || null
    rows.value = res && Array.isArray(res.lines) ? res.lines : []
  } finally {
    loading.value = false
  }
}
</script>
