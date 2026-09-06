<!-- 客户投诉 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="投诉编号" prop="complaintNo">
        <el-input v-model="formData.complaintNo" class="!w-1/1" placeholder="请输入投诉编号" />
      </el-form-item>
      <el-form-item label="客户 ID" prop="customerId">
        <el-input-number v-model="formData.customerId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入客户 ID" />
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="formData.customerName" class="!w-1/1" placeholder="请输入客户名称" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="formData.productName" class="!w-1/1" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="投诉内容" prop="complaintContent">
        <el-input v-model="formData.complaintContent" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入投诉内容" />
      </el-form-item>
      <el-form-item label="投诉日期" prop="complaintDate">
        <el-date-picker v-model="formData.complaintDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择投诉日期" />
      </el-form-item>
      <el-form-item label="调查根因" prop="rootCause">
        <el-input v-model="formData.rootCause" class="!w-1/1" placeholder="请输入调查根因" />
      </el-form-item>
      <el-form-item label="影响范围" prop="impactScope">
        <el-input v-model="formData.impactScope" class="!w-1/1" placeholder="请输入影响范围" />
      </el-form-item>
      <el-form-item label="处理方式" prop="handleType">
        <el-input-number v-model="formData.handleType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入处理方式" />
      </el-form-item>
      <el-form-item label="处理措施描述" prop="handleAction">
        <el-input v-model="formData.handleAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入处理措施描述" />
      </el-form-item>
      <el-form-item label="关联 8D 报告 ID" prop="eightDId">
        <el-input-number v-model="formData.eightDId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入关联 8D 报告 ID" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { CustomerComplaintApi } from '@/api/qms/complaint/customer'

defineOptions({ name: 'CustomerComplaintForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  customerId: [required],

  customerName: [required],

  complaintDate: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CustomerComplaintApi.getCustomerComplaint(id)
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
    if (formType.value === 'create') {
      await CustomerComplaintApi.createCustomerComplaint(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await CustomerComplaintApi.updateCustomerComplaint(formData.value)
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
