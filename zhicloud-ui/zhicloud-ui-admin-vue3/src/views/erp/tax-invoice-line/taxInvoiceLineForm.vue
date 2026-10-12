<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="invoiceId" prop="invoiceId">
        <el-input-number v-model="formData.invoiceId" placeholder="请输入invoiceId" />
      </el-form-item>
      <el-form-item label="lineNo" prop="lineNo">
        <el-input-number v-model="formData.lineNo" placeholder="请输入lineNo" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入productName" />
      </el-form-item>
      <el-form-item label="specification" prop="specification">
        <el-input v-model="formData.specification" placeholder="请输入specification" />
      </el-form-item>
      <el-form-item label="unit" prop="unit">
        <el-input v-model="formData.unit" placeholder="请输入unit" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="unitPrice" prop="unitPrice">
        <el-input-number v-model="formData.unitPrice" placeholder="请输入unitPrice" />
      </el-form-item>
      <el-form-item label="amountWithoutTax" prop="amountWithoutTax">
        <el-input-number v-model="formData.amountWithoutTax" placeholder="请输入amountWithoutTax" />
      </el-form-item>
      <el-form-item label="taxRate" prop="taxRate">
        <el-input-number v-model="formData.taxRate" placeholder="请输入taxRate" />
      </el-form-item>
      <el-form-item label="taxAmount" prop="taxAmount">
        <el-input-number v-model="formData.taxAmount" placeholder="请输入taxAmount" />
      </el-form-item>
      <el-form-item label="amountWithTax" prop="amountWithTax">
        <el-input-number v-model="formData.amountWithTax" placeholder="请输入amountWithTax" />
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
import { TaxInvoiceLineApi } from '@/api/erp/tax-invoice-line'

defineOptions({ name: 'TaxInvoiceLineForm' })

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
      formData.value = await TaxInvoiceLineApi.getTaxInvoiceLine(id)
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
      await TaxInvoiceLineApi.createTaxInvoiceLine(data)
      message.success(t('common.createSuccess'))
    } else {
      await TaxInvoiceLineApi.updateTaxInvoiceLine(data)
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
