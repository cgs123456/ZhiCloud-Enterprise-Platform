<!-- WMS 越库单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="越库单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入越库单号" />
      </el-form-item>
      <el-form-item label="源头供应商编号" prop="sourceSupplierId">
        <el-input-number v-model="formData.sourceSupplierId" class="!w-1/1" :controls="false" placeholder="请输入源头供应商编号" />
      </el-form-item>
      <el-form-item label="源头供应商名称" prop="sourceSupplierName">
        <el-input v-model="formData.sourceSupplierName" class="!w-1/1" placeholder="请输入源头供应商名称" />
      </el-form-item>
      <el-form-item label="目标客户编号" prop="targetCustomerId">
        <el-input-number v-model="formData.targetCustomerId" class="!w-1/1" :controls="false" placeholder="请输入目标客户编号" />
      </el-form-item>
      <el-form-item label="目标客户名称" prop="targetCustomerName">
        <el-input v-model="formData.targetCustomerName" class="!w-1/1" placeholder="请输入目标客户名称" />
      </el-form-item>
      <el-form-item label="关联入库单号" prop="receiptOrderNo">
        <el-input v-model="formData.receiptOrderNo" class="!w-1/1" placeholder="请输入关联入库单号" />
      </el-form-item>
      <el-form-item label="关联出库单号" prop="shipmentOrderNo">
        <el-input v-model="formData.shipmentOrderNo" class="!w-1/1" placeholder="请输入关联出库单号" />
      </el-form-item>
      <el-form-item label="越库状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入越库状态" />
      </el-form-item>
      <el-form-item label="总数量" prop="totalQuantity">
        <el-input-number v-model="formData.totalQuantity" class="!w-1/1" :controls="false" placeholder="请输入总数量" />
      </el-form-item>
      <el-form-item label="总金额" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入总金额" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="创建者名称" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建者名称" />
      </el-form-item>
      <el-form-item label="更新者名称" prop="updaterName">
        <el-input v-model="formData.updaterName" class="!w-1/1" placeholder="请输入更新者名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { WmsCrossDockOrderApi } from '@/api/wms/cross-dock-order'

defineOptions({ name: 'WmsCrossDockOrderForm' })

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
  sourceSupplierName: [required],
  targetCustomerName: [required],
  receiptOrderNo: [required],
  shipmentOrderNo: [required],
  remark: [required],
  creatorName: [required],
  updaterName: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await WmsCrossDockOrderApi.getWmsCrossDockOrder(id)
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
      await WmsCrossDockOrderApi.createWmsCrossDockOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await WmsCrossDockOrderApi.updateWmsCrossDockOrder(data)
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
