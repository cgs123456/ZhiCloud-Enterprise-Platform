<!-- QMS 审核计划 -->
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
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" class="!w-240px" clearable placeholder="请输入计划编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入审核标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核类型" prop="auditType">
        <el-input-number v-model="queryParams.auditType" class="!w-240px" :controls="false" clearable placeholder="请输入审核类型" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="主审 ID" prop="leadAuditorId">
        <el-input-number v-model="queryParams.leadAuditorId" class="!w-240px" :controls="false" clearable placeholder="请输入主审 ID" />
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
          v-hasPermi="['qms:audit:create']"
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
      <el-table-column align="center" label="计划编号" prop="planNo" min-width="110" />
      <el-table-column align="center" label="审核类型" prop="auditType" min-width="110" />
      <el-table-column align="center" label="审核标题" prop="title" min-width="110" />
      <el-table-column align="center" label="审核依据" prop="auditStandard" min-width="110" />
      <el-table-column align="center" label="审核范围" prop="auditScope" min-width="110" />
      <el-table-column align="center" label="审核目的" prop="auditPurpose" min-width="110" />
      <el-table-column align="center" label="主审 ID" prop="leadAuditorId" min-width="110" />
      <el-table-column align="center" label="审核开始日期" prop="auditStartDate" min-width="110" />
      <el-table-column align="center" label="审核结束日期" prop="auditEndDate" min-width="110" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_AUDIT_PLAN_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="280">
        <template #default="scope">
          <!-- 10 已计划：开始执行 -->
          <el-button
            v-if="scope.row.status === AuditPlanStatus.PLANNED"
            v-hasPermi="['qms:audit:execute']"
            link
            type="primary"
            @click="handleExecute(scope.row)"
          >
            开始执行
          </el-button>
          <!-- 20 执行中：完成 -->
          <el-button
            v-if="scope.row.status === AuditPlanStatus.EXECUTING"
            v-hasPermi="['qms:audit:execute']"
            link
            type="success"
            @click="handleComplete(scope.row)"
          >
            完成
          </el-button>
          <!-- 10/20：取消 -->
          <el-button
            v-if="
              scope.row.status === AuditPlanStatus.PLANNED ||
              scope.row.status === AuditPlanStatus.EXECUTING
            "
            v-hasPermi="['qms:audit:execute']"
            link
            type="danger"
            @click="handleCancel(scope.row)"
          >
            取消
          </el-button>
          <el-button
            v-hasPermi="['qms:audit:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:audit:delete']"
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
  <QmsAuditPlanForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { QmsAuditPlanApi } from '@/api/qms/audit/plan'
import { DICT_TYPE } from '@/utils/dict'
import QmsAuditPlanForm from './QmsAuditPlanForm.vue'

defineOptions({ name: 'QmsAuditPlan' })

const message = useMessage()

/** 审核计划状态机（与后端 QmsAuditPlanStatusEnum 对齐；§12.239 接线执行闭环） */
const AuditPlanStatus = {
  PLANNED: 10,
  EXECUTING: 20,
  COMPLETED: 30,
  CANCELLED: 40
} as const

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planNo: undefined,
  title: undefined,
  auditType: undefined,
  status: undefined,
  leadAuditorId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await QmsAuditPlanApi.getQmsAuditPlanPage(queryParams)
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
    await QmsAuditPlanApi.deleteQmsAuditPlan(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 开始执行（10 已计划 → 20 执行中） */
const handleExecute = async (row: any) => {
  try {
    await message.confirm(`是否确认开始执行审核计划「${row.planNo}」？`)
    await QmsAuditPlanApi.executeQmsAuditPlan(row.id)
    message.success('已开始执行')
    await getList()
  } catch {}
}

/** 完成（20 执行中 → 30 已完成） */
const handleComplete = async (row: any) => {
  try {
    await message.confirm(`是否确认完成审核计划「${row.planNo}」？`)
    await QmsAuditPlanApi.completeQmsAuditPlan(row.id)
    message.success('已完成')
    await getList()
  } catch {}
}

/** 取消（10/20 → 40 已取消） */
const handleCancel = async (row: any) => {
  try {
    await message.confirm(`是否确认取消审核计划「${row.planNo}」？`)
    await QmsAuditPlanApi.cancelQmsAuditPlan(row.id)
    message.success('已取消')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
