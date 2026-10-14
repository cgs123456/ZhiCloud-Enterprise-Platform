<!-- CRM 销售订单（软状态机：草稿可改/删/确认/提交；后续状态只读） -->
<template>
  <ContentWrap>
    <doc-alert title="【销售】销售订单管理" url="https://doc.zhicloud.cn/crm/sale-order/" />
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="订单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入订单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-input-number v-model="queryParams.customerId" class="!w-240px" :controls="false" clearable placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="合同" prop="contractId">
        <el-input-number v-model="queryParams.contractId" class="!w-240px" :controls="false" clearable placeholder="请输入合同编号" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          v-hasPermi="['crm:sale-order:create']"
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
      <el-table-column align="center" label="订单号" prop="no" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="客户" prop="customerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合同" prop="contractName" min-width="140" show-overflow-tooltip />
      <el-table-column align="center" label="订单日期" prop="orderDate" width="120" />
      <el-table-column align="right" label="总金额" prop="totalAmount" min-width="110" />
      <el-table-column align="right" label="最终金额" prop="finalAmount" min-width="110" />
      <el-table-column align="center" label="付款状态" prop="paymentStatus" min-width="100">
        <template #default="scope">{{ paymentStatusText(scope.row.paymentStatus) }}</template>
      </el-table-column>
      <el-table-column align="center" label="发货状态" prop="deliveryStatus" min-width="100">
        <template #default="scope">{{ deliveryStatusText(scope.row.deliveryStatus) }}</template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="负责人" prop="ownerUserName" min-width="100" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="260">
        <template #default="scope">
          <!-- 仅草稿可编辑/删除/确认/提交 -->
          <template v-if="scope.row.status === STATUS.DRAFT">
            <el-button
              v-hasPermi="['crm:sale-order:update']"
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
            >
              修改
            </el-button>
            <el-button
              v-hasPermi="['crm:sale-order:update']"
              link
              type="success"
              @click="handleConfirm(scope.row)"
            >
              确认
            </el-button>
            <el-button
              v-hasPermi="['crm:sale-order:update']"
              link
              type="warning"
              @click="handleSubmit(scope.row)"
            >
              提交审批
            </el-button>
            <el-button
              v-hasPermi="['crm:sale-order:delete']"
              link
              type="danger"
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
          <template v-else>
            <el-button
              v-hasPermi="['crm:sale-order:query']"
              link
              type="primary"
              @click="openForm('detail', scope.row.id)"
            >
              详情
            </el-button>
          </template>
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

  <!-- 表单弹窗（新增/修改/详情，含产品明细子表） -->
  <CrmSaleOrderForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CrmSaleOrderApi, CrmSaleOrderStatus } from '@/api/crm/saleOrder'
import CrmSaleOrderForm from './CrmSaleOrderForm.vue'

defineOptions({ name: 'CrmSaleOrder' })

const message = useMessage()

const STATUS = CrmSaleOrderStatus

const STATUS_OPTIONS = [
  { value: CrmSaleOrderStatus.DRAFT, label: '草稿' },
  { value: CrmSaleOrderStatus.CONFIRMED, label: '已确认' },
  { value: CrmSaleOrderStatus.SHIPPED, label: '已发货' },
  { value: CrmSaleOrderStatus.COMPLETED, label: '已完成' },
  { value: CrmSaleOrderStatus.CANCELLED, label: '已取消' }
]

const PAYMENT_OPTIONS = [
  { value: 10, label: '未付款' },
  { value: 20, label: '部分付款' },
  { value: 30, label: '已付款' }
]

const DELIVERY_OPTIONS = [
  { value: 10, label: '未发货' },
  { value: 20, label: '部分发货' },
  { value: 30, label: '已发货' },
  { value: 40, label: '已签收' }
]

const statusText = (v?: number) => STATUS_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const statusTagType = (v?: number) => {
  if (v === CrmSaleOrderStatus.COMPLETED) return 'success'
  if (v === CrmSaleOrderStatus.CANCELLED) return 'info'
  if (v === CrmSaleOrderStatus.DRAFT) return 'warning'
  return 'primary'
}
const paymentStatusText = (v?: number) => PAYMENT_OPTIONS.find((i) => i.value === v)?.label ?? '-'
const deliveryStatusText = (v?: number) => DELIVERY_OPTIONS.find((i) => i.value === v)?.label ?? '-'

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  customerId: undefined,
  contractId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CrmSaleOrderApi.getCrmSaleOrderPage(queryParams)
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
    await CrmSaleOrderApi.deleteCrmSaleOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const handleConfirm = async (row: any) => {
  await message.confirm(`是否确认订单「${row.no}」？（仅草稿可确认）`)
  try {
    await CrmSaleOrderApi.confirmCrmSaleOrder(row.id)
    message.success('已确认')
    await getList()
  } catch {}
}

const handleSubmit = async (row: any) => {
  await message.confirm(`是否提交订单「${row.no}」的审批？（仅草稿可提交）`)
  try {
    await CrmSaleOrderApi.submitCrmSaleOrder(row.id)
    message.success('已提交')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
