<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="supplierId" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" placeholder="请输入supplierId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" placeholder="请输入warehouseId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="queryParams.productId" placeholder="请输入productId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:vmi-inventory:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="supplierId" align="center" prop="supplierId" />
      <el-table-column label="warehouseId" align="center" prop="warehouseId" />
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="productName" align="center" prop="productName" />
      <el-table-column label="quantity" align="center" prop="quantity" />
      <el-table-column label="availableQuantity" align="center" prop="availableQuantity" />
      <el-table-column label="lockedQuantity" align="center" prop="lockedQuantity" />
      <el-table-column label="minQuantity" align="center" prop="minQuantity" />
      <el-table-column label="maxQuantity" align="center" prop="maxQuantity" />
      <el-table-column label="replenishmentPoint" align="center" prop="replenishmentPoint" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:vmi-inventory:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:vmi-inventory:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <VmiInventoryForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { VmiInventoryApi } from '@/api/erp/vmi-inventory'
import VmiInventoryForm from './vmiInventoryForm.vue'

defineOptions({ name: 'ErpVmiInventory' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierId: undefined,
  warehouseId: undefined,
  productId: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await VmiInventoryApi.getVmiInventoryPage(queryParams)
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
    await VmiInventoryApi.deleteVmiInventory(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
