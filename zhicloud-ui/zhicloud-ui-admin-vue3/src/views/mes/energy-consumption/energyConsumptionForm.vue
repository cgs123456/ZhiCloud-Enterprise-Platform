<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="workshopId" prop="workshopId">
        <el-input-number v-model="formData.workshopId" placeholder="请输入workshopId" />
      </el-form-item>
      <el-form-item label="workstationId" prop="workstationId">
        <el-input-number v-model="formData.workstationId" placeholder="请输入workstationId" />
      </el-form-item>
      <el-form-item label="energyType" prop="energyType">
        <el-input-number v-model="formData.energyType" placeholder="请输入energyType" />
      </el-form-item>
      <el-form-item label="recordDate" prop="recordDate">
        <el-input-number v-model="formData.recordDate" placeholder="请输入recordDate" />
      </el-form-item>
      <el-form-item label="consumption" prop="consumption">
        <el-input-number v-model="formData.consumption" placeholder="请输入consumption" />
      </el-form-item>
      <el-form-item label="unit" prop="unit">
        <el-input v-model="formData.unit" placeholder="请输入unit" />
      </el-form-item>
      <el-form-item label="unitPrice" prop="unitPrice">
        <el-input-number v-model="formData.unitPrice" placeholder="请输入unitPrice" />
      </el-form-item>
      <el-form-item label="totalAmount" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" placeholder="请输入totalAmount" />
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
import { EnergyConsumptionApi } from '@/api/mes/energy-consumption'

defineOptions({ name: 'EnergyConsumptionForm' })

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
      formData.value = await EnergyConsumptionApi.getEnergyConsumption(id)
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
      await EnergyConsumptionApi.createEnergyConsumption(data)
      message.success(t('common.createSuccess'))
    } else {
      await EnergyConsumptionApi.updateEnergyConsumption(data)
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
