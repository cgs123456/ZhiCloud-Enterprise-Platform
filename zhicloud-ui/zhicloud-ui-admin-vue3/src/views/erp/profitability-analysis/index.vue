<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="profitCenterId" prop="profitCenterId">
        <el-input-number v-model="queryParams.profitCenterId" placeholder="请输入profitCenterId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="periodId" prop="periodId">
        <el-input-number v-model="queryParams.periodId" placeholder="请输入periodId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="revenue" prop="revenue">
        <el-input-number v-model="queryParams.revenue" placeholder="请输入revenue" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:profitability-analysis:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="profitCenterId" align="center" prop="profitCenterId" />
      <el-table-column label="periodId" align="center" prop="periodId" />
      <el-table-column label="revenue" align="center" prop="revenue" />
      <el-table-column label="cost" align="center" prop="cost" />
      <el-table-column label="profit" align="center" prop="profit" />
      <el-table-column label="profitMargin" align="center" prop="profitMargin" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:profitability-analysis:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:profitability-analysis:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <ProfitabilityAnalysisForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ProfitabilityAnalysisApi } from '@/api/erp/profitability-analysis'
import ProfitabilityAnalysisForm from './profitabilityAnalysisForm.vue'

defineOptions({ name: 'ErpProfitabilityAnalysis' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  profitCenterId: undefined,
  periodId: undefined,
  revenue: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ProfitabilityAnalysisApi.getProfitabilityAnalysisPage(queryParams)
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

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ProfitabilityAnalysisApi.deleteProfitabilityAnalysis(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
