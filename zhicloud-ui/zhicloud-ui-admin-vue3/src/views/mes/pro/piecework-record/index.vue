<!-- MES 计件记录（只读列表 + 汇总） -->
<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="工单 ID" prop="workOrderId">
        <el-input-number v-model="queryParams.workOrderId" class="!w-200px" :controls="false" clearable placeholder="工单 ID" />
      </el-form-item>
      <el-form-item label="期间" prop="periodMonth">
        <el-input v-model="queryParams.periodMonth" class="!w-160px" clearable placeholder="如 2026-10" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="报工人" prop="feedbackUserName" min-width="120" />
      <el-table-column align="center" label="工单 ID" prop="workOrderId" min-width="100" />
      <el-table-column align="center" label="工序 ID" prop="processId" min-width="100" />
      <el-table-column align="center" label="产品 ID" prop="itemId" min-width="100" />
      <el-table-column align="center" label="合格数" prop="qualifiedQty" min-width="100" />
      <el-table-column align="center" label="报废数" prop="scrapQty" min-width="100" />
      <el-table-column align="center" label="工废数" prop="laborScrapQty" min-width="100" />
      <el-table-column align="center" label="单价" prop="unitPrice" min-width="100" />
      <el-table-column align="center" label="金额" prop="totalAmount" min-width="110" />
      <el-table-column align="center" label="期间" prop="periodMonth" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="90" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MesProPieceworkRecordApi } from '@/api/mes/pro-piecework-record'

defineOptions({ name: 'MesProPieceworkRecord' })

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderId: undefined,
  periodMonth: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MesProPieceworkRecordApi.getRecordPage(queryParams)
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

const resetQuery = async () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

onMounted(() => getList())
</script>
