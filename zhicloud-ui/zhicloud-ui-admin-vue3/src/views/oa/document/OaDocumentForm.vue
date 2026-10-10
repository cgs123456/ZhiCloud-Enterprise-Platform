<!-- OA 公文 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="公文编号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入公文编号" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）" prop="documentType">
        <el-input-number v-model="formData.documentType" class="!w-1/1" :controls="false" placeholder="请输入公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）" />
      </el-form-item>
      <el-form-item label="紧急程度（10 普通 20 紧急 30 特急）" prop="urgency">
        <el-input-number v-model="formData.urgency" class="!w-1/1" :controls="false" placeholder="请输入紧急程度（10 普通 20 紧急 30 特急）" />
      </el-form-item>
      <el-form-item label="保密级别（10 公开 20 内部 30 秘密）" prop="confidentiality">
        <el-input-number v-model="formData.confidentiality" class="!w-1/1" :controls="false" placeholder="请输入保密级别（10 公开 20 内部 30 秘密）" />
      </el-form-item>
      <el-form-item label="发文人 ID" prop="issuerUserId">
        <el-input-number v-model="formData.issuerUserId" class="!w-1/1" :controls="false" placeholder="请输入发文人 ID" />
      </el-form-item>
      <el-form-item label="发文部门 ID" prop="issueDeptId">
        <el-input-number v-model="formData.issueDeptId" class="!w-1/1" :controls="false" placeholder="请输入发文部门 ID" />
      </el-form-item>
      <el-form-item label="发文日期" prop="issueDate">
        <el-date-picker v-model="formData.issueDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择发文日期" />
      </el-form-item>
      <el-form-item label="正文内容" prop="content">
        <el-input v-model="formData.content" class="!w-1/1" placeholder="请输入正文内容" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="formData.processInstanceId" class="!w-1/1" placeholder="请输入工作流编号" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 审核中 30 已发布 40 已废止）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 草稿 20 审核中 30 已发布 40 已废止）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="核稿人 ID" prop="reviewerUserId">
        <el-input-number v-model="formData.reviewerUserId" class="!w-1/1" :controls="false" placeholder="请输入核稿人 ID" />
      </el-form-item>
      <el-form-item label="核稿人姓名" prop="reviewerName">
        <el-input v-model="formData.reviewerName" class="!w-1/1" placeholder="请输入核稿人姓名" />
      </el-form-item>
      <el-form-item label="核稿时间" prop="reviewTime">
        <el-date-picker v-model="formData.reviewTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择核稿时间" />
      </el-form-item>
      <el-form-item label="核稿意见" prop="reviewOpinion">
        <el-input v-model="formData.reviewOpinion" class="!w-1/1" placeholder="请输入核稿意见" />
      </el-form-item>
      <el-form-item label="签发人 ID" prop="signerUserId">
        <el-input-number v-model="formData.signerUserId" class="!w-1/1" :controls="false" placeholder="请输入签发人 ID" />
      </el-form-item>
      <el-form-item label="签发人姓名" prop="signerName">
        <el-input v-model="formData.signerName" class="!w-1/1" placeholder="请输入签发人姓名" />
      </el-form-item>
      <el-form-item label="签发时间" prop="signTime">
        <el-date-picker v-model="formData.signTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择签发时间" />
      </el-form-item>
      <el-form-item label="签发意见" prop="signOpinion">
        <el-input v-model="formData.signOpinion" class="!w-1/1" placeholder="请输入签发意见" />
      </el-form-item>
      <el-form-item label="归档人 ID" prop="archiverUserId">
        <el-input-number v-model="formData.archiverUserId" class="!w-1/1" :controls="false" placeholder="请输入归档人 ID" />
      </el-form-item>
      <el-form-item label="归档人姓名" prop="archiverName">
        <el-input v-model="formData.archiverName" class="!w-1/1" placeholder="请输入归档人姓名" />
      </el-form-item>
      <el-form-item label="归档时间" prop="archiveTime">
        <el-date-picker v-model="formData.archiveTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择归档时间" />
      </el-form-item>
      <el-form-item label="归档编号" prop="archiveNo">
        <el-input v-model="formData.archiveNo" class="!w-1/1" placeholder="请输入归档编号" />
      </el-form-item>
      <el-form-item label="主送部门（逗号分隔 ID）" prop="mainSendDepts">
        <el-input v-model="formData.mainSendDepts" class="!w-1/1" placeholder="请输入主送部门（逗号分隔 ID）" />
      </el-form-item>
      <el-form-item label="抄送部门（逗号分隔 ID）" prop="copySendDepts">
        <el-input v-model="formData.copySendDepts" class="!w-1/1" placeholder="请输入抄送部门（逗号分隔 ID）" />
      </el-form-item>
      <el-form-item label="阅读量" prop="readCount">
        <el-input-number v-model="formData.readCount" class="!w-1/1" :controls="false" placeholder="请输入阅读量" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { OaDocumentApi } from '@/api/oa/document'

defineOptions({ name: 'OaDocumentForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  no: [required],
  title: [required],
  content: [required],
  processInstanceId: [required],
  remark: [required],
  reviewerName: [required],
  reviewOpinion: [required],
  signerName: [required],
  signOpinion: [required],
  archiverName: [required],
  archiveNo: [required],
  mainSendDepts: [required],
  copySendDepts: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaDocumentApi.getOaDocument(id)
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
      await OaDocumentApi.createOaDocument(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaDocumentApi.updateOaDocument(data)
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
