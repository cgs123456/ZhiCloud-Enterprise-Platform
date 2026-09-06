<!-- 不合格品NCR 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="NCR 单号" prop="ncrNo">
        <el-input v-model="formData.ncrNo" class="!w-1/1" placeholder="请输入NCR 单号" />
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-input-number v-model="formData.source" class="!w-1/1" :min="0" :controls="false" placeholder="请输入来源" />
      </el-form-item>
      <el-form-item label="检验单 ID" prop="inspectionOrderId">
        <el-input-number v-model="formData.inspectionOrderId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验单 ID" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="formData.supplierId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="工单 ID" prop="workOrderId">
        <el-input-number v-model="formData.workOrderId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入工单 ID" />
      </el-form-item>
      <el-form-item label="缺陷描述" prop="defectDescription">
        <el-input v-model="formData.defectDescription" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入缺陷描述" />
      </el-form-item>
      <el-form-item label="缺陷等级" prop="defectLevel">
        <el-input-number v-model="formData.defectLevel" class="!w-1/1" :min="0" :controls="false" placeholder="请输入缺陷等级" />
      </el-form-item>
      <el-form-item label="不合格数量" prop="quantity">
        <el-input-number v-model="formData.quantity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入不合格数量" />
      </el-form-item>
      <el-form-item label="处置方式" prop="disposition">
        <el-input-number v-model="formData.disposition" class="!w-1/1" :min="0" :controls="false" placeholder="请输入处置方式" />
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
import { NcrDocumentApi } from '@/api/qms/ncr/document'

defineOptions({ name: 'NcrDocumentForm' })

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

  inspectionOrderId: [required],

  defectLevel: [required],

  disposition: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await NcrDocumentApi.getNcrDocument(id)
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
      await NcrDocumentApi.createNcrDocument(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await NcrDocumentApi.updateNcrDocument(formData.value)
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
