<!-- 变更申请 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="受控文档 ID" prop="documentId">
        <el-input-number v-model="formData.documentId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入受控文档 ID" />
      </el-form-item>
      <el-form-item label="变更类型" prop="changeType">
        <el-input-number v-model="formData.changeType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入变更类型" />
      </el-form-item>
      <el-form-item label="变更原因" prop="changeReason">
        <el-input v-model="formData.changeReason" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入变更原因" />
      </el-form-item>
      <el-form-item label="变更内容" prop="changeContent">
        <el-input v-model="formData.changeContent" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入变更内容" />
      </el-form-item>
      <el-form-item label="申请人 ID" prop="applicantId">
        <el-input-number v-model="formData.applicantId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入申请人 ID" />
      </el-form-item>
      <el-form-item label="申请日期" prop="applyDate">
        <el-date-picker v-model="formData.applyDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择申请日期" />
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
import { QmsDocumentChangeRequestApi } from '@/api/qms/document/change-request'

defineOptions({ name: 'QmsDocumentChangeRequestForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  changeType: [required],

  changeReason: [required],

  changeContent: [required],

  applicantId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsDocumentChangeRequestApi.getQmsDocumentChangeRequest(id)
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
      await QmsDocumentChangeRequestApi.createQmsDocumentChangeRequest(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsDocumentChangeRequestApi.updateQmsDocumentChangeRequest(formData.value)
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
