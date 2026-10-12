<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="forecastId" prop="forecastId">
        <el-input-number v-model="formData.forecastId" placeholder="请输入forecastId" />
      </el-form-item>
      <el-form-item label="exceptionType" prop="exceptionType">
        <el-input-number v-model="formData.exceptionType" placeholder="请输入exceptionType" />
      </el-form-item>
      <el-form-item label="exceptionDescription" prop="exceptionDescription">
        <el-input v-model="formData.exceptionDescription" placeholder="请输入exceptionDescription" />
      </el-form-item>
      <el-form-item label="handlingStatus" prop="handlingStatus">
        <el-input-number v-model="formData.handlingStatus" placeholder="请输入handlingStatus" />
      </el-form-item>
      <el-form-item label="handlerUserId" prop="handlerUserId">
        <el-input-number v-model="formData.handlerUserId" placeholder="请输入handlerUserId" />
      </el-form-item>
      <el-form-item label="handlingTime" prop="handlingTime">
        <el-input-number v-model="formData.handlingTime" placeholder="请输入handlingTime" />
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
import { CpfrExceptionApi } from '@/api/erp/cpfr-exception'

defineOptions({ name: 'CpfrExceptionForm' })

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
      formData.value = await CpfrExceptionApi.getCpfrException(id)
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
      await CpfrExceptionApi.createCpfrException(data)
      message.success(t('common.createSuccess'))
    } else {
      await CpfrExceptionApi.updateCpfrException(data)
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
