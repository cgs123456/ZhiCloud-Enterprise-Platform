<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="planNo" prop="planNo">
        <el-input v-model="formData.planNo" placeholder="请输入planNo" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="productCode" prop="productCode">
        <el-input v-model="formData.productCode" placeholder="请输入productCode" />
      </el-form-item>
      <el-form-item label="productName" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入productName" />
      </el-form-item>
      <el-form-item label="计划期间" prop="planPeriod">
        <el-input v-model="formData.planPeriod" placeholder="请输入计划期间" />
      </el-form-item>
      <el-form-item label="计划类型" prop="planType">
        <el-input-number v-model="formData.planType" placeholder="请输入计划类型" />
      </el-form-item>
      <el-form-item label="demandDate" prop="demandDate">
        <el-input-number v-model="formData.demandDate" placeholder="请输入demandDate" />
      </el-form-item>
      <el-form-item label="plannedQuantity" prop="plannedQuantity">
        <el-input-number v-model="formData.plannedQuantity" placeholder="请输入plannedQuantity" />
      </el-form-item>
      <el-form-item label="plannedFinishDate" prop="plannedFinishDate">
        <el-input-number v-model="formData.plannedFinishDate" placeholder="请输入plannedFinishDate" />
      </el-form-item>
      <el-form-item label="source" prop="source">
        <el-input-number v-model="formData.source" placeholder="请输入source" />
      </el-form-item>
      <el-form-item label="sourceOrderId" prop="sourceOrderId">
        <el-input-number v-model="formData.sourceOrderId" placeholder="请输入sourceOrderId" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { MpsPlanApi } from '@/api/erp/mps-plan'

defineOptions({ name: 'MpsPlanForm' })

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
      formData.value = await MpsPlanApi.getMpsPlan(id)
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
      await MpsPlanApi.createMpsPlan(data)
      message.success(t('common.createSuccess'))
    } else {
      await MpsPlanApi.updateMpsPlan(data)
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
