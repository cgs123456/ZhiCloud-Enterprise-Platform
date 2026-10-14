<!-- OA 知识版本表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="文章" prop="articleId">
        <el-input-number
          v-model="formData.articleId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入文章编号"
        />
      </el-form-item>
      <el-form-item label="版本号" prop="versionNo">
        <el-input-number
          v-model="formData.versionNo"
          :controls="false"
          :min="1"
          class="!w-1/1"
          placeholder="请输入版本号（同一文章内唯一）"
        />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" maxlength="256" placeholder="请输入标题" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="formData.summary" type="textarea" :rows="2" maxlength="512" placeholder="请输入摘要" />
      </el-form-item>
      <el-form-item label="正文" prop="content">
        <el-input v-model="formData.content" type="textarea" :rows="6" maxlength="20000" placeholder="请输入正文内容" />
      </el-form-item>
      <el-form-item label="变更说明" prop="changeLog">
        <el-input v-model="formData.changeLog" type="textarea" :rows="2" maxlength="512" placeholder="请输入变更说明" />
      </el-form-item>
      <el-form-item label="编辑人姓名" prop="editorName">
        <el-input v-model="formData.editorName" maxlength="64" placeholder="留空则取当前登录用户" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { OaKnowledgeVersionApi } from '@/api/oa/knowledge-version'
import type { OaKnowledgeVersionVO } from '@/api/oa/knowledge-version'

defineOptions({ name: 'OaKnowledgeVersionForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<OaKnowledgeVersionVO>({})
const formRules = reactive<FormRules>({
  articleId: [{ required: true, message: '文章编号不能为空', trigger: 'blur' }],
  versionNo: [{ required: true, message: '版本号不能为空', trigger: 'blur' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = {}
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await OaKnowledgeVersionApi.getOaKnowledgeVersion(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: OaKnowledgeVersionVO = {
      id: formData.value.id,
      articleId: formData.value.articleId,
      versionNo: formData.value.versionNo,
      title: formData.value.title,
      summary: formData.value.summary,
      content: formData.value.content,
      changeLog: formData.value.changeLog,
      editorName: formData.value.editorName
    }
    if (formData.value.id) {
      await OaKnowledgeVersionApi.updateOaKnowledgeVersion(data)
      message.success(t('common.updateSuccess'))
    } else {
      await OaKnowledgeVersionApi.createOaKnowledgeVersion(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
