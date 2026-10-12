<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" placeholder="请输入计划编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="生产工单" prop="workOrderId">
        <el-input v-model="queryParams.workOrderId" placeholder="请输入生产工单" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="计划编号" align="center" prop="planNo" />
      <el-table-column label="生产工单" align="center" prop="workOrderId" />
      <el-table-column label="产品" align="center" prop="productId" />
      <el-table-column label="工作站" align="center" prop="workstationId" />
      <el-table-column label="计划开始" align="center" prop="plannedStartTime" />
      <el-table-column label="计划结束" align="center" prop="plannedEndTime" />
      <el-table-column label="数量" align="center" prop="quantity" />
      <el-table-column label="优先级" align="center" prop="priority" />
      <el-table-column label="状态" align="center" prop="status" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { ApsApi } from '@/api/mes/pro/aps'

defineOptions({ name: 'MesAps' })

const loading = ref(false)
const list = ref<any[]>([])
const queryParams = reactive({
  planNo: undefined,
  workOrderId: undefined,
})

const getList = async () => {
  loading.value = true
  try {
    const data = await ApsApi.ApsPlanList(queryParams)
    list.value = data.list || data
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
