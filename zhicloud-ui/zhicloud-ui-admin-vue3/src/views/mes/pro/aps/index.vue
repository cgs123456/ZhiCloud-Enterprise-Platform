<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" placeholder="请输入计划编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="生产工单" prop="workOrderId">
        <el-input v-model="queryParams.workOrderId" placeholder="请输入生产工单" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="计划编号" align="center" prop="planNo" />
      <el-table-column label="生产工单" align="center" prop="workOrderId" />
      <el-table-column label="产品" align="center" prop="productId" />
      <el-table-column label="工作站" align="center" prop="workstationId" />
      <el-table-column label="计划开始" align="center" prop="plannedStartTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="计划结束" align="center" prop="plannedEndTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="数量" align="center" prop="quantity" />
      <el-table-column label="优先级" align="center" prop="priority" :formatter="priorityFormatter" />
      <el-table-column label="状态" align="center" prop="status" :formatter="statusFormatter" />
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="scope">
          <!-- 0 草稿：确认（确认后方可重排产） -->
          <el-button
            v-if="scope.row.status === ApsPlanStatus.DRAFT"
            v-hasPermi="['mes:pro-aps:update']"
            link
            type="primary"
            @click="handleConfirm(scope.row)"
          >
            确认
          </el-button>
          <!-- 1 已确认：重排产（后端要求已确认态） -->
          <el-button
            v-if="scope.row.status === ApsPlanStatus.CONFIRMED"
            v-hasPermi="['mes:pro-aps:update']"
            link
            type="primary"
            @click="openReschedule(scope.row)"
          >
            重排产
          </el-button>
          <!-- 0/1：取消（已取消/已完成不可再取消） -->
          <el-button
            v-if="scope.row.status === ApsPlanStatus.DRAFT || scope.row.status === ApsPlanStatus.CONFIRMED"
            v-hasPermi="['mes:pro-aps:update']"
            link
            type="danger"
            @click="handleCancel(scope.row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <!-- 重排产弹窗（仅已确认计划；新开始时间须早于计划结束时间，重叠冲突由后端校验） -->
  <Dialog v-model="rescheduleVisible" title="重排产" width="460">
    <el-form label-width="100px">
      <el-form-item label="计划编号">
        <span>{{ currentRow?.planNo }}</span>
      </el-form-item>
      <el-form-item label="当前时段">
        <span>
          {{ formatDateTime(currentRow?.plannedStartTime) }} ~
          {{ formatDateTime(currentRow?.plannedEndTime) }}
        </span>
      </el-form-item>
      <el-form-item label="新开始时间" required>
        <el-date-picker
          v-model="rescheduleForm.newStartTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择新的开始时间"
          class="!w-full"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button
        type="primary"
        :disabled="!rescheduleForm.newStartTime"
        :loading="rescheduleLoading"
        @click="submitReschedule"
      >
        确 定
      </el-button>
      <el-button @click="rescheduleVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ApsApi } from '@/api/mes/pro/aps'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'MesAps' })

// 排产计划状态（对齐 MesProApsPlanStatusEnum）
const ApsPlanStatus = {
  DRAFT: 0,
  CONFIRMED: 1,
  EXECUTING: 2,
  COMPLETED: 3,
  CANCELLED: 4
}
const APS_PLAN_STATUS_LABEL: Record<number, string> = {
  0: '草稿',
  1: '已确认',
  2: '执行中',
  3: '已完成',
  4: '已取消'
}
// 优先级（对齐 MesProApsPlanPriorityEnum）
const APS_PRIORITY_LABEL: Record<number, string> = { 1: '高', 2: '中', 3: '低' }

const loading = ref(false)
const list = ref<any[]>([])
const queryParams = reactive({
  planNo: undefined,
  workOrderId: undefined
})
const message = useMessage()

const statusFormatter = (_row: any, _column: any, cellValue: any) =>
  APS_PLAN_STATUS_LABEL[cellValue] ?? cellValue
const priorityFormatter = (_row: any, _column: any, cellValue: any) =>
  APS_PRIORITY_LABEL[cellValue] ?? cellValue
const formatDateTime = (value?: string | number) => (value ? dateFormatter({}, {} as any, value) : '')

const getList = async () => {
  loading.value = true
  try {
    const data = await ApsApi.ApsPlanList(queryParams)
    list.value = data.list || data
  } finally {
    loading.value = false
  }
}

/** 确认排产（0 草稿 → 1 已确认；确认后方可重排产） */
const handleConfirm = async (row: any) => {
  try {
    await message.confirm(`是否确认排产计划「${row.planNo}」？确认后可进行重排产。`)
    await ApsApi.ApsPlanConfirm(row.id)
    message.success('排产计划已确认')
    await getList()
  } catch {}
}

/** 取消排产（0/1 → 4；取消后原工单可重新排产） */
const handleCancel = async (row: any) => {
  try {
    await message.confirm(`是否取消排产计划「${row.planNo}」？取消后原工单可重新排产。`)
    await ApsApi.ApsPlanCancel(row.id)
    message.success('排产计划已取消')
    await getList()
  } catch {}
}

/** 重排产（仅已确认计划；起止时间由后端同步平移并校验重叠冲突） */
const rescheduleVisible = ref(false)
const rescheduleLoading = ref(false)
const currentRow = ref<any>({})
const rescheduleForm = ref<{ newStartTime?: string }>({})

const openReschedule = (row: any) => {
  currentRow.value = row
  rescheduleForm.value = { newStartTime: undefined }
  rescheduleVisible.value = true
}

const submitReschedule = async () => {
  if (!rescheduleForm.value.newStartTime) {
    return
  }
  rescheduleLoading.value = true
  try {
    await ApsApi.ApsPlanReschedule({
      planId: currentRow.value.id,
      newStartTime: rescheduleForm.value.newStartTime
    })
    message.success('重排产成功')
    rescheduleVisible.value = false
    await getList()
  } finally {
    rescheduleLoading.value = false
  }
}

onMounted(() => getList())
</script>
