<!-- HR 请假申请 表单（发起请假，草稿） -->
<template>
  <Dialog v-model="dialogVisible" title="发起请假">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="formData.employeeId" class="!w-1/1" :controls="false" placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="假期类型 ID" prop="leaveTypeId">
        <el-input-number v-model="formData.leaveTypeId" class="!w-1/1" :controls="false" placeholder="请输入假期类型 ID" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker v-model="formData.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" placeholder="请选择开始时间" />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker v-model="formData.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" placeholder="请选择结束时间" />
      </el-form-item>
      <el-form-item label="时长(天)" prop="duration">
        <el-input-number v-model="formData.duration" class="!w-1/1" :min="0" :controls="false" placeholder="请输入时长" />
      </el-form-item>
      <el-form-item label="事由" prop="reason">
        <el-input v-model="formData.reason" class="!w-1/1" type="textarea" :rows="3" placeholder="请输入请假事由" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrLeaveRequestApi } from '@/api/hr/leave-request'

defineOptions({ name: 'HrLeaveRequestForm' })

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = ref<any>({})
const formRules = reactive({
  employeeId: [required],
  leaveTypeId: [required],
  startTime: [required],
  endTime: [required]
})

const emit = defineEmits(['success'])

const open = () => {
  dialogVisible.value = true
  formData.value = {}
}

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    await HrLeaveRequestApi.createLeaveRequest(formData.value)
    message.success('已创建请假申请（草稿）')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
