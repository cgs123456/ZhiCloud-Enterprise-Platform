<!-- OA 会议预约表单（含「冲突预检」按钮：只读 check-conflict，避免提交才撞冲突） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="会议主题" prop="title">
        <el-input v-model="formData.title" maxlength="256" placeholder="请输入会议主题" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="会议室" prop="roomId">
        <el-input-number
          v-model="formData.roomId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入会议室编号"
        />
      </el-form-item>
      <el-form-item label="组织人" prop="organizerUserId">
        <el-input-number
          v-model="formData.organizerUserId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入组织人 ID"
        />
      </el-form-item>
      <el-form-item label="参会人" prop="attendeeUserIds">
        <el-input v-model="formData.attendeeUserIds" maxlength="2000" placeholder="多个用户 ID 用逗号分隔，如 1,2,3" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="formData.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="请选择开始时间"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="formData.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="请选择结束时间"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" class="!w-1/1" placeholder="留空则默认为待确认">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="提醒" prop="reminderMinutes">
        <div class="flex items-center gap-8px w-1/1">
          <el-switch v-model="formData.reminderEnabled" />
          <el-input-number
            v-if="formData.reminderEnabled"
            v-model="formData.reminderMinutes"
            :controls="false"
            :min="0"
            class="!w-160px"
            placeholder="提前分钟"
          />
        </div>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="512" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item>
        <el-button plain type="warning" :loading="checkLoading" @click="handleCheckConflict">
          <Icon icon="ep:warn-triangle-filled" class="mr-5px" /> 冲突预检
        </el-button>
        <span class="ml-8px text-13px text-gray-500">提交时服务端会再次校验（悲观锁会议室），预检只做提前提示</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { OaMeetingReservationApi } from '@/api/oa/meeting-reservation'
import type { OaMeetingReservationVO } from '@/api/oa/meeting-reservation'

defineOptions({ name: 'OaMeetingReservationForm' })

const { t } = useI18n()
const message = useMessage()

const STATUS_OPTIONS = [
  { value: 10, label: '待确认' },
  { value: 20, label: '已确认' },
  { value: 30, label: '已取消' },
  { value: 40, label: '已完成' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<OaMeetingReservationVO>({})
const formRules = reactive<FormRules>({
  title: [{ required: true, message: '会议主题不能为空', trigger: 'blur' }],
  roomId: [{ required: true, message: '会议室不能为空', trigger: 'blur' }],
  organizerUserId: [{ required: true, message: '组织人不能为空', trigger: 'blur' }],
  startTime: [{ required: true, message: '开始时间不能为空', trigger: 'change' }],
  endTime: [{ required: true, message: '结束时间不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = { status: undefined, reminderEnabled: false, reminderMinutes: undefined }
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaMeetingReservationApi.getOaMeetingReservation(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 冲突预检（只读） */
const checkLoading = ref(false)
const handleCheckConflict = async () => {
  if (!formData.value.roomId || !formData.value.startTime || !formData.value.endTime) {
    message.warning('请先填写会议室与起止时间')
    return
  }
  checkLoading.value = true
  try {
    const conflict = await OaMeetingReservationApi.checkConflict({
      roomId: formData.value.roomId,
      startTime: formData.value.startTime!.replace('T', ' '),
      endTime: formData.value.endTime!.replace('T', ' '),
      excludeReservationId: formData.value.id
    })
    if (conflict) {
      message.error('该会议室在所选时段已有预约，请更换会议室或时间段')
    } else {
      message.success('时段可用，无冲突')
    }
  } finally {
    checkLoading.value = false
  }
}

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: OaMeetingReservationVO = {
      id: formData.value.id,
      title: formData.value.title,
      roomId: formData.value.roomId,
      organizerUserId: formData.value.organizerUserId,
      attendeeUserIds: formData.value.attendeeUserIds,
      startTime: formData.value.startTime,
      endTime: formData.value.endTime,
      status: formData.value.status,
      reminderEnabled: formData.value.reminderEnabled,
      reminderMinutes: formData.value.reminderEnabled ? formData.value.reminderMinutes : undefined,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await OaMeetingReservationApi.updateOaMeetingReservation(data)
      message.success(t('common.updateSuccess'))
    } else {
      await OaMeetingReservationApi.createOaMeetingReservation(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
