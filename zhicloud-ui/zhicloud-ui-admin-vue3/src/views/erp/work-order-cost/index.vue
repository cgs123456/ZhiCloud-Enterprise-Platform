<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="workOrderId" prop="workOrderId">
        <el-input-number v-model="queryParams.workOrderId" placeholder="请输入workOrderId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="workOrderCode" prop="workOrderCode">
        <el-input v-model="queryParams.workOrderCode" placeholder="请输入workOrderCode" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="queryParams.productId" placeholder="请输入productId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:work-order-cost:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="workOrderId" align="center" prop="workOrderId" />
      <el-table-column label="workOrderCode" align="center" prop="workOrderCode" />
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="costPeriod" align="center" prop="costPeriod" />
      <el-table-column label="materialCost" align="center" prop="materialCost" />
      <el-table-column label="laborCost" align="center" prop="laborCost" />
      <el-table-column label="overheadCost" align="center" prop="overheadCost" />
      <el-table-column label="outsourcingCost" align="center" prop="outsourcingCost" />
      <el-table-column label="totalCost" align="center" prop="totalCost" />
      <el-table-column label="quantity" align="center" prop="quantity" />
      <el-table-column label="unitCost" align="center" prop="unitCost" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:work-order-cost:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:work-order-cost:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <WorkOrderCostForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { WorkOrderCostApi } from '@/api/erp/work-order-cost'
import WorkOrderCostForm from './workOrderCostForm.vue'

defineOptions({ name: 'ErpWorkOrderCost' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderId: undefined,
  workOrderCode: undefined,
  productId: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await WorkOrderCostApi.getWorkOrderCostPage(queryParams)
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
    await WorkOrderCostApi.deleteWorkOrderCost(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
