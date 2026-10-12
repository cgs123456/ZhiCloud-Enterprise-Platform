<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="abcClassification" prop="abcClassification">
        <el-input v-model="formData.abcClassification" placeholder="请输入abcClassification" />
      </el-form-item>
      <el-form-item label="cycleDays" prop="cycleDays">
        <el-input-number v-model="formData.cycleDays" placeholder="请输入cycleDays" />
      </el-form-item>
      <el-form-item label="nextCheckDate" prop="nextCheckDate">
        <el-input-number v-model="formData.nextCheckDate" placeholder="请输入nextCheckDate" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input-number v-model="formData.enabled" placeholder="请输入是否启用" />
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
import { CheckCyclePlanApi } from '@/api/wms/check-cycle-plan'

defineOptions({ name: 'CheckCyclePlanForm' })

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
      formData.value = await CheckCyclePlanApi.getCheckCyclePlan(id)
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
      await CheckCyclePlanApi.createCheckCyclePlan(data)
      message.success(t('common.createSuccess'))
    } else {
      await CheckCyclePlanApi.updateCheckCyclePlan(data)
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
