<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="bizType" prop="bizType">
        <el-input-number v-model="formData.bizType" placeholder="请输入bizType" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="formData.amount" placeholder="请输入金额" />
      </el-form-item>
      <el-form-item label="银行账户" prop="bankAccountId">
        <el-input-number v-model="formData.bankAccountId" placeholder="请输入银行账户" />
      </el-form-item>
      <el-form-item label="bizOrderId" prop="bizOrderId">
        <el-input-number v-model="formData.bizOrderId" placeholder="请输入bizOrderId" />
      </el-form-item>
      <el-form-item label="bizOrderType" prop="bizOrderType">
        <el-input v-model="formData.bizOrderType" placeholder="请输入bizOrderType" />
      </el-form-item>
      <el-form-item label="occurDate" prop="occurDate">
        <el-input-number v-model="formData.occurDate" placeholder="请输入occurDate" />
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
import { CashFlowApi } from '@/api/erp/cash-flow'

defineOptions({ name: 'CashFlowForm' })

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
      formData.value = await CashFlowApi.getCashFlow(id)
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
      await CashFlowApi.createCashFlow(data)
      message.success(t('common.createSuccess'))
    } else {
      await CashFlowApi.updateCashFlow(data)
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
