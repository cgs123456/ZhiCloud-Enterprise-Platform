<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="税率类型" prop="rateType">
        <el-input-number v-model="formData.rateType" placeholder="请输入税率类型" />
      </el-form-item>
      <el-form-item label="税率" prop="rate">
        <el-input-number v-model="formData.rate" placeholder="请输入税率" />
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-input-number v-model="formData.isDefault" placeholder="请输入是否默认" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-input-number v-model="formData.effectiveDate" placeholder="请输入生效日期" />
      </el-form-item>
      <el-form-item label="失效日期" prop="expiryDate">
        <el-input-number v-model="formData.expiryDate" placeholder="请输入失效日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TaxRateApi } from '@/api/erp/tax-rate'

defineOptions({ name: 'TaxRateForm' })

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
      formData.value = await TaxRateApi.getTaxRate(id)
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
      await TaxRateApi.createTaxRate(data)
      message.success(t('common.createSuccess'))
    } else {
      await TaxRateApi.updateTaxRate(data)
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
