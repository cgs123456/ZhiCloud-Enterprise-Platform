<!-- TMS 跟踪 表单（上报跟踪事件） -->
<template>
  <Dialog v-model="dialogVisible" title="上报跟踪事件">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="运单 ID" prop="shipmentId">
        <el-input-number v-model="formData.shipmentId" class="!w-1/1" :controls="false" placeholder="请输入运单 ID" />
      </el-form-item>
      <el-form-item label="事件类型" prop="eventType">
        <el-input-number v-model="formData.eventType" class="!w-1/1" :controls="false" placeholder="请输入事件类型" />
      </el-form-item>
      <el-form-item label="事件时间" prop="eventTime">
        <el-date-picker v-model="formData.eventTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" placeholder="请选择事件时间" />
      </el-form-item>
      <el-form-item label="位置" prop="location">
        <el-input v-model="formData.location" class="!w-1/1" placeholder="请输入位置描述" />
      </el-form-item>
      <el-form-item label="经度" prop="longitude">
        <el-input-number v-model="formData.longitude" class="!w-1/1" :controls="false" placeholder="请输入经度" />
      </el-form-item>
      <el-form-item label="纬度" prop="latitude">
        <el-input-number v-model="formData.latitude" class="!w-1/1" :controls="false" placeholder="请输入纬度" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { TmsTrackingApi } from '@/api/tms/tracking'

defineOptions({ name: 'TmsTrackingForm' })

const props = defineProps<{ shipmentId?: number }>()
const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = ref<any>({})
const formRules = reactive({
  shipmentId: [required],
  eventType: [required],
  eventTime: [required]
})

const emit = defineEmits(['success'])

const open = (shipmentId?: number) => {
  dialogVisible.value = true
  formData.value = { shipmentId: shipmentId ?? props.shipmentId }
}

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    await TmsTrackingApi.reportTracking(formData.value)
    message.success('上报成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
