<!-- HR 考勤 -->
<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="queryParams.employeeId" class="!w-240px" :controls="false" clearable placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="考勤月份" prop="month">
        <el-input v-model="queryParams.month" class="!w-240px" clearable placeholder="请输入考勤月份" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="应出勤天数" prop="totalDays">
        <el-input-number v-model="queryParams.totalDays" class="!w-240px" :controls="false" clearable placeholder="请输入应出勤天数" />
      </el-form-item>
      <el-form-item label="正常天数" prop="normalDays">
        <el-input-number v-model="queryParams.normalDays" class="!w-240px" :controls="false" clearable placeholder="请输入正常天数" />
      </el-form-item>
      <el-form-item label="迟到天数" prop="lateDays">
        <el-input-number v-model="queryParams.lateDays" class="!w-240px" :controls="false" clearable placeholder="请输入迟到天数" />
      </el-form-item>
      <el-form-item label="早退天数" prop="earlyLeaveDays">
        <el-input-number v-model="queryParams.earlyLeaveDays" class="!w-240px" :controls="false" clearable placeholder="请输入早退天数" />
      </el-form-item>
      <el-form-item label="缺勤天数" prop="absentDays">
        <el-input-number v-model="queryParams.absentDays" class="!w-240px" :controls="false" clearable placeholder="请输入缺勤天数" />
      </el-form-item>
      <el-form-item label="加班天数" prop="overtimeDays">
        <el-input-number v-model="queryParams.overtimeDays" class="!w-240px" :controls="false" clearable placeholder="请输入加班天数" />
      </el-form-item>
      <el-form-item label="加班总时长（小时）" prop="totalOvertimeHours">
        <el-input-number v-model="queryParams.totalOvertimeHours" class="!w-240px" :controls="false" clearable placeholder="请输入加班总时长（小时）" />
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
        <el-button
          v-hasPermi="['hr:attendance:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="员工 ID" prop="employeeId" min-width="120" />
      <el-table-column align="center" label="考勤月份" prop="month" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="应出勤天数" prop="totalDays" min-width="120" />
      <el-table-column align="center" label="正常天数" prop="normalDays" min-width="120" />
      <el-table-column align="center" label="迟到天数" prop="lateDays" min-width="120" />
      <el-table-column align="center" label="早退天数" prop="earlyLeaveDays" min-width="120" />
      <el-table-column align="center" label="缺勤天数" prop="absentDays" min-width="120" />
      <el-table-column align="center" label="加班天数" prop="overtimeDays" min-width="120" />
      <el-table-column align="center" label="加班总时长（小时）" prop="totalOvertimeHours" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['hr:attendance:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['hr:attendance:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗 -->
  <HrAttendanceForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { HrAttendanceApi } from '@/api/hr/attendance'
import HrAttendanceForm from './HrAttendanceForm.vue'

defineOptions({ name: 'HrAttendance' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  employeeId: undefined
  month: undefined
  totalDays: undefined
  normalDays: undefined
  lateDays: undefined
  earlyLeaveDays: undefined
  absentDays: undefined
  overtimeDays: undefined
  totalOvertimeHours: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await HrAttendanceApi.getHrAttendancePage(queryParams)
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

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await HrAttendanceApi.deleteHrAttendance(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
