<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="taskNo" prop="taskNo">
        <el-input v-model="formData.taskNo" placeholder="请输入taskNo" />
      </el-form-item>
      <el-form-item label="shipmentOrderId" prop="shipmentOrderId">
        <el-input-number v-model="formData.shipmentOrderId" placeholder="请输入shipmentOrderId" />
      </el-form-item>
      <el-form-item label="waveOrderId" prop="waveOrderId">
        <el-input-number v-model="formData.waveOrderId" placeholder="请输入waveOrderId" />
      </el-form-item>
      <el-form-item label="skuId" prop="skuId">
        <el-input-number v-model="formData.skuId" placeholder="请输入skuId" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入productName" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="pickedQuantity" prop="pickedQuantity">
        <el-input-number v-model="formData.pickedQuantity" placeholder="请输入pickedQuantity" />
      </el-form-item>
      <el-form-item label="locationId" prop="locationId">
        <el-input-number v-model="formData.locationId" placeholder="请输入locationId" />
      </el-form-item>
      <el-form-item label="pickSequence" prop="pickSequence">
        <el-input-number v-model="formData.pickSequence" placeholder="请输入pickSequence" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="pickerUserId" prop="pickerUserId">
        <el-input-number v-model="formData.pickerUserId" placeholder="请输入pickerUserId" />
      </el-form-item>
      <el-form-item label="pickTime" prop="pickTime">
        <el-input-number v-model="formData.pickTime" placeholder="请输入pickTime" />
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
import { PickTaskApi } from '@/api/wms/pick-task'

defineOptions({ name: 'PickTaskForm' })

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
      formData.value = await PickTaskApi.getPickTask(id)
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
      await PickTaskApi.createPickTask(data)
      message.success(t('common.createSuccess'))
    } else {
      await PickTaskApi.updatePickTask(data)
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
