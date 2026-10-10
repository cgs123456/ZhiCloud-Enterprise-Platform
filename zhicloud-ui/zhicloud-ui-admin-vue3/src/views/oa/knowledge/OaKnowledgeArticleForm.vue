<!-- OA 知识文章 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="分类 ID" prop="categoryId">
        <el-input-number v-model="formData.categoryId" class="!w-1/1" :controls="false" placeholder="请输入分类 ID" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="formData.summary" class="!w-1/1" placeholder="请输入摘要" />
      </el-form-item>
      <el-form-item label="正文（Markdown/HTML）" prop="content">
        <el-input v-model="formData.content" class="!w-1/1" placeholder="请输入正文（Markdown/HTML）" />
      </el-form-item>
      <el-form-item label="标签（逗号分隔）" prop="tags">
        <el-input v-model="formData.tags" class="!w-1/1" placeholder="请输入标签（逗号分隔）" />
      </el-form-item>
      <el-form-item label="作者 ID" prop="authorUserId">
        <el-input-number v-model="formData.authorUserId" class="!w-1/1" :controls="false" placeholder="请输入作者 ID" />
      </el-form-item>
      <el-form-item label="作者姓名" prop="authorName">
        <el-input v-model="formData.authorName" class="!w-1/1" placeholder="请输入作者姓名" />
      </el-form-item>
      <el-form-item label="阅读量" prop="viewCount">
        <el-input-number v-model="formData.viewCount" class="!w-1/1" :controls="false" placeholder="请输入阅读量" />
      </el-form-item>
      <el-form-item label="点赞数" prop="likeCount">
        <el-input-number v-model="formData.likeCount" class="!w-1/1" :controls="false" placeholder="请输入点赞数" />
      </el-form-item>
      <el-form-item label="评论数" prop="commentCount">
        <el-input-number v-model="formData.commentCount" class="!w-1/1" :controls="false" placeholder="请输入评论数" />
      </el-form-item>
      <el-form-item label="当前版本号" prop="currentVersion">
        <el-input-number v-model="formData.currentVersion" class="!w-1/1" :controls="false" placeholder="请输入当前版本号" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 已发布 30 已下架）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 草稿 20 已发布 30 已下架）" />
      </el-form-item>
      <el-form-item label="是否置顶" prop="topFlag">
        <el-switch v-model="formData.topFlag" />
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
import { OaKnowledgeArticleApi } from '@/api/oa/knowledge-article'

defineOptions({ name: 'OaKnowledgeArticleForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  title: [required],
  summary: [required],
  content: [required],
  tags: [required],
  authorName: [required],
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
      formData.value = await OaKnowledgeArticleApi.getOaKnowledgeArticle(id)
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
      await OaKnowledgeArticleApi.createOaKnowledgeArticle(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaKnowledgeArticleApi.updateOaKnowledgeArticle(data)
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
