<!-- 审核不符合项 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="审核报告 ID" prop="reportId">
        <el-input-number v-model="formData.reportId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核报告 ID" />
      </el-form-item>
      <el-form-item label="不符合项编号" prop="ncNo">
        <el-input v-model="formData.ncNo" class="!w-1/1" placeholder="请输入不符合项编号" />
      </el-form-item>
      <el-form-item label="严重程度" prop="severity">
        <el-input-number v-model="formData.severity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入严重程度" />
      </el-form-item>
      <el-form-item label="不符合描述" prop="description">
        <el-input v-model="formData.description" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入不符合描述" />
      </el-form-item>
      <el-form-item label="不符合条款" prop="clause">
        <el-input v-model="formData.clause" class="!w-1/1" placeholder="请输入不符合条款" />
      </el-form-item>
      <el-form-item label="责任部门 ID" prop="responsibleDeptId">
        <el-input-number v-model="formData.responsibleDeptId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入责任部门 ID" />
      </el-form-item>
      <el-form-item label="整改截止日期" prop="correctiveActionDeadline">
        <el-date-picker v-model="formData.correctiveActionDeadline" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择整改截止日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :min="0" :controls="false" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { QmsAuditNonconformityApi } from '@/api/qms/audit/nonconformity'

defineOptions({ name: 'QmsAuditNonconformityForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  ncNo: [required],

  severity: [required],

  description: [required],

  clause: [required],

  responsibleDeptId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsAuditNonconformityApi.getQmsAuditNonconformity(id)
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
      await QmsAuditNonconformityApi.createQmsAuditNonconformity(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsAuditNonconformityApi.updateQmsAuditNonconformity(formData.value)
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
