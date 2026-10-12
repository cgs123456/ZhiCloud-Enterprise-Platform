<!-- CRM 开票管理 -->
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
      <el-form-item label="开票单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入开票单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractId">
        <el-input-number v-model="queryParams.contractId" class="!w-240px" :controls="false" clearable placeholder="请输入合同编号" />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="queryParams.contractName" class="!w-240px" clearable placeholder="请输入合同名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户编号" prop="customerId">
        <el-input-number v-model="queryParams.customerId" class="!w-240px" :controls="false" clearable placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="queryParams.customerName" class="!w-240px" clearable placeholder="请输入客户名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系人编号" prop="contactId">
        <el-input-number v-model="queryParams.contactId" class="!w-240px" :controls="false" clearable placeholder="请输入联系人编号" />
      </el-form-item>
      <el-form-item label="联系人名称" prop="contactName">
        <el-input v-model="queryParams.contactName" class="!w-240px" clearable placeholder="请输入联系人名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="发票类型" prop="invoiceType">
        <el-input-number v-model="queryParams.invoiceType" class="!w-240px" :controls="false" clearable placeholder="请输入发票类型" />
      </el-form-item>
      <el-form-item label="发票号码" prop="invoiceNo">
        <el-input v-model="queryParams.invoiceNo" class="!w-240px" clearable placeholder="请输入发票号码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="购方名称" prop="buyerName">
        <el-input v-model="queryParams.buyerName" class="!w-240px" clearable placeholder="请输入购方名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="购方税号" prop="buyerTaxNo">
        <el-input v-model="queryParams.buyerTaxNo" class="!w-240px" clearable placeholder="请输入购方税号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="不含税金额" prop="amountWithoutTax">
        <el-input-number v-model="queryParams.amountWithoutTax" class="!w-240px" :controls="false" clearable placeholder="请输入不含税金额" />
      </el-form-item>
      <el-form-item label="税额" prop="taxAmount">
        <el-input-number v-model="queryParams.taxAmount" class="!w-240px" :controls="false" clearable placeholder="请输入税额" />
      </el-form-item>
      <el-form-item label="含税金额" prop="amountWithTax">
        <el-input-number v-model="queryParams.amountWithTax" class="!w-240px" :controls="false" clearable placeholder="请输入含税金额" />
      </el-form-item>
      <el-form-item label="审批状态" prop="auditStatus">
        <el-input-number v-model="queryParams.auditStatus" class="!w-240px" :controls="false" clearable placeholder="请输入审批状态" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="queryParams.processInstanceId" class="!w-240px" clearable placeholder="请输入工作流编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="负责人的用户编号" prop="ownerUserId">
        <el-input-number v-model="queryParams.ownerUserId" class="!w-240px" :controls="false" clearable placeholder="请输入负责人的用户编号" />
      </el-form-item>
      <el-form-item label="负责人名字" prop="ownerUserName">
        <el-input v-model="queryParams.ownerUserName" class="!w-240px" clearable placeholder="请输入负责人名字" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="负责人部门" prop="ownerUserDeptName">
        <el-input v-model="queryParams.ownerUserDeptName" class="!w-240px" clearable placeholder="请输入负责人部门" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建人名字" prop="creatorName">
        <el-input v-model="queryParams.creatorName" class="!w-240px" clearable placeholder="请输入创建人名字" @keyup.enter="handleQuery" />
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
          v-hasPermi="['crm:invoice:create']"
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
      <el-table-column align="center" label="开票单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合同编号" prop="contractId" min-width="120" />
      <el-table-column align="center" label="合同名称" prop="contractName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="客户编号" prop="customerId" min-width="120" />
      <el-table-column align="center" label="客户名称" prop="customerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="联系人编号" prop="contactId" min-width="120" />
      <el-table-column align="center" label="联系人名称" prop="contactName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="发票类型" prop="invoiceType" min-width="120" />
      <el-table-column align="center" label="发票号码" prop="invoiceNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="购方名称" prop="buyerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="购方税号" prop="buyerTaxNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="不含税金额" prop="amountWithoutTax" min-width="120" />
      <el-table-column align="center" label="税额" prop="taxAmount" min-width="120" />
      <el-table-column align="center" label="含税金额" prop="amountWithTax" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="开票日期" prop="invoiceDate" width="170" />
      <el-table-column align="center" label="审批状态" prop="auditStatus" min-width="120" />
      <el-table-column align="center" label="工作流编号" prop="processInstanceId" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="负责人的用户编号" prop="ownerUserId" min-width="120" />
      <el-table-column align="center" label="负责人名字" prop="ownerUserName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="负责人部门" prop="ownerUserDeptName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建人名字" prop="creatorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['crm:invoice:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['crm:invoice:delete']"
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
  <CrmInvoiceForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CrmInvoiceApi } from '@/api/crm/invoice'
import CrmInvoiceForm from './CrmInvoiceForm.vue'

defineOptions({ name: 'CrmInvoice' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  contractId: undefined,
  contractName: undefined,
  customerId: undefined,
  customerName: undefined,
  contactId: undefined,
  contactName: undefined,
  invoiceType: undefined,
  invoiceNo: undefined,
  buyerName: undefined,
  buyerTaxNo: undefined,
  amountWithoutTax: undefined,
  taxAmount: undefined,
  amountWithTax: undefined,
  invoiceDate: undefined,
  auditStatus: undefined,
  processInstanceId: undefined,
  ownerUserId: undefined,
  ownerUserName: undefined,
  ownerUserDeptName: undefined,
  remark: undefined,
  creatorName: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CrmInvoiceApi.getCrmInvoicePage(queryParams)
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
    await CrmInvoiceApi.deleteCrmInvoice(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
