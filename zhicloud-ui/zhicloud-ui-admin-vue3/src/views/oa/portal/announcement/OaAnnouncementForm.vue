<!-- OA 公告 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="正文内容" prop="content">
        <el-input v-model="formData.content" class="!w-1/1" placeholder="请输入正文内容" />
      </el-form-item>
      <el-form-item label="公告分类（company 公司/dept 部门/policy 制度/notice 通知）" prop="category">
        <el-input v-model="formData.category" class="!w-1/1" placeholder="请输入公告分类（company 公司/dept 部门/policy 制度/notice 通知）" />
      </el-form-item>
      <el-form-item label="优先级（10 普通 20 重要 30 紧急）" prop="priority">
        <el-input-number v-model="formData.priority" class="!w-1/1" :controls="false" placeholder="请输入优先级（10 普通 20 重要 30 紧急）" />
      </el-form-item>
      <el-form-item label="发布人用户 ID" prop="publisherUserId">
        <el-input-number v-model="formData.publisherUserId" class="!w-1/1" :controls="false" placeholder="请输入发布人用户 ID" />
      </el-form-item>
      <el-form-item label="发布人姓名" prop="publisherName">
        <el-input v-model="formData.publisherName" class="!w-1/1" placeholder="请输入发布人姓名" />
      </el-form-item>
      <el-form-item label="目标范围（all 全员/dept_id 列表逗号分隔）" prop="targetScope">
        <el-input v-model="formData.targetScope" class="!w-1/1" placeholder="请输入目标范围（all 全员/dept_id 列表逗号分隔）" />
      </el-form-item>
      <el-form-item label="发布时间" prop="publishTime">
        <el-date-picker v-model="formData.publishTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择发布时间" />
      </el-form-item>
      <el-form-item label="过期时间" prop="expireTime">
        <el-date-picker v-model="formData.expireTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择过期时间" />
      </el-form-item>
      <el-form-item label="是否置顶" prop="topFlag">
        <el-switch v-model="formData.topFlag" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 已发布 30 已下架 40 已过期）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 草稿 20 已发布 30 已下架 40 已过期）" />
      </el-form-item>
      <el-form-item label="浏览次数" prop="viewCount">
        <el-input-number v-model="formData.viewCount" class="!w-1/1" :controls="false" placeholder="请输入浏览次数" />
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
import { OaAnnouncementApi } from '@/api/oa/announcement'

defineOptions({ name: 'OaAnnouncementForm' })

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
  content: [required],
  category: [required],
  publisherName: [required],
  targetScope: [required],
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
      formData.value = await OaAnnouncementApi.getOaAnnouncement(id)
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
      await OaAnnouncementApi.createOaAnnouncement(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaAnnouncementApi.updateOaAnnouncement(data)
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
