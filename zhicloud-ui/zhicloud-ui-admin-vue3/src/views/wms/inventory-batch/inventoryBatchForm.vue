<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="inventoryId" prop="inventoryId">
        <el-input-number v-model="formData.inventoryId" placeholder="请输入inventoryId" />
      </el-form-item>
      <el-form-item label="batchNo" prop="batchNo">
        <el-input v-model="formData.batchNo" placeholder="请输入batchNo" />
      </el-form-item>
      <el-form-item label="productionDate" prop="productionDate">
        <el-input-number v-model="formData.productionDate" placeholder="请输入productionDate" />
      </el-form-item>
      <el-form-item label="失效日期" prop="expiryDate">
        <el-input-number v-model="formData.expiryDate" placeholder="请输入失效日期" />
      </el-form-item>
      <el-form-item label="shelfLifeDays" prop="shelfLifeDays">
        <el-input-number v-model="formData.shelfLifeDays" placeholder="请输入shelfLifeDays" />
      </el-form-item>
      <el-form-item label="supplierBatchNo" prop="supplierBatchNo">
        <el-input v-model="formData.supplierBatchNo" placeholder="请输入supplierBatchNo" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="lockedQuantity" prop="lockedQuantity">
        <el-input-number v-model="formData.lockedQuantity" placeholder="请输入lockedQuantity" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input v-model="formData.status" placeholder="请输入状态" />
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
import { InventoryBatchApi } from '@/api/wms/inventory-batch'

defineOptions({ name: 'InventoryBatchForm' })

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
      formData.value = await InventoryBatchApi.getInventoryBatch(id)
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
      await InventoryBatchApi.createInventoryBatch(data)
      message.success(t('common.createSuccess'))
    } else {
      await InventoryBatchApi.updateInventoryBatch(data)
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
