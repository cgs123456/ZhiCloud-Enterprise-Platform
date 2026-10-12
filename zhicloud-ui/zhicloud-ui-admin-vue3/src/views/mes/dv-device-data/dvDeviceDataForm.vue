<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="设备" prop="machineryId">
        <el-input-number v-model="formData.machineryId" placeholder="请输入设备" />
      </el-form-item>
      <el-form-item label="SCADA配置" prop="scadaConfigId">
        <el-input-number v-model="formData.scadaConfigId" placeholder="请输入SCADA配置" />
      </el-form-item>
      <el-form-item label="属性名" prop="propertyName">
        <el-input v-model="formData.propertyName" placeholder="请输入属性名" />
      </el-form-item>
      <el-form-item label="属性值" prop="propertyValue">
        <el-input v-model="formData.propertyValue" placeholder="请输入属性值" />
      </el-form-item>
      <el-form-item label="数据类型" prop="dataType">
        <el-input-number v-model="formData.dataType" placeholder="请输入数据类型" />
      </el-form-item>
      <el-form-item label="采集时间" prop="collectTime">
        <el-date-picker v-model="formData.collectTime" placeholder="请输入采集时间" />
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
import { DvDeviceDataApi } from '@/api/mes/dv-device-data'

defineOptions({ name: 'DvDeviceDataForm' })

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
      formData.value = await DvDeviceDataApi.getDvDeviceData(id)
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
      await DvDeviceDataApi.createDvDeviceData(data)
      message.success(t('common.createSuccess'))
    } else {
      await DvDeviceDataApi.updateDvDeviceData(data)
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
