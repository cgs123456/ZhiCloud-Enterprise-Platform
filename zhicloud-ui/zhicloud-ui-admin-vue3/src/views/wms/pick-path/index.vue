<template>
  <ContentWrap>
    <!-- 拣货路径：按出库单/波次触发路径优化，展示优化结果 -->
    <el-form class="-mb-15px" :model="form" :inline="true" label-width="100px">
      <el-form-item label="出库单" prop="shipmentOrderId">
        <el-input-number v-model="form.shipmentOrderId" :controls="false" placeholder="出库单 ID" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="optimizeByOrder" :loading="loading">优化单出库单路径</el-button>
      </el-form-item>
    </el-form>

    <el-form class="-mb-15px" :model="form" :inline="true" label-width="100px">
      <el-form-item label="波次单" prop="waveOrderId">
        <el-input-number v-model="form.waveOrderId" :controls="false" placeholder="波次单 ID" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="success" @click="optimizeByWave" :loading="loading">优化波次路径</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="result">
    <el-descriptions :column="3" border title="路径优化结果">
      <el-descriptions-item label="出库单号">{{ result.orderNo ?? result.orderId }}</el-descriptions-item>
      <el-descriptions-item label="仓库">{{ result.warehouseName ?? result.warehouseId }}</el-descriptions-item>
      <el-descriptions-item label="策略">{{ result.strategy ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="库位数">{{ result.locationCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="总数量">{{ result.totalQuantity ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="预估距离">{{ result.estimatedDistance ?? '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap v-else>
    <el-empty description="输入出库单或波次单后执行路径优化" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { PickPathApi } from '@/api/wms/pick-path'

defineOptions({ name: 'WmsPickPath' })

const message = useMessage()

const loading = ref(false)
const result = ref<any>(null)
const form = reactive({
  shipmentOrderId: undefined as any,
  waveOrderId: undefined as any
})

const optimizeByOrder = async () => {
  if (!form.shipmentOrderId) {
    message.warning('请输入出库单 ID')
    return
  }
  loading.value = true
  try {
    result.value = await PickPathApi.optimizePickPath(form.shipmentOrderId)
  } finally {
    loading.value = false
  }
}

const optimizeByWave = async () => {
  if (!form.waveOrderId) {
    message.warning('请输入波次单 ID')
    return
  }
  loading.value = true
  try {
    result.value = await PickPathApi.optimizeBatchPickPath(form.waveOrderId)
  } finally {
    loading.value = false
  }
}
</script>
