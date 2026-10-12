<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="taskNo" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入taskNo" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="shipmentOrderId" prop="shipmentOrderId">
        <el-input-number v-model="queryParams.shipmentOrderId" placeholder="请输入shipmentOrderId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="waveOrderId" prop="waveOrderId">
        <el-input-number v-model="queryParams.waveOrderId" placeholder="请输入waveOrderId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="handleGenerate" v-hasPermi="['wms:pick-task:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 按出库单生成
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="taskNo" align="center" prop="taskNo" />
      <el-table-column label="shipmentOrderId" align="center" prop="shipmentOrderId" />
      <el-table-column label="waveOrderId" align="center" prop="waveOrderId" />
      <el-table-column label="skuId" align="center" prop="skuId" />
      <el-table-column label="productName" align="center" prop="productName" />
      <el-table-column label="quantity" align="center" prop="quantity" />
      <el-table-column label="pickedQuantity" align="center" prop="pickedQuantity" />
      <el-table-column label="locationId" align="center" prop="locationId" />
      <el-table-column label="pickSequence" align="center" prop="pickSequence" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="pickerUserId" align="center" prop="pickerUserId" />
      <el-table-column label="pickTime" align="center" prop="pickTime" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="success" @click="handleConfirmPick(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['wms:pick-task:update']">确认拣货</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { PickTaskApi } from '@/api/wms/pick-task'

defineOptions({ name: 'WmsPickTask' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskNo: undefined,
  shipmentOrderId: undefined,
  waveOrderId: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await PickTaskApi.getPickTaskPage(queryParams)
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

/** 按出库单生成拣货任务 */
const handleGenerate = async () => {
  try {
    const { value } = await message.prompt('请输入出库单 ID', '生成拣货任务')
    if (!value) return
    await PickTaskApi.PickTaskGenerate(Number(value))
    message.success('已生成拣货任务')
    await getList()
  } catch {}
}

/** 确认拣货（待拣 10 -> 已拣 20） */
const handleConfirmPick = async (row: any) => {
  try {
    const { value } = await message.prompt('请输入已拣数量', `确认拣货「${row.taskNo}」`)
    if (value === null || value === '') return
    await PickTaskApi.PickTaskConfirmPick(row.id, Number(value))
    message.success('已确认拣货')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>

