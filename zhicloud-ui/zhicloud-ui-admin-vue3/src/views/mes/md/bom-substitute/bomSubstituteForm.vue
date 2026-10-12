<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="bomId" prop="bomId">
        <el-input-number v-model="formData.bomId" placeholder="请输入bomId" />
      </el-form-item>
      <el-form-item label="bomDetailId" prop="bomDetailId">
        <el-input-number v-model="formData.bomDetailId" placeholder="请输入bomDetailId" />
      </el-form-item>
      <el-form-item label="substituteItemId" prop="substituteItemId">
        <el-input-number v-model="formData.substituteItemId" placeholder="请输入substituteItemId" />
      </el-form-item>
      <el-form-item label="substituteRatio" prop="substituteRatio">
        <el-input-number v-model="formData.substituteRatio" placeholder="请输入substituteRatio" />
      </el-form-item>
      <el-form-item label="priority" prop="priority">
        <el-input-number v-model="formData.priority" placeholder="请输入priority" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-input-number v-model="formData.effectiveDate" placeholder="请输入生效日期" />
      </el-form-item>
      <el-form-item label="失效日期" prop="expiryDate">
        <el-input-number v-model="formData.expiryDate" placeholder="请输入失效日期" />
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
import { BomSubstituteApi } from '@/api/mes/md/bom-substitute'

defineOptions({ name: 'BomSubstituteForm' })

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
      formData.value = await BomSubstituteApi.getBomSubstitute(id)
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
      await BomSubstituteApi.createBomSubstitute(data)
      message.success(t('common.createSuccess'))
    } else {
      await BomSubstituteApi.updateBomSubstitute(data)
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
