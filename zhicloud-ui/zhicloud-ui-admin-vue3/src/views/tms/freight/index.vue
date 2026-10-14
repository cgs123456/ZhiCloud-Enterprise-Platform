<!-- TMS 运费结算（状态机：10 待审核 →20 已审核 →30 已结算；10 →40 已驳回） -->
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
      <el-form-item label="结算单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入结算单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="运单编号" prop="shipmentId">
        <el-input-number v-model="queryParams.shipmentId" class="!w-240px" :controls="false" clearable placeholder="请输入运单编号" />
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
          v-hasPermi="['tms:freight:create']"
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
      <el-table-column align="center" label="结算单号" prop="no" min-width="140" show-overflow-tooltip />
      <el-table-column align="center" label="运单编号" prop="shipmentId" min-width="100" />
      <el-table-column align="center" label="承运商编号" prop="carrierId" min-width="100" />
      <el-table-column align="center" label="计费方式" prop="billingMethod" min-width="110">
        <template #default="scope">{{ billingMethodText(scope.row.billingMethod) }}</template>
      </el-table-column>
      <el-table-column align="right" label="计费数量" prop="billingQuantity" min-width="110" />
      <el-table-column align="right" label="单价" prop="unitPrice" min-width="100" />
      <el-table-column align="right" label="附加费" prop="surcharge" min-width="100" />
      <el-table-column align="right" label="折扣" prop="discountAmount" min-width="100" />
      <el-table-column align="right" label="总金额" prop="totalAmount" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="结算时间" prop="settleTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="260">
        <template #default="scope">
          <!-- 待审核：可审核（通过/驳回）、修改、删除 -->
          <template v-if="scope.row.status === STATUS.PENDING_AUDIT">
            <el-button
              v-hasPermi="['tms:freight:audit']"
              link
              type="primary"
              @click="openAudit(scope.row)"
            >
              审核
            </el-button>
            <el-button
              v-hasPermi="['tms:freight:update']"
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
            >
              修改
            </el-button>
            <el-button
              v-hasPermi="['tms:freight:delete']"
              link
              type="danger"
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
          <!-- 已审核：可结算 -->
          <template v-else-if="scope.row.status === STATUS.AUDITED">
            <el-button
              v-hasPermi="['tms:freight:settle']"
              link
              type="success"
              @click="handleSettle(scope.row)"
            >
              结算
            </el-button>
            <el-button
              v-hasPermi="['tms:freight:query']"
              link
              type="primary"
              @click="openForm('detail', scope.row.id)"
            >
              详情
            </el-button>
          </template>
          <template v-else>
            <el-button
              v-hasPermi="['tms:freight:query']"
              link
              type="primary"
              @click="openForm('detail', scope.row.id)"
            >
              详情
            </el-button>
          </template>
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

  <!-- 表单弹窗（新增/修改/详情） -->
  <TmsFreightForm ref="formRef" @success="getList" />

  <!-- 审核弹窗 -->
  <Dialog v-model="auditVisible" title="运费结算审核" width="480">
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
      <el-form-item label="结算单号">
        <span>{{ currentRow?.no }}</span>
      </el-form-item>
      <el-form-item label="审核结果" prop="pass">
        <el-radio-group v-model="auditForm.pass">
          <el-radio :value="true">通过</el-radio>
          <el-radio :value="false">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="auditForm.pass === false" label="驳回原因" prop="reason">
        <el-input v-model="auditForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="auditLoading" type="primary" @click="submitAudit">提 交</el-button>
      <el-button @click="auditVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsFreightApi, TmsFreightStatus } from '@/api/tms/freight'
import TmsFreightForm from './TmsFreightForm.vue'

defineOptions({ name: 'TmsFreight' })

const message = useMessage()

const STATUS = TmsFreightStatus

const STATUS_OPTIONS = [
  { value: TmsFreightStatus.PENDING_AUDIT, label: '待审核' },
  { value: TmsFreightStatus.AUDITED, label: '已审核' },
  { value: TmsFreightStatus.SETTLED, label: '已结算' },
  { value: TmsFreightStatus.REJECTED, label: '已驳回' }
]

const BILLING_METHODS = [
  { value: 10, label: '按重量' },
  { value: 20, label: '按体积' },
  { value: 30, label: '按件数' },
  { value: 40, label: '整车一口价' },
  { value: 50, label: '里程计费' }
]

const billingMethodText = (v?: number) => BILLING_METHODS.find((i) => i.value === v)?.label ?? '-'
const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === TmsFreightStatus.SETTLED) return 'success'
  if (v === TmsFreightStatus.AUDITED) return 'primary'
  if (v === TmsFreightStatus.REJECTED) return 'danger'
  return 'warning'
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  shipmentId: undefined,
  carrierId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsFreightApi.getTmsFreightPage(queryParams)
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
    await TmsFreightApi.deleteTmsFreight(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 结算（要求运单已签收） */
const handleSettle = async (row: any) => {
  await message.confirm(`是否确认结算结算单「${row.no}」？（要求对应运单已签收）`)
  try {
    await TmsFreightApi.settleTmsFreight(row.id)
    message.success('已结算')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 审核 */
const auditVisible = ref(false)
const auditLoading = ref(false)
const auditFormRef = ref()
const auditForm = ref<any>({ pass: true, reason: undefined })
const auditRules = reactive({
  pass: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  reason: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (auditForm.value.pass === false && !value) {
          callback(new Error('驳回原因不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const openAudit = (row: any) => {
  currentRow.value = row
  auditForm.value = { pass: true, reason: undefined }
  auditVisible.value = true
}

const submitAudit = async () => {
  await auditFormRef.value.validate()
  auditLoading.value = true
  try {
    await TmsFreightApi.auditTmsFreight({
      id: currentRow.value.id,
      pass: auditForm.value.pass,
      reason: auditForm.value.pass === false ? auditForm.value.reason : undefined
    })
    message.success(auditForm.value.pass ? '已审核通过' : '已驳回')
    auditVisible.value = false
    await getList()
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => getList())
</script>
