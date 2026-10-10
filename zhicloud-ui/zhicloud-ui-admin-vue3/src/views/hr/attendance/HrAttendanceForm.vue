<!-- HR 考勤 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="formData.employeeId" class="!w-1/1" :controls="false" placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="考勤月份" prop="month">
        <el-input v-model="formData.month" class="!w-1/1" placeholder="请输入考勤月份" />
      </el-form-item>
      <el-form-item label="应出勤天数" prop="totalDays">
        <el-input-number v-model="formData.totalDays" class="!w-1/1" :controls="false" placeholder="请输入应出勤天数" />
      </el-form-item>
      <el-form-item label="正常天数" prop="normalDays">
        <el-input-number v-model="formData.normalDays" class="!w-1/1" :controls="false" placeholder="请输入正常天数" />
      </el-form-item>
      <el-form-item label="迟到天数" prop="lateDays">
        <el-input-number v-model="formData.lateDays" class="!w-1/1" :controls="false" placeholder="请输入迟到天数" />
      </el-form-item>
      <el-form-item label="早退天数" prop="earlyLeaveDays">
        <el-input-number v-model="formData.earlyLeaveDays" class="!w-1/1" :controls="false" placeholder="请输入早退天数" />
      </el-form-item>
      <el-form-item label="缺勤天数" prop="absentDays">
        <el-input-number v-model="formData.absentDays" class="!w-1/1" :controls="false" placeholder="请输入缺勤天数" />
      </el-form-item>
      <el-form-item label="加班天数" prop="overtimeDays">
        <el-input-number v-model="formData.overtimeDays" class="!w-1/1" :controls="false" placeholder="请输入加班天数" />
      </el-form-item>
      <el-form-item label="加班总时长（小时）" prop="totalOvertimeHours">
        <el-input-number v-model="formData.totalOvertimeHours" class="!w-1/1" :controls="false" placeholder="请输入加班总时长（小时）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrAttendanceApi } from '@/api/hr/attendance'

defineOptions({ name: 'HrAttendanceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  month: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await HrAttendanceApi.getHrAttendance(id)
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
      await HrAttendanceApi.createHrAttendance(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrAttendanceApi.updateHrAttendance(data)
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
