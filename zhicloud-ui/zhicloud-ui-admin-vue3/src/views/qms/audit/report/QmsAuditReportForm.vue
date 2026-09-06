<!-- 审核报告 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="审核计划 ID" prop="planId">
        <el-input-number v-model="formData.planId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核计划 ID" />
      </el-form-item>
      <el-form-item label="报告编号" prop="reportNo">
        <el-input v-model="formData.reportNo" class="!w-1/1" placeholder="请输入报告编号" />
      </el-form-item>
      <el-form-item label="审核总结" prop="auditSummary">
        <el-input v-model="formData.auditSummary" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入审核总结" />
      </el-form-item>
      <el-form-item label="审核结论" prop="conclusion">
        <el-input-number v-model="formData.conclusion" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核结论" />
      </el-form-item>
      <el-form-item label="发现的不符合项数" prop="issueCount">
        <el-input-number v-model="formData.issueCount" class="!w-1/1" :min="0" :controls="false" placeholder="请输入发现的不符合项数" />
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
import { QmsAuditReportApi } from '@/api/qms/audit/report'

defineOptions({ name: 'QmsAuditReportForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  reportNo: [required],

  auditSummary: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsAuditReportApi.getQmsAuditReport(id)
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
      await QmsAuditReportApi.createQmsAuditReport(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsAuditReportApi.updateQmsAuditReport(formData.value)
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
