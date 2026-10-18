<template>
  <doc-alert
    title="【设置】币别、科目、辅助核算、初始余额"
    url="https://doc.zhicloud.cn/fms/config/accounting/"
  />
  <ContentWrap>
    <el-form class="-mb-15px" :inline="true" label-width="68px">
      <el-form-item label="科目类别">
        <el-select v-model="subjectType" class="!w-240px" @change="changeSubjectType">
          <el-option
            v-for="item in subjectTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          v-if="fmsStore.isAccountSetWritable"
          type="primary"
          :loading="saving"
          @click="handleSave"
          v-hasPermi="['erp:gl-initial-balance:update']"
        >
          保存
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openTrialBalance"
          v-hasPermi="['erp:gl-initial-balance:query']"
        >
          试算平衡
        </el-button>
        <el-button
          v-if="fmsStore.isAccountSetWritable"
          type="warning"
          plain
          @click="openImportForm"
          v-hasPermi="['erp:gl-initial-balance:import']"
        >
          <Icon icon="ep:upload" class="mr-5px" /> 导入
        </el-button>
        <el-button
          type="success"
          plain
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['erp:gl-initial-balance:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="isJanuary"
      class="mb-16px"
      :closable="false"
      show-icon
      title="账套从一月启用，只需录入期初余额"
      type="info"
    />
    <el-alert
      v-if="edited"
      class="mb-16px"
      :closable="false"
      show-icon
      title="当前修改尚未保存，切换科目类别或离开页面前请先保存"
      type="warning"
    />

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column fixed="left" label="科目编码" min-width="140">
        <template #default="scope">
          {{ scope.row.subjectCode }}
        </template>
      </el-table-column>
      <el-table-column fixed="left" label="科目名称" min-width="240">
        <template #default="scope">
          <span :style="{ paddingLeft: `${(scope.row.level - 1) * 14}px` }">
            {{ scope.row.subjectName }}
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="left" label="方向" width="72">
        <template #default="scope">
          {{ formatDirection(scope.row.balanceDirection) }}
        </template>
      </el-table-column>

      <el-table-column align="right" label="期初余额" min-width="160">
        <template #default="scope">
          <AmountInput
            v-if="canEdit(scope.row)"
            v-model="scope.row.openingAmount"
            @change="handleAmountChange(scope.row)"
          />
          <span v-else>{{ formatAmount(scope.row.openingAmount) }}</span>
        </template>
      </el-table-column>

      <template v-if="!isJanuary">
        <el-table-column align="right" label="本年累计借方" min-width="160">
          <template #default="scope">
            <AmountInput
              v-if="canEdit(scope.row)"
              v-model="scope.row.yearDebitAmount"
              @change="handleAmountChange(scope.row)"
            />
            <span v-else>{{ formatAmount(scope.row.yearDebitAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column align="right" label="本年累计贷方" min-width="160">
          <template #default="scope">
            <AmountInput
              v-if="canEdit(scope.row)"
              v-model="scope.row.yearCreditAmount"
              @change="handleAmountChange(scope.row)"
            />
            <span v-else>{{ formatAmount(scope.row.yearCreditAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column align="right" label="年初余额" min-width="160">
          <template #default="scope">{{ formatAmount(scope.row.yearOpeningAmount) }}</template>
        </el-table-column>
        <el-table-column
          v-if="isProfitLossType"
          align="right"
          label="实际损益发生额"
          min-width="160"
        >
          <template #default="scope">
            <AmountInput
              v-if="canEdit(scope.row)"
              v-model="scope.row.profitLossAmount"
              @change="handleProfitLossAmountChange"
            />
            <span v-else>{{ formatAmount(scope.row.profitLossAmount) }}</span>
          </template>
        </el-table-column>
      </template>
    </el-table>
  </ContentWrap>

  <!-- 导入和试算平衡弹窗 -->
  <FmsInitialBalanceImportForm ref="importFormRef" @success="loadPage" />
  <FmsTrialBalanceDialog ref="trialBalanceRef" />
</template>

<script lang="ts" setup>
import { FmsAccountSetApi } from '@/api/fms/config/account-set'
import { FmsInitialBalanceApi } from '@/api/fms/config/initial-balance'
import type {
  FmsInitialBalanceUpdateVO,
  FmsInitialBalanceVO
} from '@/api/fms/config/initial-balance'
import { useFmsStore } from '@/views/fms/store/fms'
import download from '@/utils/download'
import { formatAmount } from '@/views/fms/utils/format'
import FmsInitialBalanceImportForm from './FmsInitialBalanceImportForm.vue'
import FmsTrialBalanceDialog from './FmsTrialBalanceDialog.vue'

defineOptions({ name: 'FmsInitialBalance' })

/**
 * ERP 科目类型值域（对齐 ErpGlAccountTypeEnum 10-60）
 *
 * V194 口径：科目值域的单一真相在 ERP；FMS 页面不依赖本仓未播种的 FMS_SUBJECT_TYPE 字典。
 */
const SUBJECT_TYPE = {
  ASSET: 10,
  LIABILITY: 20,
  EQUITY: 30,
  REVENUE: 40,
  EXPENSE: 50,
  COMMON: 60
} as const

/** 损益类科目类型（收入 / 费用） */
const PROFIT_LOSS_TYPES: number[] = [SUBJECT_TYPE.REVENUE, SUBJECT_TYPE.EXPENSE]

/** 余额方向值域（对齐 ErpGlAccountBalanceDirectionEnum 10/20/30） */
const BALANCE_DIRECTION = {
  DEBIT: 10,
  CREDIT: 20,
  BOTH: 30
} as const

const subjectTypeOptions = [
  { label: '资产', value: SUBJECT_TYPE.ASSET },
  { label: '负债', value: SUBJECT_TYPE.LIABILITY },
  { label: '所有者权益', value: SUBJECT_TYPE.EQUITY },
  { label: '收入', value: SUBJECT_TYPE.REVENUE },
  { label: '费用', value: SUBJECT_TYPE.EXPENSE },
  { label: '共同', value: SUBJECT_TYPE.COMMON }
]

type ViewRow = FmsInitialBalanceVO & {
  rowKey: string
  level: number
}

/** 参与父级汇总的金额字段 */
type AmountField =
  | 'openingAmount'
  | 'yearDebitAmount'
  | 'yearCreditAmount'
  | 'yearOpeningAmount'
  | 'profitLossAmount'

const AMOUNT_FIELDS: AmountField[] = [
  'openingAmount',
  'yearDebitAmount',
  'yearCreditAmount',
  'yearOpeningAmount',
  'profitLossAmount'
]

/** 汇总时不区分余额方向、直接累加的字段 */
const DIRECT_SUM_FIELDS: Set<AmountField> = new Set(['yearDebitAmount', 'yearCreditAmount'])

/** 金额输入框，隐藏控制按钮，最小值为 0 */
const AmountInput = defineComponent({
  props: {
    modelValue: { type: Number, default: 0 },
    precision: { type: Number, default: 2 }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    return () =>
      h(resolveComponent('ElInputNumber'), {
        modelValue: props.modelValue,
        'onUpdate:modelValue': (value: number | undefined) => emit('update:modelValue', value || 0),
        controls: false,
        min: 0,
        precision: props.precision,
        class: 'amount-input',
        onChange: () => emit('change')
      })
  }
})

// ==================== 页面状态 ====================

const message = useMessage() // 消息弹窗
const fmsStore = useFmsStore() // FMS 状态
const loading = ref(false) // 列表的加载中
const saving = ref(false) // 保存的提交中
const exportLoading = ref(false) // 导出的加载中
const edited = ref(false) // 是否存在未保存修改
const subjectType = ref<number>(SUBJECT_TYPE.ASSET) // 当前科目类别
const loadedSubjectType = ref<number>(subjectType.value) // 当前列表对应的科目类别
const tableData = ref<ViewRow[]>([]) // 列表的数据
const accountStartTime = ref<number>() // 账套启用期间（yyyyMM）
const importFormRef = ref<InstanceType<typeof FmsInitialBalanceImportForm>>()
const trialBalanceRef = ref<InstanceType<typeof FmsTrialBalanceDialog>>()
const accountSetId = computed(() => fmsStore.getAccountSetId) // 当前账套编号
// 账套是否从一月启用（startTime 为 yyyyMM 数值；逐月判定不能用 Date 解析，见 ADR 265）
const isJanuary = computed(() => !!accountStartTime.value && accountStartTime.value % 100 === 1)
const isProfitLossType = computed(() => PROFIT_LOSS_TYPES.includes(subjectType.value))

watch(accountSetId, () => loadPage())

// ==================== 页面加载 ====================

/** 加载账套信息与期初余额列表 */
async function loadPage() {
  if (!accountSetId.value) {
    tableData.value = []
    return
  }
  loading.value = true
  try {
    const [accountSet, balances] = await Promise.all([
      FmsAccountSetApi.getAccountSet(accountSetId.value),
      FmsInitialBalanceApi.getInitialBalanceList(accountSetId.value, subjectType.value)
    ])
    accountStartTime.value = accountSet.startTime
    tableData.value = buildViewRows(balances)
    loadedSubjectType.value = subjectType.value
    edited.value = false
  } finally {
    loading.value = false
  }
}

/** 切换科目类别 */
async function changeSubjectType(type: number) {
  if (edited.value) {
    try {
      await message.confirm('当前修改尚未保存，确定放弃修改并切换科目类别吗？')
    } catch {
      subjectType.value = loadedSubjectType.value
      return
    }
  }
  if (isJanuary.value && PROFIT_LOSS_TYPES.includes(type)) {
    message.warning('年初启用的账套不需要录入损益初始余额')
    subjectType.value = loadedSubjectType.value
    return
  }
  subjectType.value = type
  await loadPage()
}

/** 打开导入弹窗 */
function openImportForm() {
  if (!accountSetId.value) return
  importFormRef.value?.open(accountSetId.value)
}

/** 打开试算平衡 */
function openTrialBalance() {
  if (!accountSetId.value) return
  trialBalanceRef.value?.open(accountSetId.value)
}

/** 导出初始余额 */
async function handleExport() {
  if (!accountSetId.value || exportLoading.value) return
  exportLoading.value = true
  try {
    download.excel(
      await FmsInitialBalanceApi.exportInitialBalance(accountSetId.value),
      '财务初始余额.xlsx'
    )
  } finally {
    exportLoading.value = false
  }
}

/** 构建平铺行：父级科目在前，按父级链路计算层级 */
function buildViewRows(list: FmsInitialBalanceVO[]): ViewRow[] {
  const rows: ViewRow[] = []
  const levelMap = new Map<number, number>()
  list.forEach((item) => {
    const level = (item.parentId ? levelMap.get(item.parentId) || 0 : 0) + 1
    levelMap.set(item.subjectId, level)
    rows.push({
      ...item,
      rowKey: `subject-${item.subjectId}`,
      level
    })
  })
  return rows
}

/** 是否可编辑：成员级可写 + 末级科目（父级为汇总展示行） */
function canEdit(row: ViewRow) {
  return fmsStore.isAccountSetWritable && !!row.isLeaf
}

/** 余额方向展示：10 借 / 20 贷 / 30 双向 */
function formatDirection(direction?: number) {
  if (direction === BALANCE_DIRECTION.DEBIT) return '借'
  if (direction === BALANCE_DIRECTION.CREDIT) return '贷'
  return '双'
}

/** 派生年初余额：借向 = 期初 - 累计借 + 累计贷；其余方向 = 期初 + 累计借 - 累计贷 */
function calcYearOpening(row: ViewRow) {
  const opening = Number(row.openingAmount || 0)
  const yearDebit = Number(row.yearDebitAmount || 0)
  const yearCredit = Number(row.yearCreditAmount || 0)
  return row.balanceDirection === BALANCE_DIRECTION.DEBIT
    ? opening - yearDebit + yearCredit
    : opening + yearDebit - yearCredit
}

/** 金额变化时按余额方向重算年初余额，并汇总父级 */
function handleAmountChange(row: ViewRow) {
  if (!isJanuary.value) {
    row.yearOpeningAmount = calcYearOpening(row)
  }
  edited.value = true
  aggregateRows()
}

/** 实际损益发生额变化时标记未保存并汇总父级 */
function handleProfitLossAmountChange() {
  edited.value = true
  aggregateRows()
}

// ==================== 汇总与保存 ====================

/** 汇总父级科目余额：子科目按余额方向计入父科目 */
function aggregateRows() {
  const rows = tableData.value
  const subjectMap = new Map<number, ViewRow>()
  // 1. 非末级科目由子级汇总，先清零
  rows.forEach((row) => {
    subjectMap.set(row.subjectId, row)
    if (!row.isLeaf) {
      AMOUNT_FIELDS.forEach((field) => (row[field] = 0))
    }
  })
  // 2. 平铺列表父级在前，倒序遍历时子级先完成汇总，再逐级累加到父级
  for (let index = rows.length - 1; index >= 0; index--) {
    const row = rows[index]
    const parent = subjectMap.get(row.parentId!)
    if (!parent) continue
    AMOUNT_FIELDS.forEach((field) => {
      const amount = Number(row[field] || 0)
      parent[field] =
        Number(parent[field] || 0) +
        (DIRECT_SUM_FIELDS.has(field) || row.balanceDirection === parent.balanceDirection
          ? amount
          : -amount)
    })
  }
}

/** 保存初始余额（仅末级科目行；服务端强校验已审核凭证锁定） */
async function handleSave() {
  if (!accountSetId.value) return
  const leafRows = tableData.value.filter((row) => row.isLeaf)
  if (isProfitLossType.value && leafRows.some((row) => Math.abs(calcYearOpening(row)) >= 0.005)) {
    message.warning('损益类科目的年初余额必须为 0')
    return
  }
  const balances: FmsInitialBalanceUpdateVO[] = leafRows.map((row) => ({
    subjectId: row.subjectId,
    openingAmount: Number(row.openingAmount || 0),
    yearDebitAmount: Number(row.yearDebitAmount || 0),
    yearCreditAmount: Number(row.yearCreditAmount || 0),
    profitLossAmount: Number(row.profitLossAmount || 0)
  }))
  saving.value = true
  try {
    await FmsInitialBalanceApi.saveInitialBalance(accountSetId.value, balances)
    message.success('保存成功')
    await loadPage()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  loadPage()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(async () => {
  if (!edited.value) return true
  try {
    await message.confirm('当前修改尚未保存，确定放弃修改并离开页面吗？')
    edited.value = false
    return true
  } catch {
    return false
  }
})

/** 浏览器刷新或关闭前提示未保存修改 */
function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!edited.value) return
  event.preventDefault()
  event.returnValue = ''
}
</script>

<style lang="scss" scoped>
:deep(.amount-input) {
  width: 118px;
}

:deep(.amount-input .el-input__inner) {
  text-align: right;
}
</style>
