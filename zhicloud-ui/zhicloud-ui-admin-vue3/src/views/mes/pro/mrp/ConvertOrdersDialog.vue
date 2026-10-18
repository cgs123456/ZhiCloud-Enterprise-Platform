<!-- MRP 转出工单回链对话框（结果行 → 工单；§12.270 D7；历史已转计划无链可回溯） -->
<template>
  <Dialog v-model="dialogVisible" :title="`转出工单 - ${planNo}`" width="860">
    <el-table v-if="list.length > 0" v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="结果行" align="center" prop="resultId" width="90" />
      <el-table-column label="产品物料" align="center">
        <template #default="scope">{{ scope.row.productName ?? scope.row.productId }}</template>
      </el-table-column>
      <el-table-column label="计划订单量" align="center" prop="plannedOrderQty" width="110" />
      <el-table-column label="工单编码" align="center" prop="workOrderCode" width="170" />
      <el-table-column label="工单类型" align="center" width="110">
        <template #default="scope">
          {{ WORK_ORDER_TYPE_LABEL[scope.row.workOrderType] ?? scope.row.workOrderType }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else v-loading="loading" description="暂无回链（历史已转计划无行级回链）" />
  </Dialog>
</template>

<script setup lang="ts">
import { MrpApi } from '@/api/mes/pro/mrp'

defineOptions({ name: 'MrpConvertOrdersDialog' })

// 工单类型（对齐 MesProWorkOrderTypeEnum：1 自行生产 / 2 代工 / 3 采购）
const WORK_ORDER_TYPE_LABEL: Record<number, string> = {
  1: '自行生产',
  2: '代工',
  3: '采购'
}

const dialogVisible = ref(false) // 弹窗的是否展示
const planNo = ref('') // 当前计划编号
const loading = ref(false) // 列表的加载中
const list = ref<any[]>([]) // 回链列表

/** 打开弹窗并加载回链 */
const open = async (row: any) => {
  planNo.value = row?.planNo || ''
  dialogVisible.value = true
  loading.value = true
  try {
    list.value = (await MrpApi.getMrpConvertLink(row.id)) || []
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
