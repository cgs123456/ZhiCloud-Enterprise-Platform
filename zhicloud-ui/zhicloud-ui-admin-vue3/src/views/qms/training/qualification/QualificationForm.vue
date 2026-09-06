<!-- 上岗资质 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="用户 ID" prop="userId">
        <el-input-number v-model="formData.userId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入用户 ID" />
      </el-form-item>
      <el-form-item label="用户姓名" prop="userName">
        <el-input v-model="formData.userName" class="!w-1/1" placeholder="请输入用户姓名" />
      </el-form-item>
      <el-form-item label="岗位 ID" prop="postId">
        <el-input-number v-model="formData.postId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入岗位 ID" />
      </el-form-item>
      <el-form-item label="岗位名称" prop="postName">
        <el-input v-model="formData.postName" class="!w-1/1" placeholder="请输入岗位名称" />
      </el-form-item>
      <el-form-item label="资格名称" prop="qualificationName">
        <el-input v-model="formData.qualificationName" class="!w-1/1" placeholder="请输入资格名称" />
      </el-form-item>
      <el-form-item label="取得日期" prop="qualifyDate">
        <el-date-picker v-model="formData.qualifyDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择取得日期" />
      </el-form-item>
      <el-form-item label="到期日期" prop="expireDate">
        <el-date-picker v-model="formData.expireDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择到期日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
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
import { QualificationApi } from '@/api/qms/training/qualification'

defineOptions({ name: 'QualificationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  userName: [required],

  postId: [required],

  postName: [required],

  qualifyDate: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QualificationApi.getQualification(id)
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
      await QualificationApi.createQualification(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QualificationApi.updateQualification(formData.value)
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
