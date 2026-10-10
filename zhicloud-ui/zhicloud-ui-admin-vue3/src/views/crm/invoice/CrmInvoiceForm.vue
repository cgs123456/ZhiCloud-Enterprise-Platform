<!-- CRM 开票管理 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="开票单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入开票单号" />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractId">
        <el-input-number v-model="formData.contractId" class="!w-1/1" :controls="false" placeholder="请输入合同编号" />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="formData.contractName" class="!w-1/1" placeholder="请输入合同名称" />
      </el-form-item>
      <el-form-item label="客户编号" prop="customerId">
        <el-input-number v-model="formData.customerId" class="!w-1/1" :controls="false" placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="formData.customerName" class="!w-1/1" placeholder="请输入客户名称" />
      </el-form-item>
      <el-form-item label="联系人编号" prop="contactId">
        <el-input-number v-model="formData.contactId" class="!w-1/1" :controls="false" placeholder="请输入联系人编号" />
      </el-form-item>
      <el-form-item label="联系人名称" prop="contactName">
        <el-input v-model="formData.contactName" class="!w-1/1" placeholder="请输入联系人名称" />
      </el-form-item>
      <el-form-item label="发票类型" prop="invoiceType">
        <el-input-number v-model="formData.invoiceType" class="!w-1/1" :controls="false" placeholder="请输入发票类型" />
      </el-form-item>
      <el-form-item label="发票号码" prop="invoiceNo">
        <el-input v-model="formData.invoiceNo" class="!w-1/1" placeholder="请输入发票号码" />
      </el-form-item>
      <el-form-item label="购方名称" prop="buyerName">
        <el-input v-model="formData.buyerName" class="!w-1/1" placeholder="请输入购方名称" />
      </el-form-item>
      <el-form-item label="购方税号" prop="buyerTaxNo">
        <el-input v-model="formData.buyerTaxNo" class="!w-1/1" placeholder="请输入购方税号" />
      </el-form-item>
      <el-form-item label="不含税金额" prop="amountWithoutTax">
        <el-input-number v-model="formData.amountWithoutTax" class="!w-1/1" :controls="false" placeholder="请输入不含税金额" />
      </el-form-item>
      <el-form-item label="税额" prop="taxAmount">
        <el-input-number v-model="formData.taxAmount" class="!w-1/1" :controls="false" placeholder="请输入税额" />
      </el-form-item>
      <el-form-item label="含税金额" prop="amountWithTax">
        <el-input-number v-model="formData.amountWithTax" class="!w-1/1" :controls="false" placeholder="请输入含税金额" />
      </el-form-item>
      <el-form-item label="开票日期" prop="invoiceDate">
        <el-date-picker v-model="formData.invoiceDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择开票日期" />
      </el-form-item>
      <el-form-item label="审批状态" prop="auditStatus">
        <el-input-number v-model="formData.auditStatus" class="!w-1/1" :controls="false" placeholder="请输入审批状态" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="formData.processInstanceId" class="!w-1/1" placeholder="请输入工作流编号" />
      </el-form-item>
      <el-form-item label="负责人的用户编号" prop="ownerUserId">
        <el-input-number v-model="formData.ownerUserId" class="!w-1/1" :controls="false" placeholder="请输入负责人的用户编号" />
      </el-form-item>
      <el-form-item label="负责人名字" prop="ownerUserName">
        <el-input v-model="formData.ownerUserName" class="!w-1/1" placeholder="请输入负责人名字" />
      </el-form-item>
      <el-form-item label="负责人部门" prop="ownerUserDeptName">
        <el-input v-model="formData.ownerUserDeptName" class="!w-1/1" placeholder="请输入负责人部门" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="创建人名字" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建人名字" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { CrmInvoiceApi } from '@/api/crm/invoice'

defineOptions({ name: 'CrmInvoiceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  no: [required],
  contractName: [required],
  customerName: [required],
  contactName: [required],
  invoiceNo: [required],
  buyerName: [required],
  buyerTaxNo: [required],
  processInstanceId: [required],
  ownerUserName: [required],
  ownerUserDeptName: [required],
  remark: [required],
  creatorName: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CrmInvoiceApi.getCrmInvoice(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await CrmInvoiceApi.createCrmInvoice(data)
      message.success(t('common.createSuccess'))
    } else {
      await CrmInvoiceApi.updateCrmInvoice(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {}
  formRef.value?.resetFields()
}
</script>
