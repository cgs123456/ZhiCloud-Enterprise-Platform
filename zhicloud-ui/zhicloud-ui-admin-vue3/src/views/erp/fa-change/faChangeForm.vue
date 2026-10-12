<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="assetId" prop="assetId">
        <el-input-number v-model="formData.assetId" placeholder="请输入assetId" />
      </el-form-item>
      <el-form-item label="changeType" prop="changeType">
        <el-input-number v-model="formData.changeType" placeholder="请输入changeType" />
      </el-form-item>
      <el-form-item label="beforeValue" prop="beforeValue">
        <el-input v-model="formData.beforeValue" placeholder="请输入beforeValue" />
      </el-form-item>
      <el-form-item label="afterValue" prop="afterValue">
        <el-input v-model="formData.afterValue" placeholder="请输入afterValue" />
      </el-form-item>
      <el-form-item label="changeDate" prop="changeDate">
        <el-input-number v-model="formData.changeDate" placeholder="请输入changeDate" />
      </el-form-item>
      <el-form-item label="changeReason" prop="changeReason">
        <el-input v-model="formData.changeReason" placeholder="请输入changeReason" />
      </el-form-item>
      <el-form-item label="operatorId" prop="operatorId">
        <el-input-number v-model="formData.operatorId" placeholder="请输入operatorId" />
      </el-form-item>
      <el-form-item label="approverId" prop="approverId">
        <el-input-number v-model="formData.approverId" placeholder="请输入approverId" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="rejectReason" prop="rejectReason">
        <el-input v-model="formData.rejectReason" placeholder="请输入rejectReason" />
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
import { FaChangeApi } from '@/api/erp/fa-change'

defineOptions({ name: 'FaChangeForm' })

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
      formData.value = await FaChangeApi.getFaChange(id)
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
      await FaChangeApi.createFaChange(data)
      message.success(t('common.createSuccess'))
    } else {
      await FaChangeApi.updateFaChange(data)
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
