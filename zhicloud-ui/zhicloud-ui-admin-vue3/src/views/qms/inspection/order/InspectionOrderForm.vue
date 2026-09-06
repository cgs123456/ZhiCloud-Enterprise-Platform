<!-- 检验单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="检验单号" prop="orderNo">
        <el-input v-model="formData.orderNo" class="!w-1/1" placeholder="请输入检验单号" />
      </el-form-item>
      <el-form-item label="检验类型" prop="type">
        <el-input-number v-model="formData.type" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验类型" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="formData.supplierId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="formData.batchNo" class="!w-1/1" placeholder="请输入批次号" />
      </el-form-item>
      <el-form-item label="工单 ID" prop="workOrderId">
        <el-input-number v-model="formData.workOrderId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入工单 ID" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="formData.productId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="检验员" prop="inspector">
        <el-input v-model="formData.inspector" class="!w-1/1" placeholder="请输入检验员" />
      </el-form-item>
      <el-form-item label="检验时间" prop="inspectTime">
        <el-date-picker v-model="formData.inspectTime" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择检验时间" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="AQL 接收数 Ac（缺陷数 <= Ac 判合格）" prop="acceptanceQuantity">
        <el-input-number v-model="formData.acceptanceQuantity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入AQL 接收数 Ac（缺陷数 <= Ac 判合格）" />
      </el-form-item>
      <el-form-item label="AQL 拒收数 Re（缺陷数 >= Re 判不合格）" prop="rejectQuantity">
        <el-input-number v-model="formData.rejectQuantity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入AQL 拒收数 Re（缺陷数 >= Re 判不合格）" />
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-input v-model="formData.bizType" class="!w-1/1" placeholder="请输入业务类型" />
      </el-form-item>
      <el-form-item label="业务单据 ID" prop="bizId">
        <el-input-number v-model="formData.bizId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入业务单据 ID" />
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
import { InspectionOrderApi } from '@/api/qms/inspection/order'

defineOptions({ name: 'InspectionOrderForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  type: [required],

  supplierId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await InspectionOrderApi.getInspectionOrder(id)
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
      await InspectionOrderApi.createInspectionOrder(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await InspectionOrderApi.updateInspectionOrder(formData.value)
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
