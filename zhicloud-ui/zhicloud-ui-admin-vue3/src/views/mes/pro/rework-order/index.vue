<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="编码" prop="code">
        <el-input v-model="queryParams.code" placeholder="请输入编码" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="原工单" prop="originalWorkOrderId">
        <el-input-number v-model="queryParams.originalWorkOrderId" placeholder="请输入原工单" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:pro:rework-order:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编码" align="center" prop="code" />
      <el-table-column label="原工单" align="center" prop="originalWorkOrderId" />
      <el-table-column label="原工单编号" align="center" prop="originalWorkOrderCode" />
      <el-table-column label="产品" align="center" prop="productId" />
      <el-table-column label="产品编码" align="center" prop="productCode" />
      <el-table-column label="产品名称" align="center" prop="productName" />
      <el-table-column label="返工数量" align="center" prop="reworkQuantity" />
      <el-table-column label="返工原因" align="center" prop="reworkReason" />
      <el-table-column label="返工类型" align="center" prop="reworkType" />
      <el-table-column label="返工工序" align="center" prop="reworkProcessId" />
      <el-table-column label="返工工序名称" align="center" prop="reworkProcessName" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="负责人" align="center" prop="responsiblePersonId" />
      <el-table-column label="负责部门" align="center" prop="responsibleDeptId" />
      <el-table-column label="计划开始" align="center" prop="plannedStartTime" />
      <el-table-column label="计划结束" align="center" prop="plannedEndTime" />
      <el-table-column label="实际开始" align="center" prop="actualStartTime" />
      <el-table-column label="实际结束" align="center" prop="actualEndTime" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['mes:pro:rework-order:update']">修改</el-button>
          <el-button link type="success" @click="handleStart(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['mes:pro:rework-order:update']">开始返工</el-button>
          <el-button link type="success" @click="handleComplete(scope.row)" v-if="scope.row.status === 20" v-hasPermi="['mes:pro:rework-order:update']">完工</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-if="scope.row.status === 10" v-hasPermi="['mes:pro:rework-order:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <ReworkOrderForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ReworkOrderApi } from '@/api/mes/pro/rework-order'
import ReworkOrderForm from './reworkOrderForm.vue'

defineOptions({ name: 'MesReworkOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined,
  originalWorkOrderId: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ReworkOrderApi.getReworkOrderPage(queryParams)
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
    await ReworkOrderApi.deleteReworkOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 开始返工（status 10 -> 20） */
const handleStart = async (row: any) => {
  try {
    await message.confirm(`确认开始返工「${row.code}」？`)
    await ReworkOrderApi.ReworkOrderStart(row.id)
    message.success('已开始返工')
    await getList()
  } catch {}
}

/** 返工完工（status 20 -> 30） */
const handleComplete = async (row: any) => {
  try {
    await message.confirm(`确认返工单「${row.code}」完工？`)
    await ReworkOrderApi.ReworkOrderComplete(row.id)
    message.success('已完工')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
