<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="sn" prop="sn">
        <el-input v-model="formData.sn" placeholder="请输入sn" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="batchId" prop="batchId">
        <el-input-number v-model="formData.batchId" placeholder="请输入batchId" />
      </el-form-item>
      <el-form-item label="inventoryId" prop="inventoryId">
        <el-input-number v-model="formData.inventoryId" placeholder="请输入inventoryId" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="zoneId" prop="zoneId">
        <el-input-number v-model="formData.zoneId" placeholder="请输入zoneId" />
      </el-form-item>
      <el-form-item label="locationId" prop="locationId">
        <el-input-number v-model="formData.locationId" placeholder="请输入locationId" />
      </el-form-item>
      <el-form-item label="inboundOrderId" prop="inboundOrderId">
        <el-input-number v-model="formData.inboundOrderId" placeholder="请输入inboundOrderId" />
      </el-form-item>
      <el-form-item label="outboundOrderId" prop="outboundOrderId">
        <el-input-number v-model="formData.outboundOrderId" placeholder="请输入outboundOrderId" />
      </el-form-item>
      <el-form-item label="boundTime" prop="boundTime">
        <el-input-number v-model="formData.boundTime" placeholder="请输入boundTime" />
      </el-form-item>
      <el-form-item label="shippedTime" prop="shippedTime">
        <el-input-number v-model="formData.shippedTime" placeholder="请输入shippedTime" />
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
import { SnApi } from '@/api/wms/sn'

defineOptions({ name: 'SnForm' })

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
      formData.value = await SnApi.getSn(id)
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
      await SnApi.createSn(data)
      message.success(t('common.createSuccess'))
    } else {
      await SnApi.updateSn(data)
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
