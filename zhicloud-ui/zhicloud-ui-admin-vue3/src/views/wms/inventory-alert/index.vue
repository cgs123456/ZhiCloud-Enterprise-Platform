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
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="queryParams.productId" placeholder="请输入productId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="alertType" align="center" prop="alertType" />
      <el-table-column label="warehouseId" align="center" prop="warehouseId" />
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="batchNo" align="center" prop="batchNo" />
      <el-table-column label="currentQuantity" align="center" prop="currentQuantity" />
      <el-table-column label="thresholdValue" align="center" prop="thresholdValue" />
      <el-table-column label="alertTime" align="center" prop="alertTime" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="240">
        <template #default="scope">

          <el-button link type="success" @click="handleConfirm(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['wms:inventory-alert:update']">确认预警</el-button>

          <el-button link type="success" @click="handleProcess(scope.row)" v-if="scope.row.status === 20" v-hasPermi="['wms:inventory-alert:update']">处理预警</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { InventoryAlertApi } from '@/api/wms/inventory-alert'

defineOptions({ name: 'WmsInventoryAlert' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  alertType: undefined,
  warehouseId: undefined,
  productId: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InventoryAlertApi.getInventoryAlertPage(queryParams)
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



onMounted(() => getList())
</script>


/** 触发域动作（后端无 CRUD，只有动作端点） */
/** 确认预警（待确认 10 -> 待处理 20） */
const handleConfirm = async (row: any) => {
  try {
    await message.confirm(`确认预警「${row.alertType}」？`)
    await InventoryAlertApi.InventoryAlertConfirm(row.id)
    message.success('已确认')
    await getList()
  } catch {}
}

/** 处理预警（待处理 20 -> 已处理 30） */
const handleProcess = async (row: any) => {
  try {
    await message.confirm(`确认已处理预警「${row.alertType}」？`)
    await InventoryAlertApi.InventoryAlertProcess(row.id)
    message.success('已处理')
    await getList()
  } catch {}
}

