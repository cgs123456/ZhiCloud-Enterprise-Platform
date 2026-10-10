<!-- WMS 波次单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="波次单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入波次单号" />
      </el-form-item>
      <el-form-item label="仓库编号" prop="warehouseId">
        <el-input-number v-model="formData.warehouseId" class="!w-1/1" :controls="false" placeholder="请输入仓库编号" />
      </el-form-item>
      <el-form-item label="波次策略" prop="strategy">
        <el-input-number v-model="formData.strategy" class="!w-1/1" :controls="false" placeholder="请输入波次策略" />
      </el-form-item>
      <el-form-item label="单据日期" prop="orderTime">
        <el-date-picker v-model="formData.orderTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择单据日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="拣货员" prop="picker">
        <el-input v-model="formData.picker" class="!w-1/1" placeholder="请输入拣货员" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="出库单数" prop="shipmentCount">
        <el-input-number v-model="formData.shipmentCount" class="!w-1/1" :controls="false" placeholder="请输入出库单数" />
      </el-form-item>
      <el-form-item label="SKU 数" prop="skuCount">
        <el-input-number v-model="formData.skuCount" class="!w-1/1" :controls="false" placeholder="请输入SKU 数" />
      </el-form-item>
      <el-form-item label="总数量" prop="totalQuantity">
        <el-input-number v-model="formData.totalQuantity" class="!w-1/1" :controls="false" placeholder="请输入总数量" />
      </el-form-item>
      <el-form-item label="总金额" prop="totalPrice">
        <el-input-number v-model="formData.totalPrice" class="!w-1/1" :controls="false" placeholder="请输入总金额" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { WmsWaveOrderApi } from '@/api/wms/wave-order'

defineOptions({ name: 'WmsWaveOrderForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  no: [required],
  picker: [required],
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
      formData.value = await WmsWaveOrderApi.getWmsWaveOrder(id)
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
      await WmsWaveOrderApi.createWmsWaveOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await WmsWaveOrderApi.updateWmsWaveOrder(data)
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
