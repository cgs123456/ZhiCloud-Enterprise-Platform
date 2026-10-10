<!-- OA 会议室 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="会议主题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入会议主题" />
      </el-form-item>
      <el-form-item label="会议室 ID" prop="roomId">
        <el-input-number v-model="formData.roomId" class="!w-1/1" :controls="false" placeholder="请输入会议室 ID" />
      </el-form-item>
      <el-form-item label="组织人 ID" prop="organizerUserId">
        <el-input-number v-model="formData.organizerUserId" class="!w-1/1" :controls="false" placeholder="请输入组织人 ID" />
      </el-form-item>
      <el-form-item label="参会人（逗号分隔）" prop="attendeeUserIds">
        <el-input v-model="formData.attendeeUserIds" class="!w-1/1" placeholder="请输入参会人（逗号分隔）" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker v-model="formData.startTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择开始时间" />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker v-model="formData.endTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择结束时间" />
      </el-form-item>
      <el-form-item label="状态（10 待确认 20 已确认 30 已取消 40 已完成）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 待确认 20 已确认 30 已取消 40 已完成）" />
      </el-form-item>
      <el-form-item label="是否提醒" prop="reminderEnabled">
        <el-switch v-model="formData.reminderEnabled" />
      </el-form-item>
      <el-form-item label="提前提醒分钟" prop="reminderMinutes">
        <el-input-number v-model="formData.reminderMinutes" class="!w-1/1" :controls="false" placeholder="请输入提前提醒分钟" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { OaMeetingRoomApi } from '@/api/oa/meeting-room'

defineOptions({ name: 'OaMeetingRoomForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  title: [required],
  attendeeUserIds: [required],
  remark: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaMeetingRoomApi.getOaMeetingRoom(id)
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
      await OaMeetingRoomApi.createOaMeetingRoom(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaMeetingRoomApi.updateOaMeetingRoom(data)
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
