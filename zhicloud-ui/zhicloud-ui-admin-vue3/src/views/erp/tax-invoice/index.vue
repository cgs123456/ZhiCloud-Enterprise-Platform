<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="invoiceNo" prop="invoiceNo">
        <el-input v-model="queryParams.invoiceNo" placeholder="请输入invoiceNo" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="invoiceCode" prop="invoiceCode">
        <el-input v-model="queryParams.invoiceCode" placeholder="请输入invoiceCode" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="invoiceType" prop="invoiceType">
        <el-input-number v-model="queryParams.invoiceType" placeholder="请输入invoiceType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:tax-invoice:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="invoiceNo" align="center" prop="invoiceNo" />
      <el-table-column label="invoiceCode" align="center" prop="invoiceCode" />
      <el-table-column label="invoiceType" align="center" prop="invoiceType" />
      <el-table-column label="buyerName" align="center" prop="buyerName" />
      <el-table-column label="buyerTaxNo" align="center" prop="buyerTaxNo" />
      <el-table-column label="sellerName" align="center" prop="sellerName" />
      <el-table-column label="sellerTaxNo" align="center" prop="sellerTaxNo" />
      <el-table-column label="invoiceDate" align="center" prop="invoiceDate" />
      <el-table-column label="amountWithoutTax" align="center" prop="amountWithoutTax" />
      <el-table-column label="taxAmount" align="center" prop="taxAmount" />
      <el-table-column label="amountWithTax" align="center" prop="amountWithTax" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="sourceOrderType" align="center" prop="sourceOrderType" />
      <el-table-column label="sourceOrderId" align="center" prop="sourceOrderId" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:tax-invoice:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:tax-invoice:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <TaxInvoiceForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { TaxInvoiceApi } from '@/api/erp/tax-invoice'
import TaxInvoiceForm from './taxInvoiceForm.vue'

defineOptions({ name: 'ErpTaxInvoice' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  invoiceNo: undefined,
  invoiceCode: undefined,
  invoiceType: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TaxInvoiceApi.getTaxInvoicePage(queryParams)
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

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TaxInvoiceApi.deleteTaxInvoice(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
