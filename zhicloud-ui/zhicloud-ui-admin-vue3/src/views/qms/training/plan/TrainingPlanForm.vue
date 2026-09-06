<!-- 培训计划 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="formData.planNo" class="!w-1/1" placeholder="请输入计划编号" />
      </el-form-item>
      <el-form-item label="计划名称" prop="planName">
        <el-input v-model="formData.planName" class="!w-1/1" placeholder="请输入计划名称" />
      </el-form-item>
      <el-form-item label="年度" prop="year">
        <el-input-number v-model="formData.year" class="!w-1/1" :min="0" :controls="false" placeholder="请输入年度" />
      </el-form-item>
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="formData.courseName" class="!w-1/1" placeholder="请输入课程名称" />
      </el-form-item>
      <el-form-item label="讲师" prop="trainer">
        <el-input v-model="formData.trainer" class="!w-1/1" placeholder="请输入讲师" />
      </el-form-item>
      <el-form-item label="计划日期" prop="planDate">
        <el-date-picker v-model="formData.planDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择计划日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { TrainingPlanApi } from '@/api/qms/training/plan'

defineOptions({ name: 'TrainingPlanForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  planName: [required],

  year: [required],

  courseName: [required],

  trainer: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TrainingPlanApi.getTrainingPlan(id)
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
    if (formType.value === 'create') {
      await TrainingPlanApi.createTrainingPlan(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await TrainingPlanApi.updateTrainingPlan(formData.value)
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
