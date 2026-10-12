<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="costPeriod" prop="costPeriod">
        <el-input v-model="formData.costPeriod" placeholder="请输入costPeriod" />
      </el-form-item>
      <el-form-item label="costItemId" prop="costItemId">
        <el-input-number v-model="formData.costItemId" placeholder="请输入costItemId" />
      </el-form-item>
      <el-form-item label="standardCost" prop="standardCost">
        <el-input-number v-model="formData.standardCost" placeholder="请输入standardCost" />
      </el-form-item>
      <el-form-item label="actualCost" prop="actualCost">
        <el-input-number v-model="formData.actualCost" placeholder="请输入actualCost" />
      </el-form-item>
      <el-form-item label="varianceAmount" prop="varianceAmount">
        <el-input-number v-model="formData.varianceAmount" placeholder="请输入varianceAmount" />
      </el-form-item>
      <el-form-item label="varianceRate" prop="varianceRate">
        <el-input-number v-model="formData.varianceRate" placeholder="请输入varianceRate" />
      </el-form-item>
      <el-form-item label="varianceType" prop="varianceType">
        <el-input-number v-model="formData.varianceType" placeholder="请输入varianceType" />
      </el-form-item>
      <el-form-item label="analysisRemark" prop="analysisRemark">
        <el-input v-model="formData.analysisRemark" placeholder="请输入analysisRemark" />
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
import { CostVarianceApi } from '@/api/erp/cost-variance'

defineOptions({ name: 'CostVarianceForm' })

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
      formData.value = await CostVarianceApi.getCostVariance(id)
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
      await CostVarianceApi.createCostVariance(data)
      message.success(t('common.createSuccess'))
    } else {
      await CostVarianceApi.updateCostVariance(data)
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
