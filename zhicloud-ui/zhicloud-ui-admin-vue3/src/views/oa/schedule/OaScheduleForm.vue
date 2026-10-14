<!-- OA 日程表单（不含 status 字段：状态只能通过完成/取消端点流转） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="用户编号" prop="userId">
        <el-input-number
          v-model="formData.userId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入日程所属用户编号"
        />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" maxlength="256" placeholder="请输入标题" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" type="textarea" :rows="2" maxlength="2000" placeholder="请输入描述" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="formData.type" class="!w-1/1" placeholder="请选择类型（留空为日程）">
          <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          placeholder="请选择结束时间（可空）"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="全天" prop="allDay">
        <el-switch v-model="formData.allDay" />
      </el-form-item>
      <el-form-item label="地点" prop="location">
        <el-input v-model="formData.location" maxlength="255" placeholder="请输入地点" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="提醒" prop="remindMinutes">
        <el-input-number
          v-model="formData.remindMinutes"
          :controls="false"
          :min="0"
          class="!w-1/1"
          placeholder="提前分钟数（可空）"
        />
      </el-form-item>
      <el-form-item label="重复" prop="repeatType">
        <el-select v-model="formData.repeatType" class="!w-1/1" placeholder="请选择重复方式（留空为不重复）">
          <el-option v-for="item in REPEAT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="512" placeholder="请输入备注" />
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
import { OaScheduleApi } from '@/api/oa/schedule'
import type { OaScheduleVO } from '@/api/oa/schedule'

defineOptions({ name: 'OaScheduleForm' })

const { t } = useI18n()
const message = useMessage()

const TYPE_OPTIONS = [
  { value: 10, label: '日程' },
  { value: 20, label: '任务' },
  { value: 30, label: '纪念日' },
  { value: 40, label: '会议' }
]

const REPEAT_OPTIONS = [
  { value: 0, label: '不重复' },
  { value: 10, label: '每天' },
  { value: 20, label: '每周' },
  { value: 30, label: '每月' },
  { value: 40, label: '每年' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<OaScheduleVO>({})
const formRules = reactive<FormRules>({
  userId: [{ required: true, message: '用户编号不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  startTime: [{ required: true, message: '开始时间不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = { allDay: false }
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaScheduleApi.getOaSchedule(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单（不含 status：服务端强制置 null） */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: OaScheduleVO = {
      id: formData.value.id,
      userId: formData.value.userId,
      title: formData.value.title,
      description: formData.value.description,
      type: formData.value.type,
      startTime: formData.value.startTime,
      endTime: formData.value.endTime,
      allDay: formData.value.allDay,
      location: formData.value.location,
      remindMinutes: formData.value.remindMinutes,
      repeatType: formData.value.repeatType,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await OaScheduleApi.updateOaSchedule(data)
      message.success(t('common.updateSuccess'))
    } else {
      await OaScheduleApi.createOaSchedule(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
