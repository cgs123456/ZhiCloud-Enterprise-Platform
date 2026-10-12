<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="设备" prop="machineryId">
        <el-input v-model="queryParams.machineryId" placeholder="请输入设备" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="记录日期" prop="recordDate">
        <el-input v-model="queryParams.recordDate" placeholder="请输入记录日期" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="设备" align="center" prop="machineryId" />
      <el-table-column label="记录日期" align="center" prop="recordDate" />
      <el-table-column label="计划开机时间" align="center" prop="plannedProductionTime" />
      <el-table-column label="运行时间" align="center" prop="runTime" />
      <el-table-column label="理想节拍" align="center" prop="idealCycleTime" />
      <el-table-column label="总产量" align="center" prop="totalProduced" />
      <el-table-column label="良品数" align="center" prop="goodProduced" />
      <el-table-column label="时间开动率" align="center" prop="availability" />
      <el-table-column label="性能开动率" align="center" prop="performance" />
      <el-table-column label="合格品率" align="center" prop="quality" />
      <el-table-column label="设备综合效率(OEE)" align="center" prop="oee" />
      <el-table-column label="时间利用率" align="center" prop="timeUtilizationRate" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { OeeApi } from '@/api/mes/dv/oee'

defineOptions({ name: 'MesOee' })

const loading = ref(false)
const list = ref<any[]>([])
const queryParams = reactive({
  machineryId: undefined,
  recordDate: undefined,
})

const getList = async () => {
  loading.value = true
  try {
    const data = await OeeApi.OeeRecordList(queryParams)
    list.value = data.list || data
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
