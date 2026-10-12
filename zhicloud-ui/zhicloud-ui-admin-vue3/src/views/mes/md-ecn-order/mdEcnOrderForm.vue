<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编号" prop="no">
        <el-input v-model="formData.no" placeholder="请输入编号" />
      </el-form-item>
      <el-form-item label="ecnName" prop="ecnName">
        <el-input v-model="formData.ecnName" placeholder="请输入ecnName" />
      </el-form-item>
      <el-form-item label="changeType" prop="changeType">
        <el-input-number v-model="formData.changeType" placeholder="请输入changeType" />
      </el-form-item>
      <el-form-item label="bomId" prop="bomId">
        <el-input-number v-model="formData.bomId" placeholder="请输入bomId" />
      </el-form-item>
      <el-form-item label="newBomId" prop="newBomId">
        <el-input-number v-model="formData.newBomId" placeholder="请输入newBomId" />
      </el-form-item>
      <el-form-item label="changeReason" prop="changeReason">
        <el-input v-model="formData.changeReason" placeholder="请输入changeReason" />
      </el-form-item>
      <el-form-item label="changeDescription" prop="changeDescription">
        <el-input v-model="formData.changeDescription" placeholder="请输入changeDescription" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="applicantUserId" prop="applicantUserId">
        <el-input-number v-model="formData.applicantUserId" placeholder="请输入applicantUserId" />
      </el-form-item>
      <el-form-item label="approveUserId" prop="approveUserId">
        <el-input-number v-model="formData.approveUserId" placeholder="请输入approveUserId" />
      </el-form-item>
      <el-form-item label="approveDate" prop="approveDate">
        <el-input-number v-model="formData.approveDate" placeholder="请输入approveDate" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-input-number v-model="formData.effectiveDate" placeholder="请输入生效日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { MdEcnOrderApi } from '@/api/mes/md-ecn-order'

defineOptions({ name: 'MdEcnOrderForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<any>({})
const formRules = reactive({})
const formRef = ref()

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await MdEcnOrderApi.getMdEcnOrder(id)
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
    const data = formData.value
    if (formType.value === 'create') {
      await MdEcnOrderApi.createMdEcnOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await MdEcnOrderApi.updateMdEcnOrder(data)
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
