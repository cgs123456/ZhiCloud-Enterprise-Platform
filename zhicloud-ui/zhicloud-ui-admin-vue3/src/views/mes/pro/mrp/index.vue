<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="planId" prop="planId">
        <el-input v-model="queryParams.planId" placeholder="请输入planId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="产品" prop="productId">
        <el-input v-model="queryParams.productId" placeholder="请输入产品" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="planId" align="center" prop="planId" />
      <el-table-column label="产品" align="center" prop="productId" />
      <el-table-column label="需求数量" align="center" prop="requirementQty" />
      <el-table-column label="库存数量" align="center" prop="stockQty" />
      <el-table-column label="净需求" align="center" prop="netRequirement" />
      <el-table-column label="计划订单量" align="center" prop="plannedOrderQty" />
      <el-table-column label="计划订单日期" align="center" prop="plannedOrderDate" />
      <el-table-column label="供应商" align="center" prop="supplierId" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { MrpApi } from '@/api/mes/pro/mrp'

defineOptions({ name: 'MesMrp' })

const loading = ref(false)
const list = ref<any[]>([])
const queryParams = reactive({
  planId: undefined,
  productId: undefined,
})

const getList = async () => {
  loading.value = true
  try {
    const data = await MrpApi.MrpPlanList(queryParams)
    list.value = data.list || data
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
