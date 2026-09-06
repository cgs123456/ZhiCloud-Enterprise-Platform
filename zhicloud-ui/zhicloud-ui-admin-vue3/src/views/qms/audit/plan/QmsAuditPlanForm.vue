<!-- 审核计划 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="formData.planNo" class="!w-1/1" placeholder="请输入计划编号" />
      </el-form-item>
      <el-form-item label="审核类型" prop="auditType">
        <el-input-number v-model="formData.auditType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核类型" />
      </el-form-item>
      <el-form-item label="审核标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入审核标题" />
      </el-form-item>
      <el-form-item label="审核依据" prop="auditStandard">
        <el-input v-model="formData.auditStandard" class="!w-1/1" placeholder="请输入审核依据" />
      </el-form-item>
      <el-form-item label="审核范围" prop="auditScope">
        <el-input v-model="formData.auditScope" class="!w-1/1" placeholder="请输入审核范围" />
      </el-form-item>
      <el-form-item label="审核目的" prop="auditPurpose">
        <el-input v-model="formData.auditPurpose" class="!w-1/1" placeholder="请输入审核目的" />
      </el-form-item>
      <el-form-item label="主审 ID" prop="leadAuditorId">
        <el-input-number v-model="formData.leadAuditorId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入主审 ID" />
      </el-form-item>
      <el-form-item label="审核开始日期" prop="auditStartDate">
        <el-date-picker v-model="formData.auditStartDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择审核开始日期" />
      </el-form-item>
      <el-form-item label="审核结束日期" prop="auditEndDate">
        <el-date-picker v-model="formData.auditEndDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择审核结束日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
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
import { QmsAuditPlanApi } from '@/api/qms/audit/plan'

defineOptions({ name: 'QmsAuditPlanForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  auditType: [required],

  title: [required],

  auditStandard: [required],

  auditScope: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsAuditPlanApi.getQmsAuditPlan(id)
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
      await QmsAuditPlanApi.createQmsAuditPlan(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsAuditPlanApi.updateQmsAuditPlan(formData.value)
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
