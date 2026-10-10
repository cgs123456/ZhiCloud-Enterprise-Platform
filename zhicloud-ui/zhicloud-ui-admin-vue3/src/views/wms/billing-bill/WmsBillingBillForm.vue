<!-- WMS 计费账单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="账单号" prop="billNo">
        <el-input v-model="formData.billNo" class="!w-1/1" placeholder="请输入账单号" />
      </el-form-item>
      <el-form-item label="货主编号" prop="ownerId">
        <el-input-number v-model="formData.ownerId" class="!w-1/1" :controls="false" placeholder="请输入货主编号" />
      </el-form-item>
      <el-form-item label="货主名称" prop="ownerName">
        <el-input v-model="formData.ownerName" class="!w-1/1" placeholder="请输入货主名称" />
      </el-form-item>
      <el-form-item label="计费周期开始时间" prop="billingPeriodStart">
        <el-date-picker v-model="formData.billingPeriodStart" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择计费周期开始时间" />
      </el-form-item>
      <el-form-item label="计费周期结束时间" prop="billingPeriodEnd">
        <el-date-picker v-model="formData.billingPeriodEnd" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择计费周期结束时间" />
      </el-form-item>
      <el-form-item label="总金额" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入总金额" />
      </el-form-item>
      <el-form-item label="账单状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入账单状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="创建者名称" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建者名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { WmsBillingBillApi } from '@/api/wms/billing-bill'

defineOptions({ name: 'WmsBillingBillForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  billNo: [required],
  ownerName: [required],
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
      formData.value = await WmsBillingBillApi.getWmsBillingBill(id)
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
      await WmsBillingBillApi.createWmsBillingBill(data)
      message.success(t('common.createSuccess'))
    } else {
      await WmsBillingBillApi.updateWmsBillingBill(data)
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
