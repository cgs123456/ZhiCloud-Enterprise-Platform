<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="alertType" prop="alertType">
        <el-input v-model="formData.alertType" placeholder="请输入alertType" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="batchNo" prop="batchNo">
        <el-input v-model="formData.batchNo" placeholder="请输入batchNo" />
      </el-form-item>
      <el-form-item label="currentQuantity" prop="currentQuantity">
        <el-input-number v-model="formData.currentQuantity" placeholder="请输入currentQuantity" />
      </el-form-item>
      <el-form-item label="thresholdValue" prop="thresholdValue">
        <el-input-number v-model="formData.thresholdValue" placeholder="请输入thresholdValue" />
      </el-form-item>
      <el-form-item label="alertTime" prop="alertTime">
        <el-input-number v-model="formData.alertTime" placeholder="请输入alertTime" />
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
import { InventoryAlertApi } from '@/api/wms/inventory-alert'

defineOptions({ name: 'InventoryAlertForm' })

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
      formData.value = await InventoryAlertApi.getInventoryAlert(id)
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
      await InventoryAlertApi.createInventoryAlert(data)
      message.success(t('common.createSuccess'))
    } else {
      await InventoryAlertApi.updateInventoryAlert(data)
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
