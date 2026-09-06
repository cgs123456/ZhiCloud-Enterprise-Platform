<!-- FMEA文档 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="FMEA 单号" prop="fmeaNo">
        <el-input v-model="formData.fmeaNo" class="!w-1/1" placeholder="请输入FMEA 单号" />
      </el-form-item>
      <el-form-item label="FMEA 类型" prop="fmeaType">
        <el-input-number v-model="formData.fmeaType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入FMEA 类型" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="工序 ID" prop="processId">
        <el-input-number v-model="formData.processId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入工序 ID" />
      </el-form-item>
      <el-form-item label="版本" prop="version">
        <el-input v-model="formData.version" class="!w-1/1" placeholder="请输入版本" />
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
import { FmeaDocumentApi } from '@/api/qms/fmea/document'

defineOptions({ name: 'FmeaDocumentForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  fmeaType: [required],

  productId: [required],

  processId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await FmeaDocumentApi.getFmeaDocument(id)
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
      await FmeaDocumentApi.createFmeaDocument(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await FmeaDocumentApi.updateFmeaDocument(formData.value)
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
