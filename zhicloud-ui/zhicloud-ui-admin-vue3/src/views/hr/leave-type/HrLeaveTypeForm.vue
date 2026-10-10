<!-- HR 假期类型 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="如 年假 / 事假" />
      </el-form-item>
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="如 ANNUAL_LEAVE" />
      </el-form-item>
      <el-form-item label="是否带薪" prop="isPaid">
        <el-input-number v-model="formData.isPaid" class="!w-1/1" :controls="false" placeholder="1 带薪 0 不带薪" />
      </el-form-item>
      <el-form-item label="是否扣薪" prop="deductSalary">
        <el-input-number v-model="formData.deductSalary" class="!w-1/1" :controls="false" placeholder="1 扣薪 0 不扣薪" />
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
import { HrLeaveTypeApi } from '@/api/hr/leave-type'

defineOptions({ name: 'HrLeaveTypeForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<any>({})
const formRules = reactive({ name: [required], code: [required] })

const emit = defineEmits(['success'])

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await HrLeaveTypeApi.getLeaveType(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await HrLeaveTypeApi.createLeaveType(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await HrLeaveTypeApi.updateLeaveType(formData.value)
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
