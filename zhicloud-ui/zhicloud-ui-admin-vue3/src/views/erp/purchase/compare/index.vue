<!-- ERP 采购比价单 -->
<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="90px">
      <el-form-item label="比价单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入比价单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="询价单号" prop="inquiryNo">
        <el-input v-model="queryParams.inquiryNo" class="!w-240px" clearable placeholder="请输入询价单号" @keyup.enter="handleQuery" />
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="比价单号" prop="no" min-width="150" />
      <el-table-column align="center" label="询价单号" prop="inquiryNo" min-width="150" />
      <el-table-column align="center" label="推荐供应商" prop="recommendSupplierName" min-width="150" />
      <el-table-column align="center" label="推荐理由" prop="recommendReason" min-width="200" show-overflow-tooltip />
      <el-table-column align="center" label="报价数" prop="totalQuoteCount" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="100">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">查看明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 明细弹窗 -->
  <Dialog v-model="detailVisible" title="比价明细" width="900px">
    <el-table :data="detail.lines || []" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="产品" prop="productName" min-width="160" />
      <el-table-column align="center" label="供应商" prop="supplierName" min-width="150" />
      <el-table-column align="center" label="单价" prop="unitPrice" min-width="110" />
      <el-table-column align="center" label="金额" prop="amount" min-width="110" />
      <el-table-column align="center" label="交期" prop="deliveryDate" min-width="120" />
      <el-table-column align="center" label="是否推荐" prop="isRecommended" min-width="90" />
    </el-table>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpPurchaseCompareApi } from '@/api/erp/purchase-compare'

defineOptions({ name: 'ErpPurchaseCompare' })

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>({})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  inquiryNo: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpPurchaseCompareApi.getComparePage(queryParams)
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

const openDetail = async (id: number) => {
  detail.value = await ErpPurchaseCompareApi.getCompare(id)
  detailVisible.value = true
}

onMounted(() => getList())
</script>
