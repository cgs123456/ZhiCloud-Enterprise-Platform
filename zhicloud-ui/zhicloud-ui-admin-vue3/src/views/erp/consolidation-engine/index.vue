<template>
  <ContentWrap>
    <!-- 合并抵消：按母公司/子公司/期间生成抵消分录 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="母公司" prop="parentId">
        <el-input-number v-model="queryParams.parentId" :controls="false" placeholder="母公司" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="子公司" prop="subId">
        <el-input-number v-model="queryParams.subId" :controls="false" placeholder="子公司" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="合并期间" prop="period">
        <el-input v-model="queryParams.period" placeholder="如 2026-09" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 生成抵消</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="rows" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="合并期间" align="center" prop="consolidationPeriod" />
      <el-table-column label="母公司" align="center" prop="parentCompanyId" />
      <el-table-column label="子公司" align="center" prop="subsidiaryCompanyId" />
      <el-table-column label="抵消类型" align="center" prop="eliminationTypeName" />
      <el-table-column label="抵消金额" align="center" prop="eliminationAmount" />
      <el-table-column label="说明" align="center" prop="description" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="statusName" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { ConsolidationEngineApi } from '@/api/erp/consolidation-engine'

defineOptions({ name: 'ErpConsolidationEngine' })

const loading = ref(false)
const rows = ref<any[]>([])
const queryParams = reactive({
  parentId: undefined as any,
  subId: undefined as any,
  period: undefined as any
})

const handleQuery = async () => {
  loading.value = true
  try {
    const res: any = await ConsolidationEngineApi.ConsolidationEngineGenerateAllEliminations(queryParams)
    rows.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}
</script>
