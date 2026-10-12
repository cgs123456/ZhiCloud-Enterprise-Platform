<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="请输入编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="supplierId" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" placeholder="请输入supplierId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" placeholder="请输入warehouseId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['wms:asn-order:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="no" />
      <el-table-column label="supplierId" align="center" prop="supplierId" />
      <el-table-column label="warehouseId" align="center" prop="warehouseId" />
      <el-table-column label="dockId" align="center" prop="dockId" />
      <el-table-column label="expectedArrivalTime" align="center" prop="expectedArrivalTime" />
      <el-table-column label="actualArrivalTime" align="center" prop="actualArrivalTime" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="totalQuantity" align="center" prop="totalQuantity" />
      <el-table-column label="totalAmount" align="center" prop="totalAmount" />
      <el-table-column label="transportMode" align="center" prop="transportMode" />
      <el-table-column label="carrierName" align="center" prop="carrierName" />
      <el-table-column label="vehicleNo" align="center" prop="vehicleNo" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['wms:asn-order:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['wms:asn-order:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <AsnOrderForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AsnOrderApi } from '@/api/wms/asn-order'
import AsnOrderForm from './asnOrderForm.vue'

defineOptions({ name: 'WmsAsnOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined
  supplierId: undefined
  warehouseId: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AsnOrderApi.getAsnOrderPage(queryParams)
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
    await AsnOrderApi.deleteAsnOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
