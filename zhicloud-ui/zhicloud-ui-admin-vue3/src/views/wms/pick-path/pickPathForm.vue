<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="orderId" prop="orderId">
        <el-input-number v-model="formData.orderId" placeholder="请输入orderId" />
      </el-form-item>
      <el-form-item label="orderNo" prop="orderNo">
        <el-input v-model="formData.orderNo" placeholder="请输入orderNo" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="warehouseName" prop="warehouseName">
        <el-input v-model="formData.warehouseName" placeholder="请输入warehouseName" />
      </el-form-item>
      <el-form-item label="strategy" prop="strategy">
        <el-input v-model="formData.strategy" placeholder="请输入strategy" />
      </el-form-item>
      <el-form-item label="locationCount" prop="locationCount">
        <el-input-number v-model="formData.locationCount" placeholder="请输入locationCount" />
      </el-form-item>
      <el-form-item label="totalQuantity" prop="totalQuantity">
        <el-input-number v-model="formData.totalQuantity" placeholder="请输入totalQuantity" />
      </el-form-item>
      <el-form-item label="estimatedDistance" prop="estimatedDistance">
        <el-input-number v-model="formData.estimatedDistance" placeholder="请输入estimatedDistance" />
      </el-form-item>
      <el-form-item label="locations" prop="locations">
        <el-input v-model="formData.locations" placeholder="请输入locations" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { PickPathApi } from '@/api/wms/pick-path'

defineOptions({ name: 'PickPathForm' })

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
      formData.value = await PickPathApi.getPickPath(id)
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
      await PickPathApi.createPickPath(data)
      message.success(t('common.createSuccess'))
    } else {
      await PickPathApi.updatePickPath(data)
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
