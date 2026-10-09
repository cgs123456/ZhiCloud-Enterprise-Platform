<template>
  <doc-alert title="【库存】库存对账" url="https://doc.zhicloud.cn/erp/stock/" />

  <ContentWrap>
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <div>
        <div class="text-16px font-bold">库存日终对账</div>
        <div class="mt-4px text-13px text-gray-500">
          真值源 inventory_item 与 ERP/MES 投影逐项比对；仅手动触发，不注册任何自动排程。
        </div>
      </div>
      <div>
        <el-button type="primary" :loading="running" @click="handleRun">
          <Icon icon="ep:refresh" class="mr-5px" /> 执行对账
        </el-button>
        <el-button
          type="danger"
          plain
          :loading="pushing"
          :disabled="!report || report.consistent"
          @click="handlePushAlert"
        >
          <Icon icon="ep:bell" class="mr-5px" /> 手动推送告警
        </el-button>
      </div>
    </div>

    <el-empty v-if="!report" description="点击「执行对账」获取当前库存差异" />

    <template v-else>
      <el-descriptions :column="4" border class="mb-16px">
        <el-descriptions-item label="对账结果">
          <el-tag :type="report.consistent ? 'success' : 'danger'">
            {{ report.consistent ? '一致' : '不一致' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="真值源数量">{{ report.sourceCount }}</el-descriptions-item>
        <el-descriptions-item label="投影数量">{{ report.projectionCount }}</el-descriptions-item>
        <el-descriptions-item label="差异项">{{ report.mismatches.length }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="report.mismatches" stripe show-overflow-tooltip>
        <el-table-column label="来源" prop="source" min-width="120" />
        <el-table-column label="物料" prop="itemId" min-width="110" />
        <el-table-column label="仓库" prop="warehouseId" min-width="110" />
        <el-table-column label="库位" prop="locationId" min-width="100" />
        <el-table-column label="库区" prop="areaId" min-width="100" />
        <el-table-column label="批次" prop="batchId" min-width="100" />
        <el-table-column label="真值数量" prop="truthQuantity" min-width="110" />
        <el-table-column label="投影数量" prop="projectionQuantity" min-width="110" />
        <el-table-column label="说明" prop="description" min-width="180" />
      </el-table>
    </template>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  InventoryReconcileApi,
  InventoryReconcileAlertResult,
  InventoryReconcileReport
} from '@/api/erp/stock/reconcile'

defineOptions({ name: 'ErpStockReconcile' })

const message = useMessage()
const running = ref(false)
const pushing = ref(false)
const report = ref<InventoryReconcileReport>()

const handleRun = async () => {
  running.value = true
  try {
    report.value = await InventoryReconcileApi.run()
    if (report.value.consistent) {
      message.success('库存对账一致')
    } else {
      message.warning(`发现 ${report.value.mismatches.length} 条库存差异`)
    }
  } finally {
    running.value = false
  }
}

const handlePushAlert = async () => {
  if (!report.value || report.value.consistent) return
  await message.confirm('确认将当前库存差异手动推送到配置的 webhook？', '手动推送告警')
  pushing.value = true
  try {
    const result: InventoryReconcileAlertResult = await InventoryReconcileApi.pushAlert()
    message.success(result.message || '告警已手动推送')
  } finally {
    pushing.value = false
  }
}
</script>
