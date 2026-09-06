<!-- 纠正预防措施CAPA 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="CAPA 单号" prop="capaNo">
        <el-input v-model="formData.capaNo" class="!w-1/1" placeholder="请输入CAPA 单号" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="formData.source" class="!w-1/1" :min="0" :controls="false" placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="formData.priority" class="!w-1/1" :min="0" :controls="false" placeholder="请输入优先级" />
      </el-form-item>
      <el-form-item label="当前阶段" prop="stage">
        <el-input-number v-model="formData.stage" class="!w-1/1" :min="0" :controls="false" placeholder="请输入当前阶段" />
      </el-form-item>
      <el-form-item label="问题描述" prop="problem">
        <el-input v-model="formData.problem" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入问题描述" />
      </el-form-item>
      <el-form-item label="原因" prop="cause">
        <el-input v-model="formData.cause" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入原因" />
      </el-form-item>
      <el-form-item label="根本原因分析" prop="rootCauseAnalysis">
        <el-input v-model="formData.rootCauseAnalysis" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入根本原因分析" />
      </el-form-item>
      <el-form-item label="纠正措施" prop="correctiveAction">
        <el-input v-model="formData.correctiveAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入纠正措施" />
      </el-form-item>
      <el-form-item label="预防措施" prop="preventiveAction">
        <el-input v-model="formData.preventiveAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入预防措施" />
      </el-form-item>
      <el-form-item label="责任人" prop="responsiblePerson">
        <el-input v-model="formData.responsiblePerson" class="!w-1/1" placeholder="请输入责任人" />
      </el-form-item>
      <el-form-item label="截止日期" prop="dueDate">
        <el-date-picker v-model="formData.dueDate" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择截止日期" />
      </el-form-item>
      <el-form-item label="关闭日期" prop="closeDate">
        <el-date-picker v-model="formData.closeDate" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择关闭日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="有效性验证结果" prop="verificationResult">
        <el-input-number v-model="formData.verificationResult" class="!w-1/1" :min="0" :controls="false" placeholder="请输入有效性验证结果" />
      </el-form-item>
      <el-form-item label="有效性验证意见" prop="verificationComment">
        <el-input v-model="formData.verificationComment" class="!w-1/1" placeholder="请输入有效性验证意见" />
      </el-form-item>
      <el-form-item label="验证人" prop="verifiedBy">
        <el-input v-model="formData.verifiedBy" class="!w-1/1" placeholder="请输入验证人" />
      </el-form-item>
      <el-form-item label="验证时间" prop="verifiedTime">
        <el-date-picker v-model="formData.verifiedTime" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择验证时间" />
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
import { CAPADocumentApi } from '@/api/qms/capa/document'

defineOptions({ name: 'CAPADocumentForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  source: [required],

  priority: [required],

  cause: [required],

  rootCauseAnalysis: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CAPADocumentApi.getCAPADocument(id)
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
      await CAPADocumentApi.createCAPADocument(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await CAPADocumentApi.updateCAPADocument(formData.value)
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
