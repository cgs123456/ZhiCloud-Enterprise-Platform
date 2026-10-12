<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="invoiceNo" prop="invoiceNo">
        <el-input v-model="formData.invoiceNo" placeholder="请输入invoiceNo" />
      </el-form-item>
      <el-form-item label="invoiceCode" prop="invoiceCode">
        <el-input v-model="formData.invoiceCode" placeholder="请输入invoiceCode" />
      </el-form-item>
      <el-form-item label="invoiceType" prop="invoiceType">
        <el-input-number v-model="formData.invoiceType" placeholder="请输入invoiceType" />
      </el-form-item>
      <el-form-item label="buyerName" prop="buyerName">
        <el-input v-model="formData.buyerName" placeholder="请输入buyerName" />
      </el-form-item>
      <el-form-item label="buyerTaxNo" prop="buyerTaxNo">
        <el-input v-model="formData.buyerTaxNo" placeholder="请输入buyerTaxNo" />
      </el-form-item>
      <el-form-item label="sellerName" prop="sellerName">
        <el-input v-model="formData.sellerName" placeholder="请输入sellerName" />
      </el-form-item>
      <el-form-item label="sellerTaxNo" prop="sellerTaxNo">
        <el-input v-model="formData.sellerTaxNo" placeholder="请输入sellerTaxNo" />
      </el-form-item>
      <el-form-item label="invoiceDate" prop="invoiceDate">
        <el-input-number v-model="formData.invoiceDate" placeholder="请输入invoiceDate" />
      </el-form-item>
      <el-form-item label="amountWithoutTax" prop="amountWithoutTax">
        <el-input-number v-model="formData.amountWithoutTax" placeholder="请输入amountWithoutTax" />
      </el-form-item>
      <el-form-item label="taxAmount" prop="taxAmount">
        <el-input-number v-model="formData.taxAmount" placeholder="请输入taxAmount" />
      </el-form-item>
      <el-form-item label="amountWithTax" prop="amountWithTax">
        <el-input-number v-model="formData.amountWithTax" placeholder="请输入amountWithTax" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="sourceOrderType" prop="sourceOrderType">
        <el-input v-model="formData.sourceOrderType" placeholder="请输入sourceOrderType" />
      </el-form-item>
      <el-form-item label="sourceOrderId" prop="sourceOrderId">
        <el-input-number v-model="formData.sourceOrderId" placeholder="请输入sourceOrderId" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TaxInvoiceApi } from '@/api/erp/tax-invoice'

defineOptions({ name: 'TaxInvoiceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<any>({})
const formRules = reactive({})
const formRef = ref()

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TaxInvoiceApi.getTaxInvoice(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await TaxInvoiceApi.createTaxInvoice(data)
      message.success(t('common.createSuccess'))
    } else {
      await TaxInvoiceApi.updateTaxInvoice(data)
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
