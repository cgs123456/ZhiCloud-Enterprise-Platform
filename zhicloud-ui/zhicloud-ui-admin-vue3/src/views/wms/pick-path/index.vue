<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="orderId" prop="orderId">
        <el-input-number v-model="queryParams.orderId" placeholder="请输入orderId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="orderNo" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入orderNo" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" placeholder="请输入warehouseId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['wms:pick-path:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="orderId" align="center" prop="orderId" />
      <el-table-column label="orderNo" align="center" prop="orderNo" />
      <el-table-column label="warehouseId" align="center" prop="warehouseId" />
      <el-table-column label="warehouseName" align="center" prop="warehouseName" />
      <el-table-column label="strategy" align="center" prop="strategy" />
      <el-table-column label="locationCount" align="center" prop="locationCount" />
      <el-table-column label="totalQuantity" align="center" prop="totalQuantity" />
      <el-table-column label="estimatedDistance" align="center" prop="estimatedDistance" />
      <el-table-column label="locations" align="center" prop="locations" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['wms:pick-path:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['wms:pick-path:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <PickPathForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { PickPathApi } from '@/api/wms/pick-path'
import PickPathForm from './pickPathForm.vue'

defineOptions({ name: 'WmsPickPath' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderId: undefined
  orderNo: undefined
  warehouseId: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await PickPathApi.getPickPathPage(queryParams)
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
    await PickPathApi.deletePickPath(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
