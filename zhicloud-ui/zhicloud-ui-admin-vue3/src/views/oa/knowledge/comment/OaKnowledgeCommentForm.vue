<!-- OA 知识文章评论表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
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
      <el-form-item label="父评论" prop="parentId">
        <el-input-number
          v-model="formData.parentId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入父评论编号，留空表示根评论"
        />
      </el-form-item>
      <el-form-item label="评论内容" prop="content">
        <el-input v-model="formData.content" type="textarea" :rows="4" maxlength="2000" placeholder="请输入评论内容" />
      </el-form-item>
      <el-form-item label="评论人姓名" prop="commentatorName">
        <el-input v-model="formData.commentatorName" maxlength="64" placeholder="留空则取当前登录用户" class="!w-1/1" />
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
import { OaKnowledgeCommentApi } from '@/api/oa/knowledge-comment'
import type { OaKnowledgeCommentVO } from '@/api/oa/knowledge-comment'

defineOptions({ name: 'OaKnowledgeCommentForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<OaKnowledgeCommentVO>({})
const formRules = reactive<FormRules>({
  articleId: [{ required: true, message: '文章编号不能为空', trigger: 'blur' }],
  content: [{ required: true, message: '评论内容不能为空', trigger: 'blur' }]
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
      formData.value = await OaKnowledgeCommentApi.getOaKnowledgeComment(id)
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
    const data: OaKnowledgeCommentVO = {
      id: formData.value.id,
      articleId: formData.value.articleId,
      parentId: formData.value.parentId || undefined,
      content: formData.value.content,
      commentatorName: formData.value.commentatorName
    }
    if (formData.value.id) {
      await OaKnowledgeCommentApi.updateOaKnowledgeComment(data)
      message.success(t('common.updateSuccess'))
    } else {
      await OaKnowledgeCommentApi.createOaKnowledgeComment(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
