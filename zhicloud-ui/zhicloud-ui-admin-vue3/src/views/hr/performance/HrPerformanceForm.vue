<!-- HR 绩效考核 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="排名" prop="rank">
        <el-input-number v-model="formData.rank" class="!w-1/1" :controls="false" placeholder="请输入排名" />
      </el-form-item>
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="formData.employeeId" class="!w-1/1" :controls="false" placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="考核周期" prop="period">
        <el-input v-model="formData.period" class="!w-1/1" placeholder="请输入考核周期" />
      </el-form-item>
      <el-form-item label="考核得分" prop="score">
        <el-input-number v-model="formData.score" class="!w-1/1" :controls="false" placeholder="请输入考核得分" />
      </el-form-item>
      <el-form-item label="考核等级" prop="grade">
        <el-input-number v-model="formData.grade" class="!w-1/1" :controls="false" placeholder="请输入考核等级" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrPerformanceApi } from '@/api/hr/performance'

defineOptions({ name: 'HrPerformanceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  period: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await HrPerformanceApi.getHrPerformance(id)
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
      await HrPerformanceApi.createHrPerformance(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrPerformanceApi.updateHrPerformance(data)
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
