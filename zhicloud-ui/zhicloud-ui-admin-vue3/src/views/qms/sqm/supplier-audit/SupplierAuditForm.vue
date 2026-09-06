<!-- 供应商审核 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="审核编号" prop="auditNo">
        <el-input v-model="formData.auditNo" class="!w-1/1" placeholder="请输入审核编号" />
      </el-form-item>
      <el-form-item label="审核名称" prop="auditName">
        <el-input v-model="formData.auditName" class="!w-1/1" placeholder="请输入审核名称" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="formData.supplierId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="formData.supplierName" class="!w-1/1" placeholder="请输入供应商名称" />
      </el-form-item>
      <el-form-item label="审核类型" prop="auditType">
        <el-input-number v-model="formData.auditType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核类型" />
      </el-form-item>
      <el-form-item label="计划日期" prop="plannedDate">
        <el-date-picker v-model="formData.plannedDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择计划日期" />
      </el-form-item>
      <el-form-item label="实际日期" prop="actualDate">
        <el-date-picker v-model="formData.actualDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择实际日期" />
      </el-form-item>
      <el-form-item label="审核员" prop="auditor">
        <el-input v-model="formData.auditor" class="!w-1/1" placeholder="请输入审核员" />
      </el-form-item>
      <el-form-item label="审核结论" prop="conclusion">
        <el-input-number v-model="formData.conclusion" class="!w-1/1" :min="0" :controls="false" placeholder="请输入审核结论" />
      </el-form-item>
      <el-form-item label="审核报告" prop="auditReport">
        <el-input v-model="formData.auditReport" class="!w-1/1" placeholder="请输入审核报告" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
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
import { SupplierAuditApi } from '@/api/qms/sqm/supplier-audit'

defineOptions({ name: 'SupplierAuditForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  auditName: [required],

  supplierId: [required],

  supplierName: [required],

  auditType: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await SupplierAuditApi.getSupplierAudit(id)
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
      await SupplierAuditApi.createSupplierAudit(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await SupplierAuditApi.updateSupplierAudit(formData.value)
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
