<!-- ERP 采购询价单 -->
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
      <el-form-item label="询价单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入询价单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="询价主题" prop="inquiryName">
        <el-input v-model="queryParams.inquiryName" class="!w-240px" clearable placeholder="请输入询价主题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商编号列表，逗号分隔" prop="supplierIds">
        <el-input v-model="queryParams.supplierIds" class="!w-240px" clearable placeholder="请输入供应商编号列表，逗号分隔" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商名称列表，逗号分隔" prop="supplierNames">
        <el-input v-model="queryParams.supplierNames" class="!w-240px" clearable placeholder="请输入供应商名称列表，逗号分隔" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="合计金额，单位：元" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入合计金额，单位：元" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建人名称" prop="creatorName">
        <el-input v-model="queryParams.creatorName" class="!w-240px" clearable placeholder="请输入创建人名称" @keyup.enter="handleQuery" />
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
      <el-form-item label="单位" prop="unit">
        <el-input v-model="queryParams.unit" class="!w-240px" clearable placeholder="请输入单位" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="期望价，单位：元" prop="unitPrice">
        <el-input-number v-model="queryParams.unitPrice" class="!w-240px" :controls="false" clearable placeholder="请输入期望价，单位：元" />
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
          v-hasPermi="['erp:purchase-inquiry:create']"
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
      <el-table-column align="center" label="询价单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="询价主题" prop="inquiryName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="供应商编号列表，逗号分隔" prop="supplierIds" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="供应商名称列表，逗号分隔" prop="supplierNames" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="合计金额，单位：元" prop="totalAmount" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="期望交货日期" prop="expectedDeliveryDate" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建人名称" prop="creatorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="产品编号" prop="productId" min-width="120" />
      <el-table-column align="center" label="产品名称" prop="productName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="数量" prop="quantity" min-width="120" />
      <el-table-column align="center" label="单位" prop="unit" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="期望价，单位：元" prop="unitPrice" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="期望交货日期" prop="deliveryDate" width="170" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['erp:purchase-inquiry:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['erp:purchase-inquiry:delete']"
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
  <ErpPurchaseInquiryForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { ErpPurchaseInquiryApi } from '@/api/erp/purchase-inquiry'
import ErpPurchaseInquiryForm from './ErpPurchaseInquiryForm.vue'

defineOptions({ name: 'ErpPurchaseInquiry' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  inquiryName: undefined,
  supplierIds: undefined,
  supplierNames: undefined,
  status: undefined,
  totalAmount: undefined,
  expectedDeliveryDate: undefined,
  remark: undefined,
  creatorName: undefined,
  productId: undefined,
  productName: undefined,
  quantity: undefined,
  unit: undefined,
  unitPrice: undefined,
  deliveryDate: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ErpPurchaseInquiryApi.getErpPurchaseInquiryPage(queryParams)
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
    await ErpPurchaseInquiryApi.deleteErpPurchaseInquiry(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
