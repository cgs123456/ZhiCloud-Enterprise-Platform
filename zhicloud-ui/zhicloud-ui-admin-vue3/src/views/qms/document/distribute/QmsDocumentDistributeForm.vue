<!-- 文档分发 表单 -->
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
      <el-form-item label="分发对象" prop="distributeTo">
        <el-input v-model="formData.distributeTo" class="!w-1/1" placeholder="请输入分发对象" />
      </el-form-item>
      <el-form-item label="分发份数" prop="distributeQty">
        <el-input-number v-model="formData.distributeQty" class="!w-1/1" :min="0" :controls="false" placeholder="请输入分发份数" />
      </el-form-item>
      <el-form-item label="分发日期" prop="distributeDate">
        <el-date-picker v-model="formData.distributeDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择分发日期" />
      </el-form-item>
      <el-form-item label="签收人" prop="receivedBy">
        <el-input v-model="formData.receivedBy" class="!w-1/1" placeholder="请输入签收人" />
      </el-form-item>
      <el-form-item label="签收日期" prop="receivedDate">
        <el-date-picker v-model="formData.receivedDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择签收日期" />
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
import { QmsDocumentDistributeApi } from '@/api/qms/document/distribute'

defineOptions({ name: 'QmsDocumentDistributeForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  distributeTo: [required],

  distributeQty: [required],

  distributeDate: [required],

  receivedBy: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsDocumentDistributeApi.getQmsDocumentDistribute(id)
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
      await QmsDocumentDistributeApi.createQmsDocumentDistribute(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsDocumentDistributeApi.updateQmsDocumentDistribute(formData.value)
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
