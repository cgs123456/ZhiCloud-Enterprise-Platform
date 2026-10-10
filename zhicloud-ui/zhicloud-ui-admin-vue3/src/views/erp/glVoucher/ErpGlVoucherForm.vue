<!-- ERP 会计凭证 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="凭证字号" prop="voucherNo">
        <el-input v-model="formData.voucherNo" class="!w-1/1" placeholder="请输入凭证字号" />
      </el-form-item>
      <el-form-item label="凭证日期" prop="voucherDate">
        <el-date-picker v-model="formData.voucherDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择凭证日期" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="formData.periodId" class="!w-1/1" :controls="false" placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="会计期间编码" prop="periodCode">
        <el-input v-model="formData.periodCode" class="!w-1/1" placeholder="请输入会计期间编码" />
      </el-form-item>
      <el-form-item label="凭证类型" prop="voucherType">
        <el-input-number v-model="formData.voucherType" class="!w-1/1" :controls="false" placeholder="请输入凭证类型" />
      </el-form-item>
      <el-form-item label="凭证类型名称" prop="voucherTypeName">
        <el-input v-model="formData.voucherTypeName" class="!w-1/1" placeholder="请输入凭证类型名称" />
      </el-form-item>
      <el-form-item label="附件张数" prop="attachmentCount">
        <el-input-number v-model="formData.attachmentCount" class="!w-1/1" :controls="false" placeholder="请输入附件张数" />
      </el-form-item>
      <el-form-item label="凭证摘要" prop="summary">
        <el-input v-model="formData.summary" class="!w-1/1" placeholder="请输入凭证摘要" />
      </el-form-item>
      <el-form-item label="借方合计" prop="debitTotal">
        <el-input-number v-model="formData.debitTotal" class="!w-1/1" :controls="false" placeholder="请输入借方合计" />
      </el-form-item>
      <el-form-item label="贷方合计" prop="creditTotal">
        <el-input-number v-model="formData.creditTotal" class="!w-1/1" :controls="false" placeholder="请输入贷方合计" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="formData.statusName" class="!w-1/1" placeholder="请输入状态名称" />
      </el-form-item>
      <el-form-item label="制单人" prop="preparedBy">
        <el-input v-model="formData.preparedBy" class="!w-1/1" placeholder="请输入制单人" />
      </el-form-item>
      <el-form-item label="审核人" prop="approvedBy">
        <el-input v-model="formData.approvedBy" class="!w-1/1" placeholder="请输入审核人" />
      </el-form-item>
      <el-form-item label="审核时间" prop="approvedTime">
        <el-date-picker v-model="formData.approvedTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择审核时间" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="账簿 ID（多账簿支持，空表示默认主账簿）" prop="accountBookId">
        <el-input-number v-model="formData.accountBookId" class="!w-1/1" :controls="false" placeholder="请输入账簿 ID（多账簿支持，空表示默认主账簿）" />
      </el-form-item>
      <el-form-item label="来源会计事件 ID" prop="sourceEventId">
        <el-input-number v-model="formData.sourceEventId" class="!w-1/1" :controls="false" placeholder="请输入来源会计事件 ID" />
      </el-form-item>
      <el-form-item label="来源单据类型" prop="sourceDocType">
        <el-input v-model="formData.sourceDocType" class="!w-1/1" placeholder="请输入来源单据类型" />
      </el-form-item>
      <el-form-item label="来源单据 ID" prop="sourceDocId">
        <el-input-number v-model="formData.sourceDocId" class="!w-1/1" :controls="false" placeholder="请输入来源单据 ID" />
      </el-form-item>
      <el-form-item label="来源单据编号" prop="sourceDocNo">
        <el-input v-model="formData.sourceDocNo" class="!w-1/1" placeholder="请输入来源单据编号" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { ErpGlVoucherApi } from '@/api/erp/gl-voucher'

defineOptions({ name: 'ErpGlVoucherForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  voucherNo: [required],
  periodCode: [required],
  voucherTypeName: [required],
  summary: [required],
  statusName: [required],
  preparedBy: [required],
  approvedBy: [required],
  remark: [required],
  sourceDocType: [required],
  sourceDocNo: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ErpGlVoucherApi.getErpGlVoucher(id)
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
      await ErpGlVoucherApi.createErpGlVoucher(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpGlVoucherApi.updateErpGlVoucher(data)
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
