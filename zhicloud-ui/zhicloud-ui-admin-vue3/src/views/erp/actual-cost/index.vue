<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="queryParams.productId" placeholder="请输入productId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="productCode" prop="productCode">
        <el-input v-model="queryParams.productCode" placeholder="请输入productCode" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="costPeriod" prop="costPeriod">
        <el-input v-model="queryParams.costPeriod" placeholder="请输入costPeriod" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:actual-cost:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="productCode" align="center" prop="productCode" />
      <el-table-column label="costPeriod" align="center" prop="costPeriod" />
      <el-table-column label="costItemId" align="center" prop="costItemId" />
      <el-table-column label="actualCost" align="center" prop="actualCost" />
      <el-table-column label="actualQuantity" align="center" prop="actualQuantity" />
      <el-table-column label="unitCost" align="center" prop="unitCost" />
      <el-table-column label="varianceAmount" align="center" prop="varianceAmount" />
      <el-table-column label="varianceRate" align="center" prop="varianceRate" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:actual-cost:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:actual-cost:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <ActualCostForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ActualCostApi } from '@/api/erp/actual-cost'
import ActualCostForm from './actualCostForm.vue'

defineOptions({ name: 'ErpActualCost' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  productId: undefined
  productCode: undefined
  costPeriod: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ActualCostApi.getActualCostPage(queryParams)
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
    await ActualCostApi.deleteActualCost(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
