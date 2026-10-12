<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="invoiceId" prop="invoiceId">
        <el-input-number v-model="queryParams.invoiceId" placeholder="请输入invoiceId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="lineNo" prop="lineNo">
        <el-input-number v-model="queryParams.lineNo" placeholder="请输入lineNo" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入productName" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:tax-invoice-line:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="invoiceId" align="center" prop="invoiceId" />
      <el-table-column label="lineNo" align="center" prop="lineNo" />
      <el-table-column label="productName" align="center" prop="productName" />
      <el-table-column label="specification" align="center" prop="specification" />
      <el-table-column label="unit" align="center" prop="unit" />
      <el-table-column label="quantity" align="center" prop="quantity" />
      <el-table-column label="unitPrice" align="center" prop="unitPrice" />
      <el-table-column label="amountWithoutTax" align="center" prop="amountWithoutTax" />
      <el-table-column label="taxRate" align="center" prop="taxRate" />
      <el-table-column label="taxAmount" align="center" prop="taxAmount" />
      <el-table-column label="amountWithTax" align="center" prop="amountWithTax" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:tax-invoice-line:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:tax-invoice-line:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <TaxInvoiceLineForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { TaxInvoiceLineApi } from '@/api/erp/tax-invoice-line'
import TaxInvoiceLineForm from './taxInvoiceLineForm.vue'

defineOptions({ name: 'ErpTaxInvoiceLine' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  invoiceId: undefined,
  lineNo: undefined,
  productName: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TaxInvoiceLineApi.getTaxInvoiceLinePage(queryParams)
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
    await TaxInvoiceLineApi.deleteTaxInvoiceLine(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
