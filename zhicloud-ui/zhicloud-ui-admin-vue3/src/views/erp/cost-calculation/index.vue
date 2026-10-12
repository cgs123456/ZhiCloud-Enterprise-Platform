<template>
  <ContentWrap>
    <!-- 成本计算：标准成本计算 / 实际成本归集 / 成本差异分析 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="产品" prop="productId">
        <el-input-number v-model="queryParams.productId" :controls="false" placeholder="产品 ID" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="成本期间" prop="costPeriod">
        <el-input v-model="queryParams.costPeriod" placeholder="如 2026-09" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="run('calculate')" :loading="loading">计算标准成本</el-button>
        <el-button type="success" @click="run('collect')" :loading="loading">归集实际成本</el-button>
        <el-button type="warning" @click="run('analyze')" :loading="loading">分析差异</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-result v-if="result !== null" icon="success" :title="`操作完成，影响 ${result} 条记录`" />
    <el-empty v-else description="请选择产品与成本期间后执行操作" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { CostCalculationApi } from '@/api/erp/cost-calculation'

defineOptions({ name: 'ErpCostCalculation' })

const loading = ref(false)
const result = ref<number | null>(null)
const queryParams = reactive({
  productId: undefined as any,
  costPeriod: undefined as any
})

const run = async (action: 'calculate' | 'collect' | 'analyze') => {
  if (!queryParams.productId || !queryParams.costPeriod) {
    message.warning('请输入产品与成本期间')
    return
  }
  loading.value = true
  try {
    let res: any
    if (action === 'calculate') res = await CostCalculationApi.CostCalculationCalculateStandardCost(queryParams)
    else if (action === 'collect') res = await CostCalculationApi.CostCalculationCollectActualCost(queryParams)
    else res = await CostCalculationApi.CostCalculationAnalyzeVariance(queryParams)
    // 三个端点分别返回 List<BigDecimal> / Integer / Integer，统一取数量
    result.value = Array.isArray(res) ? res.length : Number(res ?? 0)
  } finally {
    loading.value = false
  }
}
</script>
