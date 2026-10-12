<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="计划期间" prop="planPeriod">
        <el-input v-model="queryParams.planPeriod" placeholder="请输入计划期间" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="计划类型" prop="planType">
        <el-input-number v-model="queryParams.planType" placeholder="请输入计划类型" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="queryParams.amount" placeholder="请输入金额" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:fund-plan:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="计划期间" align="center" prop="planPeriod" />
      <el-table-column label="计划类型" align="center" prop="planType" />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="银行账户" align="center" prop="bankAccountId" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:fund-plan:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:fund-plan:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <FundPlanForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { FundPlanApi } from '@/api/erp/fund-plan'
import FundPlanForm from './fundPlanForm.vue'

defineOptions({ name: 'ErpFundPlan' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planPeriod: undefined,
  planType: undefined,
  amount: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FundPlanApi.getFundPlanPage(queryParams)
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
    await FundPlanApi.deleteFundPlan(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
