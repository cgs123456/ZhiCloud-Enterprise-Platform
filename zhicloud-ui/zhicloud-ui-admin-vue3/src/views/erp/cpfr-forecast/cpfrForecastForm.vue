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
      <el-form-item label="partnerType" prop="partnerType">
        <el-input-number v-model="formData.partnerType" placeholder="请输入partnerType" />
      </el-form-item>
      <el-form-item label="partnerId" prop="partnerId">
        <el-input-number v-model="formData.partnerId" placeholder="请输入partnerId" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入productName" />
      </el-form-item>
      <el-form-item label="forecastPeriod" prop="forecastPeriod">
        <el-input v-model="formData.forecastPeriod" placeholder="请输入forecastPeriod" />
      </el-form-item>
      <el-form-item label="forecastQuantity" prop="forecastQuantity">
        <el-input-number v-model="formData.forecastQuantity" placeholder="请输入forecastQuantity" />
      </el-form-item>
      <el-form-item label="actualQuantity" prop="actualQuantity">
        <el-input-number v-model="formData.actualQuantity" placeholder="请输入actualQuantity" />
      </el-form-item>
      <el-form-item label="deviationRate" prop="deviationRate">
        <el-input-number v-model="formData.deviationRate" placeholder="请输入deviationRate" />
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
import { CpfrForecastApi } from '@/api/erp/cpfr-forecast'

defineOptions({ name: 'CpfrForecastForm' })

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
      formData.value = await CpfrForecastApi.getCpfrForecast(id)
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
      await CpfrForecastApi.createCpfrForecast(data)
      message.success(t('common.createSuccess'))
    } else {
      await CpfrForecastApi.updateCpfrForecast(data)
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
