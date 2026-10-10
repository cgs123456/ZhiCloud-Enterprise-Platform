<!-- ERP 采购询价单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="询价单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入询价单号" />
      </el-form-item>
      <el-form-item label="询价主题" prop="inquiryName">
        <el-input v-model="formData.inquiryName" class="!w-1/1" placeholder="请输入询价主题" />
      </el-form-item>
      <el-form-item label="供应商编号列表，逗号分隔" prop="supplierIds">
        <el-input v-model="formData.supplierIds" class="!w-1/1" placeholder="请输入供应商编号列表，逗号分隔" />
      </el-form-item>
      <el-form-item label="供应商名称列表，逗号分隔" prop="supplierNames">
        <el-input v-model="formData.supplierNames" class="!w-1/1" placeholder="请输入供应商名称列表，逗号分隔" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="合计金额，单位：元" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入合计金额，单位：元" />
      </el-form-item>
      <el-form-item label="期望交货日期" prop="expectedDeliveryDate">
        <el-date-picker v-model="formData.expectedDeliveryDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择期望交货日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="创建人名称" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建人名称" />
      </el-form-item>
      <el-form-item label="产品编号" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :controls="false" placeholder="请输入产品编号" />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="formData.productName" class="!w-1/1" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="数量" prop="quantity">
        <el-input-number v-model="formData.quantity" class="!w-1/1" :controls="false" placeholder="请输入数量" />
      </el-form-item>
      <el-form-item label="单位" prop="unit">
        <el-input v-model="formData.unit" class="!w-1/1" placeholder="请输入单位" />
      </el-form-item>
      <el-form-item label="期望价，单位：元" prop="unitPrice">
        <el-input-number v-model="formData.unitPrice" class="!w-1/1" :controls="false" placeholder="请输入期望价，单位：元" />
      </el-form-item>
      <el-form-item label="期望交货日期" prop="deliveryDate">
        <el-date-picker v-model="formData.deliveryDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择期望交货日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { ErpPurchaseInquiryApi } from '@/api/erp/purchase-inquiry'

defineOptions({ name: 'ErpPurchaseInquiryForm' })

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
  inquiryName: [required],
  supplierIds: [required],
  supplierNames: [required],
  remark: [required],
  creatorName: [required],
  productName: [required],
  unit: [required],
  remark: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ErpPurchaseInquiryApi.getErpPurchaseInquiry(id)
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
      await ErpPurchaseInquiryApi.createErpPurchaseInquiry(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpPurchaseInquiryApi.updateErpPurchaseInquiry(data)
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
