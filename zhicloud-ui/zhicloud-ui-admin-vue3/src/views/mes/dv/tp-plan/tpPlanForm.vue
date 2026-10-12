<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="设备" prop="equipmentId">
        <el-input-number v-model="formData.equipmentId" placeholder="请输入设备" />
      </el-form-item>
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="formData.planNo" placeholder="请输入计划编号" />
      </el-form-item>
      <el-form-item label="计划类型" prop="planType">
        <el-input-number v-model="formData.planType" placeholder="请输入计划类型" />
      </el-form-item>
      <el-form-item label="周期类型" prop="cycleType">
        <el-input-number v-model="formData.cycleType" placeholder="请输入周期类型" />
      </el-form-item>
      <el-form-item label="周期值" prop="cycleValue">
        <el-input-number v-model="formData.cycleValue" placeholder="请输入周期值" />
      </el-form-item>
      <el-form-item label="下次执行日期" prop="nextExecuteDate">
        <el-date-picker v-model="formData.nextExecuteDate" placeholder="请输入下次执行日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { TpPlanApi } from '@/api/mes/dv/tp-plan'

defineOptions({ name: 'TpPlanForm' })

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
      formData.value = await TpPlanApi.getTpPlan(id)
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
      await TpPlanApi.createTpPlan(data)
      message.success(t('common.createSuccess'))
    } else {
      await TpPlanApi.updateTpPlan(data)
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
