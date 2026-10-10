<!-- ERP 合并抵消分录 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="合并任务编号" prop="consolidationNo">
        <el-input v-model="formData.consolidationNo" class="!w-1/1" placeholder="请输入合并任务编号" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="formData.periodId" class="!w-1/1" :controls="false" placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="期间编码" prop="periodCode">
        <el-input v-model="formData.periodCode" class="!w-1/1" placeholder="请输入期间编码" />
      </el-form-item>
      <el-form-item label="抵消类型" prop="eliminationType">
        <el-input-number v-model="formData.eliminationType" class="!w-1/1" :controls="false" placeholder="请输入抵消类型" />
      </el-form-item>
      <el-form-item label="抵消类型名称" prop="eliminationTypeName">
        <el-input v-model="formData.eliminationTypeName" class="!w-1/1" placeholder="请输入抵消类型名称" />
      </el-form-item>
      <el-form-item label="借方科目编号" prop="debitAccountId">
        <el-input-number v-model="formData.debitAccountId" class="!w-1/1" :controls="false" placeholder="请输入借方科目编号" />
      </el-form-item>
      <el-form-item label="借方科目编码" prop="debitAccountCode">
        <el-input v-model="formData.debitAccountCode" class="!w-1/1" placeholder="请输入借方科目编码" />
      </el-form-item>
      <el-form-item label="借方科目名称" prop="debitAccountName">
        <el-input v-model="formData.debitAccountName" class="!w-1/1" placeholder="请输入借方科目名称" />
      </el-form-item>
      <el-form-item label="贷方科目编号" prop="creditAccountId">
        <el-input-number v-model="formData.creditAccountId" class="!w-1/1" :controls="false" placeholder="请输入贷方科目编号" />
      </el-form-item>
      <el-form-item label="贷方科目编码" prop="creditAccountCode">
        <el-input v-model="formData.creditAccountCode" class="!w-1/1" placeholder="请输入贷方科目编码" />
      </el-form-item>
      <el-form-item label="贷方科目名称" prop="creditAccountName">
        <el-input v-model="formData.creditAccountName" class="!w-1/1" placeholder="请输入贷方科目名称" />
      </el-form-item>
      <el-form-item label="抵消金额" prop="eliminationAmount">
        <el-input-number v-model="formData.eliminationAmount" class="!w-1/1" :controls="false" placeholder="请输入抵消金额" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 / 20 已审核）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 草稿 / 20 已审核）" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="formData.statusName" class="!w-1/1" placeholder="请输入状态名称" />
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
import { ErpConsolidationEntryApi } from '@/api/erp/consolidation-entry'

defineOptions({ name: 'ErpConsolidationEntryForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  consolidationNo: [required],
  periodCode: [required],
  eliminationTypeName: [required],
  debitAccountCode: [required],
  debitAccountName: [required],
  creditAccountCode: [required],
  creditAccountName: [required],
  statusName: [required],
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
      formData.value = await ErpConsolidationEntryApi.getErpConsolidationEntry(id)
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
      await ErpConsolidationEntryApi.createErpConsolidationEntry(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpConsolidationEntryApi.updateErpConsolidationEntry(data)
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
