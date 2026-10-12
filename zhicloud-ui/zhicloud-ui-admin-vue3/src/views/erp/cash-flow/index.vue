<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="bizType" prop="bizType">
        <el-input-number v-model="queryParams.bizType" placeholder="请输入bizType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="queryParams.amount" placeholder="请输入金额" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="银行账户" prop="bankAccountId">
        <el-input-number v-model="queryParams.bankAccountId" placeholder="请输入银行账户" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:cash-flow:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="bizType" align="center" prop="bizType" />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="银行账户" align="center" prop="bankAccountId" />
      <el-table-column label="bizOrderId" align="center" prop="bizOrderId" />
      <el-table-column label="bizOrderType" align="center" prop="bizOrderType" />
      <el-table-column label="occurDate" align="center" prop="occurDate" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:cash-flow:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:cash-flow:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <CashFlowForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { CashFlowApi } from '@/api/erp/cash-flow'
import CashFlowForm from './cashFlowForm.vue'

defineOptions({ name: 'ErpCashFlow' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined,
  amount: undefined,
  bankAccountId: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CashFlowApi.getCashFlowPage(queryParams)
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
    await CashFlowApi.deleteCashFlow(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
