<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="workOrderId" prop="workOrderId">
        <el-input-number v-model="formData.workOrderId" placeholder="请输入workOrderId" />
      </el-form-item>
      <el-form-item label="workOrderCode" prop="workOrderCode">
        <el-input v-model="formData.workOrderCode" placeholder="请输入workOrderCode" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="costPeriod" prop="costPeriod">
        <el-input v-model="formData.costPeriod" placeholder="请输入costPeriod" />
      </el-form-item>
      <el-form-item label="materialCost" prop="materialCost">
        <el-input-number v-model="formData.materialCost" placeholder="请输入materialCost" />
      </el-form-item>
      <el-form-item label="laborCost" prop="laborCost">
        <el-input-number v-model="formData.laborCost" placeholder="请输入laborCost" />
      </el-form-item>
      <el-form-item label="overheadCost" prop="overheadCost">
        <el-input-number v-model="formData.overheadCost" placeholder="请输入overheadCost" />
      </el-form-item>
      <el-form-item label="outsourcingCost" prop="outsourcingCost">
        <el-input-number v-model="formData.outsourcingCost" placeholder="请输入outsourcingCost" />
      </el-form-item>
      <el-form-item label="totalCost" prop="totalCost">
        <el-input-number v-model="formData.totalCost" placeholder="请输入totalCost" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="unitCost" prop="unitCost">
        <el-input-number v-model="formData.unitCost" placeholder="请输入unitCost" />
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
import { WorkOrderCostApi } from '@/api/erp/work-order-cost'

defineOptions({ name: 'WorkOrderCostForm' })

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
      formData.value = await WorkOrderCostApi.getWorkOrderCost(id)
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
      await WorkOrderCostApi.createWorkOrderCost(data)
      message.success(t('common.createSuccess'))
    } else {
      await WorkOrderCostApi.updateWorkOrderCost(data)
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
