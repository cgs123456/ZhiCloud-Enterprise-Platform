<!-- TMS 承运商 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="承运商名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入承运商名称" />
      </el-form-item>
      <el-form-item label="承运商编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入承运商编码" />
      </el-form-item>
      <el-form-item label="服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）" prop="serviceType">
        <el-input-number v-model="formData.serviceType" class="!w-1/1" :controls="false" placeholder="请输入服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input v-model="formData.contactPerson" class="!w-1/1" placeholder="请输入联系人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="formData.contactPhone" class="!w-1/1" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="资质编号" prop="qualificationNo">
        <el-input v-model="formData.qualificationNo" class="!w-1/1" placeholder="请输入资质编号" />
      </el-form-item>
      <el-form-item label="评分（1-5）" prop="rating">
        <el-input-number v-model="formData.rating" class="!w-1/1" :controls="false" placeholder="请输入评分（1-5）" />
      </el-form-item>
      <el-form-item label="状态（10 启用 / 20 停用）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 启用 / 20 停用）" />
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
import { TmsCarrierApi } from '@/api/tms/carrier'

defineOptions({ name: 'TmsCarrierForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  name: [required],
  code: [required],
  contactPerson: [required],
  contactPhone: [required],
  qualificationNo: [required],
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
      formData.value = await TmsCarrierApi.getTmsCarrier(id)
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
      await TmsCarrierApi.createTmsCarrier(data)
      message.success(t('common.createSuccess'))
    } else {
      await TmsCarrierApi.updateTmsCarrier(data)
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
