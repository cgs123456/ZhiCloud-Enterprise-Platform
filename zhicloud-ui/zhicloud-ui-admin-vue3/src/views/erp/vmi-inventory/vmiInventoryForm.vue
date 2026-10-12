<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="supplierId" prop="supplierId">
        <el-input-number v-model="formData.supplierId" placeholder="请输入supplierId" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入productName" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="availableQuantity" prop="availableQuantity">
        <el-input-number v-model="formData.availableQuantity" placeholder="请输入availableQuantity" />
      </el-form-item>
      <el-form-item label="lockedQuantity" prop="lockedQuantity">
        <el-input-number v-model="formData.lockedQuantity" placeholder="请输入lockedQuantity" />
      </el-form-item>
      <el-form-item label="minQuantity" prop="minQuantity">
        <el-input-number v-model="formData.minQuantity" placeholder="请输入minQuantity" />
      </el-form-item>
      <el-form-item label="maxQuantity" prop="maxQuantity">
        <el-input-number v-model="formData.maxQuantity" placeholder="请输入maxQuantity" />
      </el-form-item>
      <el-form-item label="replenishmentPoint" prop="replenishmentPoint">
        <el-input-number v-model="formData.replenishmentPoint" placeholder="请输入replenishmentPoint" />
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
import { VmiInventoryApi } from '@/api/erp/vmi-inventory'

defineOptions({ name: 'VmiInventoryForm' })

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
      formData.value = await VmiInventoryApi.getVmiInventory(id)
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
      await VmiInventoryApi.createVmiInventory(data)
      message.success(t('common.createSuccess'))
    } else {
      await VmiInventoryApi.updateVmiInventory(data)
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
