<!-- 供应商纠正措施SCAR 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="SCAR 单号" prop="scarNo">
        <el-input v-model="formData.scarNo" class="!w-1/1" placeholder="请输入SCAR 单号" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="formData.supplierId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="formData.supplierName" class="!w-1/1" placeholder="请输入供应商名称" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="formData.productName" class="!w-1/1" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="缺陷描述" prop="defectDescription">
        <el-input v-model="formData.defectDescription" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入缺陷描述" />
      </el-form-item>
      <el-form-item label="根本原因" prop="rootCause">
        <el-input v-model="formData.rootCause" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入根本原因" />
      </el-form-item>
      <el-form-item label="纠正措施" prop="correctiveAction">
        <el-input v-model="formData.correctiveAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入纠正措施" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="关闭时间" prop="closeTime">
        <el-date-picker v-model="formData.closeTime" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择关闭时间" />
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
import { ScarApi } from '@/api/qms/sqm/scar'

defineOptions({ name: 'ScarForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  supplierId: [required],

  supplierName: [required],

  productId: [required],

  rootCause: [required],

  correctiveAction: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ScarApi.getScar(id)
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
      await ScarApi.createScar(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await ScarApi.updateScar(formData.value)
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
