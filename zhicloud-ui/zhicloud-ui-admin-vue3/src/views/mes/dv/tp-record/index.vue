<template>
  <ContentWrap>
    <!-- 点检保养记录（只读）：记录来自 tp-plan 执行后回写 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="计划" prop="planId">
        <el-input-number v-model="queryParams.planId" :controls="false" placeholder="计划 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="设备" prop="equipmentId">
        <el-input-number v-model="queryParams.equipmentId" :controls="false" placeholder="设备 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="计划" align="center" prop="planId" />
      <el-table-column label="设备" align="center" prop="equipmentId" />
      <el-table-column label="执行日期" align="center" prop="executeDate" />
      <el-table-column label="执行人" align="center" prop="executorId" />
      <el-table-column label="结果" align="center" prop="result" />
      <el-table-column label="发现问题" align="center" prop="issuesFound" min-width="140" show-overflow-tooltip />
      <el-table-column label="采取措施" align="center" prop="actionTaken" min-width="140" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { TpRecordApi } from '@/api/mes/dv/tp-record'

defineOptions({ name: 'MesTpRecord' })

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planId: undefined as any,
  equipmentId: undefined as any
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TpRecordApi.getTpRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

onMounted(() => getList())
</script>
