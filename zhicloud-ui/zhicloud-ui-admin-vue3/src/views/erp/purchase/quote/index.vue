<!-- ERP 采购报价单 -->
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
      <el-form-item label="报价单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入报价单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="询价单编号" prop="inquiryId">
        <el-input-number v-model="queryParams.inquiryId" class="!w-240px" :controls="false" clearable placeholder="请输入询价单编号" />
      </el-form-item>
      <el-form-item label="询价单号" prop="inquiryNo">
        <el-input v-model="queryParams.inquiryNo" class="!w-240px" clearable placeholder="请输入询价单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商编号" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" class="!w-240px" :controls="false" clearable placeholder="请输入供应商编号" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="queryParams.supplierName" class="!w-240px" clearable placeholder="请输入供应商名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合计金额，单位：元" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入合计金额，单位：元" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="询价单明细编号" prop="inquiryItemId">
        <el-input-number v-model="queryParams.inquiryItemId" class="!w-240px" :controls="false" clearable placeholder="请输入询价单明细编号" />
      </el-form-item>
      <el-form-item label="产品编号" prop="productId">
        <el-input-number v-model="queryParams.productId" class="!w-240px" :controls="false" clearable placeholder="请输入产品编号" />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="queryParams.productName" class="!w-240px" clearable placeholder="请输入产品名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="数量" prop="quantity">
        <el-input-number v-model="queryParams.quantity" class="!w-240px" :controls="false" clearable placeholder="请输入数量" />
      </el-form-item>
      <el-form-item label="报价单价，单位：元" prop="unitPrice">
        <el-input-number v-model="queryParams.unitPrice" class="!w-240px" :controls="false" clearable placeholder="请输入报价单价，单位：元" />
      </el-form-item>
      <el-form-item label="报价金额，单位：元" prop="amount">
        <el-input-number v-model="queryParams.amount" class="!w-240px" :controls="false" clearable placeholder="请输入报价金额，单位：元" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
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
          v-hasPermi="['erp:purchase-quote:create']"
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
      <el-table-column align="center" label="报价单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="询价单编号" prop="inquiryId" min-width="120" />
      <el-table-column align="center" label="询价单号" prop="inquiryNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="供应商编号" prop="supplierId" min-width="120" />
      <el-table-column align="center" label="供应商名称" prop="supplierName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="报价时间" prop="quoteDate" width="170" />
      <el-table-column align="center" label="合计金额，单位：元" prop="totalAmount" min-width="120" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="询价单明细编号" prop="inquiryItemId" min-width="120" />
      <el-table-column align="center" label="产品编号" prop="productId" min-width="120" />
      <el-table-column align="center" label="产品名称" prop="productName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="数量" prop="quantity" min-width="120" />
      <el-table-column align="center" label="报价单价，单位：元" prop="unitPrice" min-width="120" />
      <el-table-column align="center" label="报价金额，单位：元" prop="amount" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="报价交货日期" prop="deliveryDate" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:purchase-quote:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:purchase-quote:delete']"
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
  <ErpPurchaseQuoteForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpPurchaseQuoteApi } from '@/api/erp/purchase-quote'
import ErpPurchaseQuoteForm from './ErpPurchaseQuoteForm.vue'

defineOptions({ name: 'ErpPurchaseQuote' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined
  inquiryId: undefined
  inquiryNo: undefined
  supplierId: undefined
  supplierName: undefined
  quoteDate: undefined
  totalAmount: undefined
  status: undefined
  remark: undefined
  inquiryItemId: undefined
  productId: undefined
  productName: undefined
  quantity: undefined
  unitPrice: undefined
  amount: undefined
  deliveryDate: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpPurchaseQuoteApi.getErpPurchaseQuotePage(queryParams)
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
    await ErpPurchaseQuoteApi.deleteErpPurchaseQuote(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
