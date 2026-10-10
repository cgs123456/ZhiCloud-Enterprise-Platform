<!-- TMS 司机 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="司机姓名" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入司机姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="formData.phone" class="!w-1/1" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="驾照号" prop="licenseNo">
        <el-input v-model="formData.licenseNo" class="!w-1/1" placeholder="请输入驾照号" />
      </el-form-item>
      <el-form-item label="准驾车型" prop="licenseType">
        <el-input v-model="formData.licenseType" class="!w-1/1" placeholder="请输入准驾车型" />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number v-model="formData.carrierId" class="!w-1/1" :controls="false" placeholder="请输入承运商编号" />
      </el-form-item>
      <el-form-item label="状态（10 可用 / 20 运输中 / 30 休假）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 可用 / 20 运输中 / 30 休假）" />
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
import { TmsDriverApi } from '@/api/tms/driver'

defineOptions({ name: 'TmsDriverForm' })

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
  phone: [required],
  licenseNo: [required],
  licenseType: [required],
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
      formData.value = await TmsDriverApi.getTmsDriver(id)
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
      await TmsDriverApi.createTmsDriver(data)
      message.success(t('common.createSuccess'))
    } else {
      await TmsDriverApi.updateTmsDriver(data)
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
