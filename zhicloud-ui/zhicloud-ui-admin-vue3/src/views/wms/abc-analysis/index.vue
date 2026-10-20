<template>
  <ContentWrap>
    <!-- ABC 分类分析：汇总指标 + 分类明细 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker v-model="queryParams.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker v-model="queryParams.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 汇总指标 -->
  <ContentWrap v-if="summary">
    <el-descriptions :column="4" border>
      <el-descriptions-item label="SKU 总数">{{ summary.totalSkuCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="出库总数">{{ summary.totalOutQuantity ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="出库总额">{{ summary.totalOutAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="分析时间">{{ summary.analysisTime ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="A 类">{{ summary.classACount ?? '-' }}（{{ summary.classAPercent ?? '-' }}%）</el-descriptions-item>
      <el-descriptions-item label="B 类">{{ summary.classBCount ?? '-' }}（{{ summary.classBPercent ?? '-' }}%）</el-descriptions-item>
      <el-descriptions-item label="C 类">{{ summary.classCCount ?? '-' }}（{{ summary.classCPercent ?? '-' }}%）</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <!-- 分类明细 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="rows" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="物料编码" align="center" prop="itemCode" />
      <el-table-column label="物料名称" align="center" prop="itemName" min-width="140" show-overflow-tooltip />
      <el-table-column label="单位" align="center" prop="unit" />
      <el-table-column label="出库次数" align="center" prop="outCount" />
      <el-table-column label="出库数量" align="center" prop="outQuantity" />
      <el-table-column label="出库金额" align="center" prop="outAmount" />
      <el-table-column label="数量占比" align="center" prop="quantityPercent" />
      <el-table-column label="累计占比" align="center" prop="cumulativePercent" />
      <el-table-column label="ABC 分类" align="center" prop="abcClassification" />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
import { AbcAnalysisApi } from '@/api/wms/abc-analysis'

defineOptions({ name: 'WmsAbcAnalysis' })

const loading = ref(false)
const rows = ref<any[]>([])
const summary = ref<any>(null)
const queryParams = reactive({
  startDate: undefined as any,
  endDate: undefined as any
})

const handleQuery = async () => {
  loading.value = true
  try {
    // 先触发一次分析，再取报告；报告即分析结果
    let res: any = null
    // 分析接口必填起止日期：未选日期时直接取报告，避免每次进页面都弹一个 400 参数错误
    if (queryParams.startDate && queryParams.endDate) {
      try {
        res = await AbcAnalysisApi.AbcAnalysisAnalyze(queryParams)
      } catch {
        res = await AbcAnalysisApi.AbcAnalysisReport(queryParams)
      }
    } else {
      res = await AbcAnalysisApi.AbcAnalysisReport(queryParams)
    }
    summary.value = res || null
    rows.value = res && Array.isArray(res.items) ? res.items : []
  } finally {
    loading.value = false
  }
}

onMounted(() => handleQuery())
</script>
