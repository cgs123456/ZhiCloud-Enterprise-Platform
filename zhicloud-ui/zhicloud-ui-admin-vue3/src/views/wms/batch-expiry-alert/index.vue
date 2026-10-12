<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="alertType" prop="alertType">
        <el-input v-model="queryParams.alertType" placeholder="请输入alertType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" placeholder="请输入warehouseId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="warehouseName" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入warehouseName" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['wms:batch-expiry-alert:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="alertType" align="center" prop="alertType" />
      <el-table-column label="warehouseId" align="center" prop="warehouseId" />
      <el-table-column label="warehouseName" align="center" prop="warehouseName" />
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="skuCode" align="center" prop="skuCode" />
      <el-table-column label="skuName" align="center" prop="skuName" />
      <el-table-column label="batchNo" align="center" prop="batchNo" />
      <el-table-column label="supplierBatchNo" align="center" prop="supplierBatchNo" />
      <el-table-column label="productionDate" align="center" prop="productionDate" />
      <el-table-column label="失效日期" align="center" prop="expiryDate" />
      <el-table-column label="shelfLifeDays" align="center" prop="shelfLifeDays" />
      <el-table-column label="currentQuantity" align="center" prop="currentQuantity" />
      <el-table-column label="thresholdValue" align="center" prop="thresholdValue" />
      <el-table-column label="alertTime" align="center" prop="alertTime" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['wms:batch-expiry-alert:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['wms:batch-expiry-alert:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <BatchExpiryAlertForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { BatchExpiryAlertApi } from '@/api/wms/batch-expiry-alert'
import BatchExpiryAlertForm from './batchExpiryAlertForm.vue'

defineOptions({ name: 'WmsBatchExpiryAlert' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  alertType: undefined
  warehouseId: undefined
  warehouseName: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await BatchExpiryAlertApi.getBatchExpiryAlertPage(queryParams)
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
    await BatchExpiryAlertApi.deleteBatchExpiryAlert(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
