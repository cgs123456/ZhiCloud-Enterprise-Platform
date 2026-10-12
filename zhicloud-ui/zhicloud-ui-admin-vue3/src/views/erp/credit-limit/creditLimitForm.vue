<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="customerId" prop="customerId">
        <el-input-number v-model="formData.customerId" placeholder="请输入customerId" />
      </el-form-item>
      <el-form-item label="creditLimit" prop="creditLimit">
        <el-input-number v-model="formData.creditLimit" placeholder="请输入creditLimit" />
      </el-form-item>
      <el-form-item label="usedAmount" prop="usedAmount">
        <el-input-number v-model="formData.usedAmount" placeholder="请输入usedAmount" />
      </el-form-item>
      <el-form-item label="availableAmount" prop="availableAmount">
        <el-input-number v-model="formData.availableAmount" placeholder="请输入availableAmount" />
      </el-form-item>
      <el-form-item label="overdueAmount" prop="overdueAmount">
        <el-input-number v-model="formData.overdueAmount" placeholder="请输入overdueAmount" />
      </el-form-item>
      <el-form-item label="warningRatio" prop="warningRatio">
        <el-input-number v-model="formData.warningRatio" placeholder="请输入warningRatio" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
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
import { CreditLimitApi } from '@/api/erp/credit-limit'

defineOptions({ name: 'CreditLimitForm' })

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
      formData.value = await CreditLimitApi.getCreditLimit(id)
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
      await CreditLimitApi.createCreditLimit(data)
      message.success(t('common.createSuccess'))
    } else {
      await CreditLimitApi.updateCreditLimit(data)
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
