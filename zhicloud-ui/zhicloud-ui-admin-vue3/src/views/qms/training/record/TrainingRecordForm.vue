<!-- 培训记录 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="记录编号" prop="recordNo">
        <el-input v-model="formData.recordNo" class="!w-1/1" placeholder="请输入记录编号" />
      </el-form-item>
      <el-form-item label="培训计划 ID" prop="planId">
        <el-input-number v-model="formData.planId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入培训计划 ID" />
      </el-form-item>
      <el-form-item label="参训人员 ID" prop="traineeId">
        <el-input-number v-model="formData.traineeId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入参训人员 ID" />
      </el-form-item>
      <el-form-item label="参训人员姓名" prop="traineeName">
        <el-input v-model="formData.traineeName" class="!w-1/1" placeholder="请输入参训人员姓名" />
      </el-form-item>
      <el-form-item label="成绩" prop="score">
        <el-input-number v-model="formData.score" class="!w-1/1" :min="0" :controls="false" placeholder="请输入成绩" />
      </el-form-item>
      <el-form-item label="是否通过（0 否 1 是）" prop="passed">
        <el-input-number v-model="formData.passed" class="!w-1/1" :min="0" :controls="false" placeholder="请输入是否通过（0 否 1 是）" />
      </el-form-item>
      <el-form-item label="证书编号" prop="certificateNo">
        <el-input v-model="formData.certificateNo" class="!w-1/1" placeholder="请输入证书编号" />
      </el-form-item>
      <el-form-item label="证书到期日" prop="certificateExpireDate">
        <el-date-picker v-model="formData.certificateExpireDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择证书到期日" />
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
import { TrainingRecordApi } from '@/api/qms/training/record'

defineOptions({ name: 'TrainingRecordForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  planId: [required],

  traineeId: [required],

  traineeName: [required],

  score: [required],

  passed: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TrainingRecordApi.getTrainingRecord(id)
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
      await TrainingRecordApi.createTrainingRecord(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await TrainingRecordApi.updateTrainingRecord(formData.value)
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
