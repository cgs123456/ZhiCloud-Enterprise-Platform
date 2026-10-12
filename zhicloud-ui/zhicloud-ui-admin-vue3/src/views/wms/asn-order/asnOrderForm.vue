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
      <el-form-item label="supplierId" prop="supplierId">
        <el-input-number v-model="formData.supplierId" placeholder="请输入supplierId" />
      </el-form-item>
      <el-form-item label="warehouseId" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" placeholder="请输入warehouseId" />
      </el-form-item>
      <el-form-item label="dockId" prop="dockId">
        <el-input-number v-model="formData.dockId" placeholder="请输入dockId" />
      </el-form-item>
      <el-form-item label="expectedArrivalTime" prop="expectedArrivalTime">
        <el-input-number v-model="formData.expectedArrivalTime" placeholder="请输入expectedArrivalTime" />
      </el-form-item>
      <el-form-item label="actualArrivalTime" prop="actualArrivalTime">
        <el-input-number v-model="formData.actualArrivalTime" placeholder="请输入actualArrivalTime" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="totalQuantity" prop="totalQuantity">
        <el-input-number v-model="formData.totalQuantity" placeholder="请输入totalQuantity" />
      </el-form-item>
      <el-form-item label="totalAmount" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" placeholder="请输入totalAmount" />
      </el-form-item>
      <el-form-item label="transportMode" prop="transportMode">
        <el-input-number v-model="formData.transportMode" placeholder="请输入transportMode" />
      </el-form-item>
      <el-form-item label="carrierName" prop="carrierName">
        <el-input v-model="formData.carrierName" placeholder="请输入carrierName" />
      </el-form-item>
      <el-form-item label="vehicleNo" prop="vehicleNo">
        <el-input v-model="formData.vehicleNo" placeholder="请输入vehicleNo" />
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
import { AsnOrderApi } from '@/api/wms/asn-order'

defineOptions({ name: 'AsnOrderForm' })

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
      formData.value = await AsnOrderApi.getAsnOrder(id)
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
      await AsnOrderApi.createAsnOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await AsnOrderApi.updateAsnOrder(data)
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
