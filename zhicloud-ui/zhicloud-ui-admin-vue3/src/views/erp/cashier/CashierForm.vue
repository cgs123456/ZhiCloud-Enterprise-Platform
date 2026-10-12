<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编号" prop="no">
        <el-input v-model="formData.no" placeholder="请输入编号" />
      </el-form-item>
      <el-form-item label="收付类型" prop="cashierType">
        <el-input-number v-model="formData.cashierType" placeholder="请输入收付类型" />
      </el-form-item>
      <el-form-item label="银行账户" prop="bankAccountId">
        <el-input-number v-model="formData.bankAccountId" placeholder="请输入银行账户" />
      </el-form-item>
      <el-form-item label="对方户名" prop="counterpartyName">
        <el-input v-model="formData.counterpartyName" placeholder="请输入对方户名" />
      </el-form-item>
      <el-form-item label="对方账号" prop="counterpartyAccount">
        <el-input v-model="formData.counterpartyAccount" placeholder="请输入对方账号" />
      </el-form-item>
      <el-form-item label="对方银行" prop="counterpartyBank">
        <el-input v-model="formData.counterpartyBank" placeholder="请输入对方银行" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="formData.amount" placeholder="请输入金额" />
      </el-form-item>
      <el-form-item label="收付方式" prop="paymentMethod">
        <el-input-number v-model="formData.paymentMethod" placeholder="请输入收付方式" />
      </el-form-item>
      <el-form-item label="收付日期" prop="paymentDate">
        <el-input-number v-model="formData.paymentDate" placeholder="请输入收付日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="银行流水号" prop="bankSerialNo">
        <el-input v-model="formData.bankSerialNo" placeholder="请输入银行流水号" />
      </el-form-item>
      <el-form-item label="业务单号" prop="businessOrderNo">
        <el-input v-model="formData.businessOrderNo" placeholder="请输入业务单号" />
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
import { CashierApi } from '@/api/erp/cashier'

defineOptions({ name: 'CashierForm' })

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
      formData.value = await CashierApi.getCashier(id)
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
      await CashierApi.createCashier(data)
      message.success(t('common.createSuccess'))
    } else {
      await CashierApi.updateCashier(data)
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
