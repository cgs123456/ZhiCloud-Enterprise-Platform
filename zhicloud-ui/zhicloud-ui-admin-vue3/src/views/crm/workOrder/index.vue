<!-- CRM 售后工单（状态机 10 待分配 →20 已分配 →30 处理中 →40 已解决 →50 已关闭） -->
<template>
  <ContentWrap>
    <doc-alert title="【售后】工单管理" url="https://doc.zhicloud.cn/crm/work-order/" />
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="工单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入工单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-input-number v-model="queryParams.customerId" class="!w-240px" :controls="false" clearable placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="类型" prop="workOrderType">
        <el-select v-model="queryParams.workOrderType" class="!w-240px" clearable placeholder="请选择类型">
          <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-select v-model="queryParams.priority" class="!w-240px" clearable placeholder="请选择优先级">
          <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          v-hasPermi="['crm:work-order:create']"
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
      <el-table-column align="center" label="工单号" prop="no" min-width="150" show-overflow-tooltip />
      <el-table-column align="left" label="标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="客户" prop="customerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="类型" prop="workOrderType" min-width="90">
        <template #default="scope">{{ typeText(scope.row.workOrderType) }}</template>
      </el-table-column>
      <el-table-column align="center" label="优先级" prop="priority" min-width="90">
        <template #default="scope">
          <el-tag :type="priorityTagType(scope.row.priority)" size="small">
            {{ priorityText(scope.row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="处理人" prop="assigneeUserName" min-width="100" />
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="解决时间" prop="resolveTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="300">
        <template #default="scope">
          <!-- 待分配：可分配 -->
          <el-button
            v-if="scope.row.status === STATUS.UNASSIGNED"
            v-hasPermi="['crm:work-order:update']"
            link
            type="primary"
            @click="openAssign(scope.row)"
          >
            分配
          </el-button>
          <!-- 已分配/处理中：可解决（需输入解决方案） -->
          <el-button
            v-if="scope.row.status === STATUS.ASSIGNED || scope.row.status === STATUS.PROCESSING"
            v-hasPermi="['crm:work-order:update']"
            link
            type="primary"
            @click="openResolve(scope.row)"
          >
            解决
          </el-button>
          <!-- 已解决：可关闭 -->
          <el-button
            v-if="scope.row.status === STATUS.RESOLVED"
            v-hasPermi="['crm:work-order:update']"
            link
            type="success"
            @click="handleClose(scope.row)"
          >
            关闭
          </el-button>
          <el-button
            v-hasPermi="['crm:work-order:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['crm:work-order:delete']"
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
  <CrmWorkOrderForm ref="formRef" @success="getList" />

  <!-- 分配弹窗 -->
  <Dialog v-model="assignVisible" title="分配工单" width="440">
    <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
      <el-form-item label="工单">
        <span>{{ currentRow?.no }} - {{ currentRow?.title }}</span>
      </el-form-item>
      <el-form-item label="处理人" prop="assigneeUserId">
        <el-input-number
          v-model="assignForm.assigneeUserId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入处理人用户编号"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="assignLoading" type="primary" @click="submitAssign">分 配</el-button>
      <el-button @click="assignVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 解决弹窗（解决方案必填） -->
  <Dialog v-model="resolveVisible" title="解决工单" width="520">
    <el-form ref="resolveFormRef" :model="resolveForm" :rules="resolveRules" label-width="100px">
      <el-form-item label="工单">
        <span>{{ currentRow?.no }} - {{ currentRow?.title }}</span>
      </el-form-item>
      <el-form-item label="解决方案" prop="resolution">
        <el-input
          v-model="resolveForm.resolution"
          type="textarea"
          :rows="4"
          maxlength="2000"
          placeholder="请输入解决方案"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="resolveLoading" type="primary" @click="submitResolve">提 交</el-button>
      <el-button @click="resolveVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CrmWorkOrderApi, CrmWorkOrderStatus } from '@/api/crm/workOrder'
import CrmWorkOrderForm from './CrmWorkOrderForm.vue'

defineOptions({ name: 'CrmWorkOrder' })

const message = useMessage()

const STATUS = CrmWorkOrderStatus

const STATUS_OPTIONS = [
  { value: CrmWorkOrderStatus.UNASSIGNED, label: '待分配' },
  { value: CrmWorkOrderStatus.ASSIGNED, label: '已分配' },
  { value: CrmWorkOrderStatus.PROCESSING, label: '处理中' },
  { value: CrmWorkOrderStatus.RESOLVED, label: '已解决' },
  { value: CrmWorkOrderStatus.CLOSED, label: '已关闭' }
]

const TYPE_OPTIONS = [
  { value: 10, label: '安装' },
  { value: 20, label: '维修' },
  { value: 30, label: '投诉' },
  { value: 40, label: '咨询' },
  { value: 50, label: '退换货' }
]

const PRIORITY_OPTIONS = [
  { value: 10, label: '低' },
  { value: 20, label: '中' },
  { value: 30, label: '高' },
  { value: 40, label: '紧急' }
]

const typeText = (v?: number) => TYPE_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const priorityText = (v?: number) => PRIORITY_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const priorityTagType = (v?: number) => {
  if (v === 40) return 'danger'
  if (v === 30) return 'warning'
  return 'info'
}
const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === CrmWorkOrderStatus.CLOSED) return 'success'
  if (v === CrmWorkOrderStatus.RESOLVED) return 'primary'
  if (v === CrmWorkOrderStatus.PROCESSING) return 'warning'
  return 'info'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  title: undefined,
  customerId: undefined,
  workOrderType: undefined,
  priority: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CrmWorkOrderApi.getCrmWorkOrderPage(queryParams)
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
    await CrmWorkOrderApi.deleteCrmWorkOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 分配（仅待分配） */
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignFormRef = ref()
const assignForm = ref<any>({ assigneeUserId: undefined })
const assignRules = reactive({
  assigneeUserId: [{ required: true, message: '处理人不能为空', trigger: 'blur' }]
})

const openAssign = (row: any) => {
  currentRow.value = row
  assignForm.value = { assigneeUserId: undefined }
  assignVisible.value = true
}

const submitAssign = async () => {
  await assignFormRef.value.validate()
  assignLoading.value = true
  try {
    await CrmWorkOrderApi.assignCrmWorkOrder({
      id: currentRow.value.id,
      assigneeUserId: assignForm.value.assigneeUserId
    })
    message.success('已分配')
    assignVisible.value = false
    await getList()
  } finally {
    assignLoading.value = false
  }
}

/** 解决（仅已分配/处理中；需解决方案） */
const resolveVisible = ref(false)
const resolveLoading = ref(false)
const resolveFormRef = ref()
const resolveForm = ref<any>({ resolution: undefined })
const resolveRules = reactive({
  resolution: [{ required: true, message: '解决方案不能为空', trigger: 'blur' }]
})

const openResolve = (row: any) => {
  currentRow.value = row
  resolveForm.value = { resolution: undefined }
  resolveVisible.value = true
}

const submitResolve = async () => {
  await resolveFormRef.value.validate()
  resolveLoading.value = true
  try {
    await CrmWorkOrderApi.resolveCrmWorkOrder({
      id: currentRow.value.id,
      resolution: resolveForm.value.resolution
    })
    message.success('已解决')
    resolveVisible.value = false
    await getList()
  } finally {
    resolveLoading.value = false
  }
}

/** 关闭（仅已解决） */
const handleClose = async (row: any) => {
  await message.confirm(`是否确认关闭工单「${row.no}」？`)
  try {
    await CrmWorkOrderApi.closeCrmWorkOrder(row.id)
    message.success('已关闭')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
