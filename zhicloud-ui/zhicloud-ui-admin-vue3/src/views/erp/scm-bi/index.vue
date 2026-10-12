<template>
  <ContentWrap>
    <!-- 供应链 BI 分析：库存周转 / 账龄 / 呆滞 / 采购准时 / 价格波动 / 供应商绩效 / 订单履约 / 缺货率 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="开始时间" prop="beginTime">
        <el-date-picker v-model="queryParams.beginTime" type="date" value-format="YYYY-MM-DD" placeholder="开始时间" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker v-model="queryParams.endTime" type="date" value-format="YYYY-MM-DD" placeholder="结束时间" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="闲置天数" prop="idleDays">
        <el-input-number v-model="queryParams.idleDays" :controls="false" placeholder="闲置天数" clearable class="!w-160px" />
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 汇总指标 -->
  <ContentWrap>
    <el-row :gutter="12">
      <el-col :span="6" v-for="c in rateCards" :key="c.label">
        <el-card shadow="hover" class="mb-10px">
          <div class="text-13px text-gray-500">{{ c.label }}</div>
          <div class="text-22px font-bold">{{ c.value ?? '-' }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <!-- 明细 -->
  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="loadDetail">
      <el-tab-pane label="库存账龄" name="aging">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="产品" align="center" prop="productId" />
          <el-table-column label="产品名称" align="center" prop="productName" min-width="140" show-overflow-tooltip />
          <el-table-column label="仓库" align="center" prop="warehouseId" />
          <el-table-column label="数量" align="center" prop="quantity" />
          <el-table-column label="库龄天数" align="center" prop="ageDays" />
          <el-table-column label="库龄段" align="center" prop="agingBucket" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="呆滞库存" name="slow">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="产品" align="center" prop="productId" />
          <el-table-column label="产品名称" align="center" prop="productName" min-width="140" show-overflow-tooltip />
          <el-table-column label="仓库" align="center" prop="warehouseId" />
          <el-table-column label="数量" align="center" prop="quantity" />
          <el-table-column label="最后移动" align="center" prop="lastMoveTime" />
          <el-table-column label="闲置天数" align="center" prop="idleDays" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="价格波动" name="price">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="产品" align="center" prop="productId" />
          <el-table-column label="产品名称" align="center" prop="productName" min-width="140" show-overflow-tooltip />
          <el-table-column label="均价" align="center" prop="avgPrice" />
          <el-table-column label="最高价" align="center" prop="maxPrice" />
          <el-table-column label="最低价" align="center" prop="minPrice" />
          <el-table-column label="波动率" align="center" prop="fluctuationRate" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="供应商绩效" name="supplier">
        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column label="供应商" align="center" prop="supplierId" />
          <el-table-column label="供应商名称" align="center" prop="supplierName" min-width="140" show-overflow-tooltip />
          <el-table-column label="按时率" align="center" prop="onTimeRate" />
          <el-table-column label="质量合格率" align="center" prop="qualityRate" />
          <el-table-column label="综合得分" align="center" prop="overallScore" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>
<script setup lang="ts">
import { ScmBiApi } from '@/api/erp/scm-bi'

defineOptions({ name: 'ErpScmBi' })

const loading = ref(false)
const rows = ref<any[]>([])
const activeTab = ref('aging')
const rates = ref<any>({})
const queryParams = reactive({
  beginTime: undefined as any,
  endTime: undefined as any,
  idleDays: undefined as any
})

const rateCards = computed(() => [
  { label: '库存周转率', value: rates.value?.turnoverRate },
  { label: '采购按时率', value: rates.value?.onTimeRate },
  { label: '订单履约率', value: rates.value?.fulfillmentRate },
  { label: '缺货率', value: rates.value?.stockoutRate }
])

/** 汇总类指标（对象返回） */
const loadRates = async () => {
  try {
    const [turnover, onTime, fulfillment, stockout]: any = await Promise.all([
      ScmBiApi.ScmBiInventoryTurnoverRate(queryParams),
      ScmBiApi.ScmBiPurchaseOnTimeRate(queryParams),
      ScmBiApi.ScmBiOrderFulfillmentRate(queryParams),
      ScmBiApi.ScmBiStockoutRate(queryParams)
    ])
    rates.value = { ...turnover, ...onTime, ...fulfillment, ...stockout }
  } catch {
    // 指标失败不阻塞明细展示
  }
}

/** 明细类指标（数组返回） */
const loadDetail = async () => {
  loading.value = true
  try {
    let res: any
    if (activeTab.value === 'aging') res = await ScmBiApi.ScmBiInventoryAging()
    else if (activeTab.value === 'slow') res = await ScmBiApi.ScmBiSlowMovingInventory(queryParams)
    else if (activeTab.value === 'price') res = await ScmBiApi.ScmBiPriceFluctuation(queryParams)
    else res = await ScmBiApi.ScmBiSupplierPerformance(queryParams)
    rows.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

const load = async () => {
  await Promise.all([loadRates(), loadDetail()])
}

onMounted(() => load())
</script>
