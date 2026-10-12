<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="请输入编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="planName" prop="planName">
        <el-input v-model="queryParams.planName" placeholder="请输入planName" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="planDate" prop="planDate">
        <el-input-number v-model="queryParams.planDate" placeholder="请输入planDate" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:mrp-plan:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="no" />
      <el-table-column label="planName" align="center" prop="planName" />
      <el-table-column label="planDate" align="center" prop="planDate" />
      <el-table-column label="mpsPlanId" align="center" prop="mpsPlanId" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="totalDemandCount" align="center" prop="totalDemandCount" />
      <el-table-column label="totalPurchaseCount" align="center" prop="totalPurchaseCount" />
      <el-table-column label="totalProduceCount" align="center" prop="totalProduceCount" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:mrp-plan:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:mrp-plan:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <MrpPlanForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { MrpPlanApi } from '@/api/erp/mrp-plan'
import MrpPlanForm from './mrpPlanForm.vue'

defineOptions({ name: 'ErpMrpPlan' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  planName: undefined,
  planDate: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MrpPlanApi.getMrpPlanPage(queryParams)
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
    await MrpPlanApi.deleteMrpPlan(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
