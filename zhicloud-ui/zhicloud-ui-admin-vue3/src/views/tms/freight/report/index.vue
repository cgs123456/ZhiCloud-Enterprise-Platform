<!-- TMS 运费报表（前端聚合视图：基于既有 /tms/freight/page，零后端改动） -->
<template>
  <ContentWrap>
    <doc-alert title="【运输】运费报表" url="https://doc.zhicloud.cn/tms/freight-report/" />
    <!-- 筛选工作栏 -->
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
      <el-form-item label="承运商" prop="carrierId">
        <el-input-number v-model="queryParams.carrierId" class="!w-240px" :controls="false" clearable placeholder="请输入承运商编号" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          生成报表
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 汇总卡片 -->
  <ContentWrap>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-statistic title="结算单数" :value="summary.count" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="运费总额（元）" :value="summary.totalAmount" :precision="2" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="已结算金额（元）" :value="summary.settledAmount" :precision="2" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="待审核金额（元）" :value="summary.pendingAmount" :precision="2" />
      </el-col>
    </el-row>
  </ContentWrap>

  <!-- 按承运商汇总 -->
  <ContentWrap title="按承运商汇总">
    <el-table v-loading="loading" :data="carrierSummary" size="small" stripe show-summary :summary-method="carrierSummaryTotal">
      <el-table-column align="center" label="承运商编号" prop="carrierId" min-width="120" />
      <el-table-column align="center" label="结算单数" prop="count" min-width="100" />
      <el-table-column align="right" label="运费总额（元）" prop="totalAmount" min-width="140" />
      <el-table-column align="right" label="已结算（元）" prop="settledAmount" min-width="140" />
      <el-table-column align="right" label="待审核（元）" prop="pendingAmount" min-width="140" />
      <el-table-column align="right" label="平均单价（元）" prop="avgUnitPrice" min-width="140" />
    </el-table>
    <el-empty v-if="!loading && carrierSummary.length === 0" description="暂无数据" />
  </ContentWrap>

  <!-- 按月汇总 -->
  <ContentWrap title="按月汇总（按创建月份）">
    <el-table v-loading="loading" :data="monthSummary" size="small" stripe>
      <el-table-column align="center" label="月份" prop="month" min-width="120" />
      <el-table-column align="center" label="结算单数" prop="count" min-width="100" />
      <el-table-column align="right" label="运费总额（元）" prop="totalAmount" min-width="140" />
      <el-table-column align="right" label="已结算（元）" prop="settledAmount" min-width="140" />
      <el-table-column align="right" label="待审核（元）" prop="pendingAmount" min-width="140" />
    </el-table>
    <el-empty v-if="!loading && monthSummary.length === 0" description="暂无数据" />
  </ContentWrap>

  <!-- 明细（点击汇总行下钻） -->
  <ContentWrap title="结算单明细">
    <el-table v-loading="loading" :data="list" size="small" stripe>
      <el-table-column align="center" label="结算单号" prop="no" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="承运商" prop="carrierId" min-width="100" />
      <el-table-column align="center" label="运单" prop="shipmentId" min-width="100" />
      <el-table-column align="center" label="创建月份" prop="createMonth" min-width="110" />
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="right" label="总金额（元）" prop="totalAmount" min-width="120" />
      <el-table-column align="right" label="折扣（元）" prop="discountAmount" min-width="110" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170">
        <template #default="scope">{{ scope.row.createTime || '-' }}</template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { TmsFreightApi, TmsFreightStatus } from '@/api/tms/freight'

defineOptions({ name: 'TmsFreightReport' })

const STATUS_OPTIONS = [
  { value: TmsFreightStatus.PENDING_AUDIT, label: '待审核' },
  { value: TmsFreightStatus.AUDITED, label: '已审核' },
  { value: TmsFreightStatus.SETTLED, label: '已结算' },
  { value: TmsFreightStatus.REJECTED, label: '已驳回' }
]

const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === TmsFreightStatus.SETTLED) return 'success'
  if (v === TmsFreightStatus.AUDITED) return 'primary'
  if (v === TmsFreightStatus.REJECTED) return 'danger'
  return 'warning'
}

const loading = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 200, // 报表场景一次拉全量做前端聚合
  no: undefined,
  carrierId: undefined,
  status: undefined
})
const queryFormRef = ref()

const list = ref<any[]>([]) // 明细
const summary = reactive({ count: 0, totalAmount: 0, settledAmount: 0, pendingAmount: 0 })
const carrierSummary = ref<any[]>([])
const monthSummary = ref<any[]>([])

const toNum = (v: any) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}
const round2 = (n: number) => Math.round(n * 100) / 100
/** createTime 兼容 Date / epochMillis 数字 / ISO 字符串，取 yyyy-MM */
const monthOf = (v: any): string => {
  if (v == null || v === '') return '未知'
  let d: Date
  if (typeof v === 'number') {
    d = new Date(v)
  } else {
    const s = String(v)
    // 纯数字串按 epochMillis
    d = /^\d+$/.test(s) ? new Date(Number(s)) : new Date(s)
  }
  if (Number.isNaN(d.getTime())) return '未知'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const handleQuery = async () => {
  loading.value = true
  try {
    const data = await TmsFreightApi.getTmsFreightPage(queryParams)
    const rows = data.list || []
    list.value = rows.map((r: any) => ({ ...r, createMonth: monthOf(r.createTime) }))
    aggregate()
  } finally {
    loading.value = false
  }
}

const aggregate = () => {
  const acc = {
    count: 0,
    totalAmount: 0,
    settledAmount: 0,
    pendingAmount: 0
  }
  const byCarrier = new Map<number, any>()
  const byMonth = new Map<string, any>()

  for (const row of list.value) {
    const amt = toNum(row.totalAmount)
    acc.count += 1
    acc.totalAmount += amt
    if (row.status === TmsFreightStatus.SETTLED) acc.settledAmount += amt
    if (row.status === TmsFreightStatus.PENDING_AUDIT) acc.pendingAmount += amt

    const key = row.carrierId ?? 0
    const c = byCarrier.get(key) || { carrierId: key, count: 0, totalAmount: 0, settledAmount: 0, pendingAmount: 0, unitPriceSum: 0, unitPriceCount: 0 }
    c.count += 1
    c.totalAmount += amt
    if (toNum(row.unitPrice) > 0) {
      c.unitPriceSum += toNum(row.unitPrice)
      c.unitPriceCount += 1
    }
    if (row.status === TmsFreightStatus.SETTLED) c.settledAmount += amt
    if (row.status === TmsFreightStatus.PENDING_AUDIT) c.pendingAmount += amt
    byCarrier.set(key, c)

    const m = row.createMonth || '未知'
    const mm = byMonth.get(m) || { month: m, count: 0, totalAmount: 0, settledAmount: 0, pendingAmount: 0 }
    mm.count += 1
    mm.totalAmount += amt
    if (row.status === TmsFreightStatus.SETTLED) mm.settledAmount += amt
    if (row.status === TmsFreightStatus.PENDING_AUDIT) mm.pendingAmount += amt
    byMonth.set(m, mm)
  }

  summary.count = acc.count
  summary.totalAmount = round2(acc.totalAmount)
  summary.settledAmount = round2(acc.settledAmount)
  summary.pendingAmount = round2(acc.pendingAmount)

  carrierSummary.value = [...byCarrier.values()]
    .map((c) => ({
      ...c,
      totalAmount: round2(c.totalAmount),
      settledAmount: round2(c.settledAmount),
      pendingAmount: round2(c.pendingAmount),
      avgUnitPrice: c.unitPriceCount ? round2(c.unitPriceSum / c.unitPriceCount) : 0
    }))
    .sort((a, b) => b.totalAmount - a.totalAmount)

  monthSummary.value = [...byMonth.values()]
    .map((m) => ({
      ...m,
      totalAmount: round2(m.totalAmount),
      settledAmount: round2(m.settledAmount),
      pendingAmount: round2(m.pendingAmount)
    }))
    .sort((a, b) => (a.month < b.month ? 1 : -1))
}

const carrierSummaryTotal = () => {
  const sum = (field: string) => round2(carrierSummary.value.reduce((p, c) => p + toNum(c[field]), 0))
  return ['合计', carrierSummary.value.length, sum('totalAmount'), sum('settledAmount'), sum('pendingAmount'), '-']
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

onMounted(() => handleQuery())
</script>
