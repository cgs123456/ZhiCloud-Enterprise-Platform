<template>
  <ContentWrap>
    <!-- 销售订单查询（DDD 读模型）：汇总 + 统计 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="订单编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="订单编号" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="开始时间" prop="start">
        <el-date-picker v-model="queryParams.start" type="date" value-format="YYYY-MM-DD" placeholder="开始时间" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="结束时间" prop="end">
        <el-date-picker v-model="queryParams.end" type="date" value-format="YYYY-MM-DD" placeholder="结束时间" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="statistics">
    <el-descriptions :column="4" border>
      <el-descriptions-item v-for="(v, k) in statistics" :key="k" :label="String(k)">{{ v ?? '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column v-for="c in summaryCols" :key="c" :label="colLabel(c)" align="center" :prop="c" />
      <el-table-column label="操作" align="center" width="90">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { SaleOrderDddApi } from '@/api/erp/sale-order-ddd'

const message = useMessage()

defineOptions({ name: 'ErpSaleOrderDdd' })

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const statistics = ref<any>(null)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined as any,
  start: undefined as any,
  end: undefined as any
})

// 读模型字段按汇总 VO 动态渲染
const summaryCols = ref<string[]>([])
const colLabel = (c: string) => ({ id: '编号', no: '订单编号', customerId: '客户', status: '状态', totalPrice: '总金额', createTime: '创建时间' }[c] || c)

const getList = async () => {
  loading.value = true
  try {
    const data: any = await SaleOrderDddApi.SaleOrderDddSummary(queryParams)
    const arr = data?.list || []
    list.value = arr
    total.value = data?.total || arr.length
    // 依据首行动态确定列，避免写死字段
    if (arr.length && !summaryCols.value.length) {
      summaryCols.value = Object.keys(arr[0]).filter((k) => k !== 'remark')
    }
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await SaleOrderDddApi.SaleOrderDddStatistics(queryParams)
  } catch {
    // 统计失败不阻塞列表
  }
}

const openDetail = async (id: number) => {
  try {
    const d: any = await SaleOrderDddApi.getSaleOrderDddDetail(id)
    message.info(JSON.stringify(d))
  } catch {
    // 详情失败静默
  }
}

onMounted(async () => {
  await getList()
  await loadStatistics()
})
</script>
