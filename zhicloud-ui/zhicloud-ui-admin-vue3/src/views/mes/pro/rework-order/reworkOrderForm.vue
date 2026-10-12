<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码" />
      </el-form-item>
      <el-form-item label="原工单" prop="originalWorkOrderId">
        <el-input-number v-model="formData.originalWorkOrderId" placeholder="请输入原工单" />
      </el-form-item>
      <el-form-item label="原工单编号" prop="originalWorkOrderCode">
        <el-input v-model="formData.originalWorkOrderCode" placeholder="请输入原工单编号" />
      </el-form-item>
      <el-form-item label="产品" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入产品" />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="formData.productCode" placeholder="请输入产品编码" />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="formData.productName" placeholder="请输入产品名称" />
      </el-form-item>
      <el-form-item label="返工数量" prop="reworkQuantity">
        <el-input-number v-model="formData.reworkQuantity" placeholder="请输入返工数量" />
      </el-form-item>
      <el-form-item label="返工原因" prop="reworkReason">
        <el-input v-model="formData.reworkReason" placeholder="请输入返工原因" />
      </el-form-item>
      <el-form-item label="返工类型" prop="reworkType">
        <el-input-number v-model="formData.reworkType" placeholder="请输入返工类型" />
      </el-form-item>
      <el-form-item label="返工工序" prop="reworkProcessId">
        <el-input-number v-model="formData.reworkProcessId" placeholder="请输入返工工序" />
      </el-form-item>
      <el-form-item label="返工工序名称" prop="reworkProcessName">
        <el-input v-model="formData.reworkProcessName" placeholder="请输入返工工序名称" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="负责人" prop="responsiblePersonId">
        <el-input-number v-model="formData.responsiblePersonId" placeholder="请输入负责人" />
      </el-form-item>
      <el-form-item label="负责部门" prop="responsibleDeptId">
        <el-input-number v-model="formData.responsibleDeptId" placeholder="请输入负责部门" />
      </el-form-item>
      <el-form-item label="计划开始" prop="plannedStartTime">
        <el-date-picker v-model="formData.plannedStartTime" placeholder="请输入计划开始" />
      </el-form-item>
      <el-form-item label="计划结束" prop="plannedEndTime">
        <el-date-picker v-model="formData.plannedEndTime" placeholder="请输入计划结束" />
      </el-form-item>
      <el-form-item label="实际开始" prop="actualStartTime">
        <el-date-picker v-model="formData.actualStartTime" placeholder="请输入实际开始" />
      </el-form-item>
      <el-form-item label="实际结束" prop="actualEndTime">
        <el-date-picker v-model="formData.actualEndTime" placeholder="请输入实际结束" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" placeholder="请输入排序" />
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
import { ReworkOrderApi } from '@/api/mes/pro/rework-order'

defineOptions({ name: 'ReworkOrderForm' })

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
      formData.value = await ReworkOrderApi.getReworkOrder(id)
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
      await ReworkOrderApi.createReworkOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      await ReworkOrderApi.updateReworkOrder(data)
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
