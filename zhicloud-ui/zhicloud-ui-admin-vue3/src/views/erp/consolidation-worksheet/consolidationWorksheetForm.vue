<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="consolidationPeriod" prop="consolidationPeriod">
        <el-input v-model="formData.consolidationPeriod" placeholder="请输入consolidationPeriod" />
      </el-form-item>
      <el-form-item label="parentCompanyId" prop="parentCompanyId">
        <el-input-number v-model="formData.parentCompanyId" placeholder="请输入parentCompanyId" />
      </el-form-item>
      <el-form-item label="subsidiaryCompanyId" prop="subsidiaryCompanyId">
        <el-input-number v-model="formData.subsidiaryCompanyId" placeholder="请输入subsidiaryCompanyId" />
      </el-form-item>
      <el-form-item label="eliminationType" prop="eliminationType">
        <el-input-number v-model="formData.eliminationType" placeholder="请输入eliminationType" />
      </el-form-item>
      <el-form-item label="eliminationAmount" prop="eliminationAmount">
        <el-input-number v-model="formData.eliminationAmount" placeholder="请输入eliminationAmount" />
      </el-form-item>
      <el-form-item label="description" prop="description">
        <el-input v-model="formData.description" placeholder="请输入description" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" placeholder="请输入排序" />
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
import { ConsolidationWorksheetApi } from '@/api/erp/consolidation-worksheet'

defineOptions({ name: 'ConsolidationWorksheetForm' })

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
      formData.value = await ConsolidationWorksheetApi.getConsolidationWorksheet(id)
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
      await ConsolidationWorksheetApi.createConsolidationWorksheet(data)
      message.success(t('common.createSuccess'))
    } else {
      await ConsolidationWorksheetApi.updateConsolidationWorksheet(data)
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
