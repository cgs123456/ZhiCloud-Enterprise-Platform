<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="profitCenterId" prop="profitCenterId">
        <el-input-number v-model="formData.profitCenterId" placeholder="请输入profitCenterId" />
      </el-form-item>
      <el-form-item label="periodId" prop="periodId">
        <el-input-number v-model="formData.periodId" placeholder="请输入periodId" />
      </el-form-item>
      <el-form-item label="revenue" prop="revenue">
        <el-input-number v-model="formData.revenue" placeholder="请输入revenue" />
      </el-form-item>
      <el-form-item label="cost" prop="cost">
        <el-input-number v-model="formData.cost" placeholder="请输入cost" />
      </el-form-item>
      <el-form-item label="profit" prop="profit">
        <el-input-number v-model="formData.profit" placeholder="请输入profit" />
      </el-form-item>
      <el-form-item label="profitMargin" prop="profitMargin">
        <el-input-number v-model="formData.profitMargin" placeholder="请输入profitMargin" />
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
import { ProfitabilityAnalysisApi } from '@/api/erp/profitability-analysis'

defineOptions({ name: 'ProfitabilityAnalysisForm' })

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
      formData.value = await ProfitabilityAnalysisApi.getProfitabilityAnalysis(id)
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
      await ProfitabilityAnalysisApi.createProfitabilityAnalysis(data)
      message.success(t('common.createSuccess'))
    } else {
      await ProfitabilityAnalysisApi.updateProfitabilityAnalysis(data)
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
