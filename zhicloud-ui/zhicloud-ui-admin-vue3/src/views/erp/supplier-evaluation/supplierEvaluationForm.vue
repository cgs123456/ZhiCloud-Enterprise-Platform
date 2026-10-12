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
      <el-form-item label="evaluationPeriod" prop="evaluationPeriod">
        <el-input v-model="formData.evaluationPeriod" placeholder="请输入evaluationPeriod" />
      </el-form-item>
      <el-form-item label="qualityScore" prop="qualityScore">
        <el-input-number v-model="formData.qualityScore" placeholder="请输入qualityScore" />
      </el-form-item>
      <el-form-item label="deliveryScore" prop="deliveryScore">
        <el-input-number v-model="formData.deliveryScore" placeholder="请输入deliveryScore" />
      </el-form-item>
      <el-form-item label="priceScore" prop="priceScore">
        <el-input-number v-model="formData.priceScore" placeholder="请输入priceScore" />
      </el-form-item>
      <el-form-item label="serviceScore" prop="serviceScore">
        <el-input-number v-model="formData.serviceScore" placeholder="请输入serviceScore" />
      </el-form-item>
      <el-form-item label="totalScore" prop="totalScore">
        <el-input-number v-model="formData.totalScore" placeholder="请输入totalScore" />
      </el-form-item>
      <el-form-item label="grade" prop="grade">
        <el-input v-model="formData.grade" placeholder="请输入grade" />
      </el-form-item>
      <el-form-item label="evaluator" prop="evaluator">
        <el-input v-model="formData.evaluator" placeholder="请输入evaluator" />
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
import { SupplierEvaluationApi } from '@/api/erp/supplier-evaluation'

defineOptions({ name: 'SupplierEvaluationForm' })

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
      formData.value = await SupplierEvaluationApi.getSupplierEvaluation(id)
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
      await SupplierEvaluationApi.createSupplierEvaluation(data)
      message.success(t('common.createSuccess'))
    } else {
      await SupplierEvaluationApi.updateSupplierEvaluation(data)
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
