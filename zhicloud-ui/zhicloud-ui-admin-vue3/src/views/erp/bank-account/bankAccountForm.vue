<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="账号" prop="accountNo">
        <el-input v-model="formData.accountNo" placeholder="请输入账号" />
      </el-form-item>
      <el-form-item label="账户名称" prop="accountName">
        <el-input v-model="formData.accountName" placeholder="请输入账户名称" />
      </el-form-item>
      <el-form-item label="银行" prop="bankName">
        <el-input v-model="formData.bankName" placeholder="请输入银行" />
      </el-form-item>
      <el-form-item label="开户行" prop="bankBranch">
        <el-input v-model="formData.bankBranch" placeholder="请输入开户行" />
      </el-form-item>
      <el-form-item label="余额" prop="balance">
        <el-input-number v-model="formData.balance" placeholder="请输入余额" />
      </el-form-item>
      <el-form-item label="币种" prop="currencyId">
        <el-input-number v-model="formData.currencyId" placeholder="请输入币种" />
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
import { BankAccountApi } from '@/api/erp/bank-account'

defineOptions({ name: 'BankAccountForm' })

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
      formData.value = await BankAccountApi.getBankAccount(id)
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
      await BankAccountApi.createBankAccount(data)
      message.success(t('common.createSuccess'))
    } else {
      await BankAccountApi.updateBankAccount(data)
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
