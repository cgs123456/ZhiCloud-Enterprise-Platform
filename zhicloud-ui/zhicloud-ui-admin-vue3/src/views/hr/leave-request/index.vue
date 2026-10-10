<!-- HR 请假申请（含提交/审批/撤销流转） -->
<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="queryParams.employeeId" class="!w-200px" :controls="false" clearable placeholder="员工 ID" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-180px" :controls="false" clearable placeholder="状态" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button v-hasPermi="['hr:leave-request:create']" plain type="primary" @click="openForm">
          <Icon class="mr-5px" icon="ep:plus" />
          发起请假
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="员工 ID" prop="employeeId" min-width="110" />
      <el-table-column align="center" label="假期类型 ID" prop="leaveTypeId" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="开始时间" prop="startTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="结束时间" prop="endTime" width="170" />
      <el-table-column align="center" label="时长(天)" prop="duration" min-width="100" />
      <el-table-column align="center" label="事由" prop="reason" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" label="状态" prop="status" min-width="90" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="200">
        <template #default="scope">
          <el-button v-hasPermi="['hr:leave-request:update']" link type="primary" @click="handleSubmit(scope.row.id)">提交</el-button>
          <el-button v-hasPermi="['hr:leave-request:update']" link type="primary" @click="handleApprove(scope.row.id)">审批</el-button>
          <el-button v-hasPermi="['hr:leave-request:update']" link type="danger" @click="handleCancel(scope.row.id)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <HrLeaveRequestForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrLeaveRequestApi } from '@/api/hr/leave-request'
import HrLeaveRequestForm from './HrLeaveRequestForm.vue'

defineOptions({ name: 'HrLeaveRequest' })

const message = useMessage()
const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  employeeId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrLeaveRequestApi.getLeaveRequestPage(queryParams)
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

const resetQuery = async () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()
const openForm = () => formRef.value.open()

const handleSubmit = async (id: number) => {
  await HrLeaveRequestApi.submitLeaveRequest(id)
  message.success('已提交审批')
  await getList()
}

const handleApprove = async (id: number) => {
  try {
    const { value } = await message.prompt('请输入审批意见', '审批')
    await HrLeaveRequestApi.approveLeaveRequest({ id, approved: true, comment: value })
    message.success('审批完成')
    await getList()
  } catch {}
}

const handleCancel = async (id: number) => {
  await HrLeaveRequestApi.cancelLeaveRequest(id)
  message.success('已撤销')
  await getList()
}

onMounted(() => getList())
</script>
