<!-- HR 面试 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="简历 ID" prop="resumeId">
        <el-input-number v-model="formData.resumeId" class="!w-1/1" :controls="false" placeholder="请输入简历 ID" />
      </el-form-item>
      <el-form-item label="面试轮次" prop="interviewRound">
        <el-input-number v-model="formData.interviewRound" class="!w-1/1" :controls="false" placeholder="请输入面试轮次" />
      </el-form-item>
      <el-form-item label="面试官 ID" prop="interviewerId">
        <el-input-number v-model="formData.interviewerId" class="!w-1/1" :controls="false" placeholder="请输入面试官 ID" />
      </el-form-item>
      <el-form-item label="面试时间" prop="interviewTime">
        <el-date-picker v-model="formData.interviewTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择面试时间" />
      </el-form-item>
      <el-form-item label="面试类型" prop="interviewType">
        <el-input-number v-model="formData.interviewType" class="!w-1/1" :controls="false" placeholder="请输入面试类型" />
      </el-form-item>
      <el-form-item label="结果" prop="result">
        <el-input-number v-model="formData.result" class="!w-1/1" :controls="false" placeholder="请输入结果" />
      </el-form-item>
      <el-form-item label="面试评价" prop="comment">
        <el-input v-model="formData.comment" class="!w-1/1" placeholder="请输入面试评价" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrInterviewApi } from '@/api/hr/interview'

defineOptions({ name: 'HrInterviewForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  comment: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await HrInterviewApi.getHrInterview(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await HrInterviewApi.createHrInterview(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrInterviewApi.updateHrInterview(data)
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
