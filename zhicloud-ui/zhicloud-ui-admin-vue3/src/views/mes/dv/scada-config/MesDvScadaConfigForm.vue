<!-- MES SCADA 配置 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="MES 设备编号" prop="machineryId">
        <el-input-number v-model="formData.machineryId" class="!w-1/1" :controls="false" placeholder="请输入MES 设备编号" />
      </el-form-item>
      <el-form-item label="MES 设备名称" prop="machineryName">
        <el-input v-model="formData.machineryName" class="!w-1/1" placeholder="请输入MES 设备名称" />
      </el-form-item>
      <el-form-item label="IoT 平台设备 PK" prop="iotDevicePk">
        <el-input v-model="formData.iotDevicePk" class="!w-1/1" placeholder="请输入IoT 平台设备 PK" />
      </el-form-item>
      <el-form-item label="IoT 平台产品编号" prop="iotProductId">
        <el-input-number v-model="formData.iotProductId" class="!w-1/1" :controls="false" placeholder="请输入IoT 平台产品编号" />
      </el-form-item>
      <el-form-item label="SCADA 协议类型" prop="protocolType">
        <el-input v-model="formData.protocolType" class="!w-1/1" placeholder="请输入SCADA 协议类型" />
      </el-form-item>
      <el-form-item label="点位映射配置（JSON）" prop="pointConfig">
        <el-input v-model="formData.pointConfig" class="!w-1/1" placeholder="请输入点位映射配置（JSON）" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input-number v-model="formData.enabled" class="!w-1/1" :controls="false" placeholder="请输入是否启用" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { MesDvScadaConfigApi } from '@/api/mes/dv-scada-config'

defineOptions({ name: 'MesDvScadaConfigForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  machineryName: [required],
  iotDevicePk: [required],
  protocolType: [required],
  pointConfig: [required],
  remark: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await MesDvScadaConfigApi.getMesDvScadaConfig(id)
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
      await MesDvScadaConfigApi.createMesDvScadaConfig(data)
      message.success(t('common.createSuccess'))
    } else {
      await MesDvScadaConfigApi.updateMesDvScadaConfig(data)
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
