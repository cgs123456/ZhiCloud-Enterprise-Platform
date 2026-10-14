<!-- TMS 运费对账（0 待对账 →10 已对账/20 有差异 →30 已确认；40 已驳回） -->
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
      <el-form-item label="对账单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入对账单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number v-model="queryParams.carrierId" class="!w-240px" :controls="false" clearable placeholder="请输入承运商编号" />
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
          v-hasPermi="['tms:freight-reconciliation:create']"
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
      <el-table-column align="center" label="对账单号" prop="no" min-width="140" show-overflow-tooltip />
      <el-table-column align="center" label="承运商编号" prop="carrierId" min-width="100" />
      <el-table-column align="center" label="周期起" prop="periodStart" width="120" />
      <el-table-column align="center" label="周期止" prop="periodEnd" width="120" />
      <el-table-column align="right" label="系统金额" prop="systemAmount" min-width="110" />
      <el-table-column align="right" label="承运商金额" prop="carrierAmount" min-width="110" />
      <el-table-column align="right" label="差异" prop="diffAmount" min-width="100">
        <template #default="scope">
          <span :class="scope.row.diffAmount && scope.row.diffAmount !== 0 ? 'text-red-500' : ''">
            {{ scope.row.diffAmount ?? 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="对账时间" prop="reconcileTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" label="确认时间" prop="confirmTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="300">
        <template #default="scope">
          <!-- 执行对账：任何状态都允许（后端无状态门槛），重算差异 -->
          <el-button
            v-hasPermi="['tms:freight-reconciliation:update']"
            link
            type="primary"
            @click="handleReconcile(scope.row)"
          >
            执行对账
          </el-button>
          <!-- 确认：待对账(0) 时禁用（后端会拒：尚未执行对账） -->
          <el-button
            v-if="scope.row.status !== STATUS.PENDING"
            v-hasPermi="['tms:freight-reconciliation:update']"
            link
            type="success"
            @click="handleConfirm(scope.row)"
          >
            确认
          </el-button>
          <el-button
            v-hasPermi="['tms:freight-reconciliation:update']"
            link
            type="danger"
            @click="openReject(scope.row)"
          >
            驳回
          </el-button>
          <el-button
            v-hasPermi="['tms:freight-reconciliation:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:freight-reconciliation:delete']"
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
  <TmsFreightReconciliationForm ref="formRef" @success="getList" />

  <!-- 驳回弹窗（原因会覆盖备注） -->
  <Dialog v-model="rejectVisible" title="驳回对账单" width="480">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="100px">
      <el-form-item label="对账单号">
        <span>{{ currentRow?.no }}</span>
      </el-form-item>
      <el-form-item label="驳回原因" prop="reason">
        <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因（将覆盖原备注）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="rejectLoading" type="primary" @click="submitReject">提 交</el-button>
      <el-button @click="rejectVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsFreightReconciliationApi, TmsFreightReconciliationStatus } from '@/api/tms/freight-reconciliation'
import TmsFreightReconciliationForm from './TmsFreightReconciliationForm.vue'

defineOptions({ name: 'TmsFreightReconciliation' })

const message = useMessage()

const STATUS = TmsFreightReconciliationStatus

const STATUS_OPTIONS = [
  { value: 0, label: '待对账' },
  { value: 10, label: '已对账' },
  { value: 20, label: '有差异' },
  { value: 30, label: '已确认' },
  { value: 40, label: '已驳回' }
]

const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === 30) return 'success'
  if (v === 40) return 'danger'
  if (v === 20) return 'warning'
  return 'info'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  carrierId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsFreightReconciliationApi.getTmsFreightReconciliationPage(queryParams)
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
    await TmsFreightReconciliationApi.deleteTmsFreightReconciliation(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const handleReconcile = async (row: any) => {
  await message.confirm(`是否对账单「${row.no}」执行对账？（将重算差异金额）`)
  try {
    await TmsFreightReconciliationApi.doReconcileTmsFreightReconciliation(row.id)
    message.success('已执行对账')
    await getList()
  } catch {}
}

const handleConfirm = async (row: any) => {
  await message.confirm(`是否确认对账单「${row.no}」？`)
  try {
    await TmsFreightReconciliationApi.confirmTmsFreightReconciliation(row.id)
    message.success('已确认')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 驳回 */
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref()
const rejectForm = ref<any>({ reason: undefined })
const rejectRules = reactive({
  reason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
})

const openReject = (row: any) => {
  currentRow.value = row
  rejectForm.value = { reason: undefined }
  rejectVisible.value = true
}

const submitReject = async () => {
  await rejectFormRef.value.validate()
  rejectLoading.value = true
  try {
    await TmsFreightReconciliationApi.rejectTmsFreightReconciliation({
      id: currentRow.value.id,
      reason: rejectForm.value.reason
    })
    message.success('已驳回')
    rejectVisible.value = false
    await getList()
  } finally {
    rejectLoading.value = false
  }
}

onMounted(() => getList())
</script>
