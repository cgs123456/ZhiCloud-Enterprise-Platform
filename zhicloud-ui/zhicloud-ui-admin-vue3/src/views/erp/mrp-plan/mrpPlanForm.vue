<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编号" prop="no">
        <el-input v-model="formData.no" placeholder="请输入编号" />
      </el-form-item>
      <el-form-item label="planName" prop="planName">
        <el-input v-model="formData.planName" placeholder="请输入planName" />
      </el-form-item>
      <el-form-item label="planDate" prop="planDate">
        <el-input-number v-model="formData.planDate" placeholder="请输入planDate" />
      </el-form-item>
      <el-form-item label="mpsPlanId" prop="mpsPlanId">
        <el-input-number v-model="formData.mpsPlanId" placeholder="请输入mpsPlanId" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="totalDemandCount" prop="totalDemandCount">
        <el-input-number v-model="formData.totalDemandCount" placeholder="请输入totalDemandCount" />
      </el-form-item>
      <el-form-item label="totalPurchaseCount" prop="totalPurchaseCount">
        <el-input-number v-model="formData.totalPurchaseCount" placeholder="请输入totalPurchaseCount" />
      </el-form-item>
      <el-form-item label="totalProduceCount" prop="totalProduceCount">
        <el-input-number v-model="formData.totalProduceCount" placeholder="请输入totalProduceCount" />
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
import { MrpPlanApi } from '@/api/erp/mrp-plan'

defineOptions({ name: 'MrpPlanForm' })

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
      formData.value = await MrpPlanApi.getMrpPlan(id)
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
      await MrpPlanApi.createMrpPlan(data)
      message.success(t('common.createSuccess'))
    } else {
      await MrpPlanApi.updateMrpPlan(data)
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
