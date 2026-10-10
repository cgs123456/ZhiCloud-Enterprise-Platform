<!-- OA 审批模板 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="模板编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入模板编码" />
      </el-form-item>
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入模板名称" />
      </el-form-item>
      <el-form-item label="分类（通用/人事/财务/采购/合同/其他）" prop="category">
        <el-input v-model="formData.category" class="!w-1/1" placeholder="请输入分类（通用/人事/财务/采购/合同/其他）" />
      </el-form-item>
      <el-form-item label="BPM 流程定义 KEY" prop="processDefinitionKey">
        <el-input v-model="formData.processDefinitionKey" class="!w-1/1" placeholder="请输入BPM 流程定义 KEY" />
      </el-form-item>
      <el-form-item label="表单 JSON Schema（字段定义）" prop="formSchema">
        <el-input v-model="formData.formSchema" class="!w-1/1" placeholder="请输入表单 JSON Schema（字段定义）" />
      </el-form-item>
      <el-form-item label="表单 UI Schema（布局/校验）" prop="formUiSchema">
        <el-input v-model="formData.formUiSchema" class="!w-1/1" placeholder="请输入表单 UI Schema（布局/校验）" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" class="!w-1/1" placeholder="请输入描述" />
      </el-form-item>
      <el-form-item label="图标" prop="icon">
        <el-input v-model="formData.icon" class="!w-1/1" placeholder="请输入图标" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
      </el-form-item>
      <el-form-item label="状态（0 启用 1 停用）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（0 启用 1 停用）" />
      </el-form-item>
      <el-form-item label="使用次数" prop="usageCount">
        <el-input-number v-model="formData.usageCount" class="!w-1/1" :controls="false" placeholder="请输入使用次数" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { OaApprovalTemplateApi } from '@/api/oa/approval-template'

defineOptions({ name: 'OaApprovalTemplateForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  code: [required],
  name: [required],
  category: [required],
  processDefinitionKey: [required],
  formSchema: [required],
  formUiSchema: [required],
  description: [required],
  icon: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaApprovalTemplateApi.getOaApprovalTemplate(id)
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
      await OaApprovalTemplateApi.createOaApprovalTemplate(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaApprovalTemplateApi.updateOaApprovalTemplate(data)
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
