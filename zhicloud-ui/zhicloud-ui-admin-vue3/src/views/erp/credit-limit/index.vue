<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="customerId" prop="customerId">
        <el-input-number v-model="queryParams.customerId" placeholder="请输入customerId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="creditLimit" prop="creditLimit">
        <el-input-number v-model="queryParams.creditLimit" placeholder="请输入creditLimit" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="usedAmount" prop="usedAmount">
        <el-input-number v-model="queryParams.usedAmount" placeholder="请输入usedAmount" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:credit-limit:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="customerId" align="center" prop="customerId" />
      <el-table-column label="creditLimit" align="center" prop="creditLimit" />
      <el-table-column label="usedAmount" align="center" prop="usedAmount" />
      <el-table-column label="availableAmount" align="center" prop="availableAmount" />
      <el-table-column label="overdueAmount" align="center" prop="overdueAmount" />
      <el-table-column label="warningRatio" align="center" prop="warningRatio" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:credit-limit:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:credit-limit:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <CreditLimitForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { CreditLimitApi } from '@/api/erp/credit-limit'
import CreditLimitForm from './creditLimitForm.vue'

defineOptions({ name: 'ErpCreditLimit' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  customerId: undefined,
  creditLimit: undefined,
  usedAmount: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CreditLimitApi.getCreditLimitPage(queryParams)
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
    await CreditLimitApi.deleteCreditLimit(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
