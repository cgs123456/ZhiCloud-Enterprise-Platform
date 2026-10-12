<!-- ERP 财务报表（资产负债表 / 利润表 / 现金流量表） -->
<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="报表日期" prop="asOfDate">
        <el-date-picker v-model="queryParams.asOfDate" type="date" value-format="YYYY-MM-DD" class="!w-200px" placeholder="资产负债表日期" />
      </el-form-item>
      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker v-model="queryParams.startDate" type="date" value-format="YYYY-MM-DD" class="!w-200px" placeholder="利润表/现金流开始" />
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker v-model="queryParams.endDate" type="date" value-format="YYYY-MM-DD" class="!w-200px" placeholder="利润表/现金流结束" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          生成报表
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <el-tabs v-model="activeTab">
    <el-tab-pane label="资产负债表" name="balance">
      <ContentWrap v-loading="loading">
        <el-row :gutter="16" class="mb-12px">
          <el-col :span="8"><b>资产合计：</b>{{ balance.totalAssets ?? 0 }}</el-col>
          <el-col :span="8"><b>负债合计：</b>{{ balance.totalLiabilities ?? 0 }}</el-col>
          <el-col :span="8"><b>所有者权益合计：</b>{{ balance.totalEquity ?? 0 }}</el-col>
        </el-row>
        <StatementSection title="资产" :items="balance.assetItems || []" />
        <StatementSection title="负债" :items="balance.liabilityItems || []" />
        <StatementSection title="所有者权益" :items="balance.equityItems || []" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="利润表" name="income">
      <ContentWrap v-loading="loading">
        <el-row :gutter="16" class="mb-12px">
          <el-col :span="6"><b>收入合计：</b>{{ income.totalRevenue ?? 0 }}</el-col>
          <el-col :span="6"><b>成本合计：</b>{{ income.totalCost ?? 0 }}</el-col>
          <el-col :span="6"><b>费用合计：</b>{{ income.totalExpense ?? 0 }}</el-col>
          <el-col :span="6"><b>净利润：</b>{{ income.netProfit ?? 0 }}</el-col>
        </el-row>
        <StatementSection title="收入" :items="income.revenueItems || []" />
        <StatementSection title="成本" :items="income.costItems || []" />
        <StatementSection title="费用" :items="income.expenseItems || []" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="现金流量表" name="cashflow">
      <ContentWrap v-loading="loading">
        <el-row :gutter="16" class="mb-12px">
          <el-col :span="6"><b>经营活动净流量：</b>{{ cashflow.netOperatingCashFlow ?? 0 }}</el-col>
          <el-col :span="6"><b>投资活动净流量：</b>{{ cashflow.netInvestingCashFlow ?? 0 }}</el-col>
          <el-col :span="6"><b>筹资活动净流量：</b>{{ cashflow.netFinancingCashFlow ?? 0 }}</el-col>
          <el-col :span="6"><b>现金净增加额：</b>{{ cashflow.netCashFlow ?? 0 }}</el-col>
        </el-row>
        <StatementSection title="经营活动" :items="cashflow.operatingItems || []" />
        <StatementSection title="投资活动" :items="cashflow.investingItems || []" />
        <StatementSection title="筹资活动" :items="cashflow.financingItems || []" />
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import { ErpFinancialStatementApi } from '@/api/erp/financial-statement'
import StatementSection from './StatementSection.vue'

defineOptions({ name: 'ErpFinancialStatement' })

const loading = ref(false)
const activeTab = ref('balance')
const balance = ref<any>({})
const income = ref<any>({})
const cashflow = ref<any>({})

const queryParams = reactive<any>({
  asOfDate: undefined,
  startDate: undefined,
  endDate: undefined
})

const today = new Date()
const iso = (d: Date) => d.toISOString().slice(0, 10)
queryParams.asOfDate = iso(today)
queryParams.startDate = iso(new Date(today.getFullYear(), today.getMonth(), 1))
queryParams.endDate = iso(today)

const handleQuery = async () => {
  loading.value = true
  try {
    if (queryParams.asOfDate) {
      balance.value = await ErpFinancialStatementApi.getBalanceSheet({ asOfDate: queryParams.asOfDate })
    }
    if (queryParams.startDate && queryParams.endDate) {
      income.value = await ErpFinancialStatementApi.getIncomeStatement({
        startDate: queryParams.startDate,
        endDate: queryParams.endDate
      })
      cashflow.value = await ErpFinancialStatementApi.getCashFlow({
        startDate: queryParams.startDate,
        endDate: queryParams.endDate
      })
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => handleQuery())
</script>
