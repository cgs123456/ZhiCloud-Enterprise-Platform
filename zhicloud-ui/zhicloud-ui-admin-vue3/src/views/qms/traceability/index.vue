<!-- QMS 质量追溯（正向：原料→成品 / 反向：成品→原料，只读） -->
<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-12px" label-width="90px">
      <el-form-item label="追溯方向">
        <el-radio-group v-model="direction">
          <el-radio value="forward">正向（原料 → 成品）</el-radio>
          <el-radio value="backward">反向（成品 → 原料）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="批次号">
        <el-input v-model="batchNo" class="!w-240px" clearable placeholder="请输入批次号" @keyup.enter="trace" />
      </el-form-item>
      <el-form-item :label="direction === 'forward' ? '物料 ID' : '产品 ID'">
        <el-input-number v-model="refId" class="!w-200px" :controls="false" placeholder="可选" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="trace">
          <Icon class="mr-5px" icon="ep:search" />
          追溯
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="result" v-loading="loading" title="追溯结果">
    <el-descriptions :column="3" border>
      <el-descriptions-item label="追溯方向">{{ result.direction }}</el-descriptions-item>
      <el-descriptions-item label="起始批次">{{ result.startBatchNo }}</el-descriptions-item>
      <el-descriptions-item label="起始物料 ID">{{ result.startMaterialId }}</el-descriptions-item>
    </el-descriptions>
    <pre class="mt-12px">{{ JSON.stringify(result, null, 2) }}</pre>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { QualityTraceabilityApi } from '@/api/qms/traceability'

defineOptions({ name: 'QualityTraceability' })

const loading = ref(false)
const direction = ref('forward')
const batchNo = ref('')
const refId = ref<number | undefined>(undefined)
const result = ref<any>(null)

const trace = async () => {
  if (!batchNo.value) return
  loading.value = true
  try {
    if (direction.value === 'forward') {
      result.value = await QualityTraceabilityApi.forward({
        materialId: refId.value,
        batchNo: batchNo.value
      })
    } else {
      result.value = await QualityTraceabilityApi.backward({
        productId: refId.value,
        batchNo: batchNo.value
      })
    }
  } finally {
    loading.value = false
  }
}
</script>
