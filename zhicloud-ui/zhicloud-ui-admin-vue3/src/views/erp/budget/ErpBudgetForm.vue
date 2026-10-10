<!-- ERP 预算管理 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="预算编号" prop="budgetNo">
        <el-input v-model="formData.budgetNo" class="!w-1/1" placeholder="请输入预算编号" />
      </el-form-item>
      <el-form-item label="预算年度" prop="budgetYear">
        <el-input-number v-model="formData.budgetYear" class="!w-1/1" :controls="false" placeholder="请输入预算年度" />
      </el-form-item>
      <el-form-item label="会计期间编号" prop="periodId">
        <el-input-number v-model="formData.periodId" class="!w-1/1" :controls="false" placeholder="请输入会计期间编号" />
      </el-form-item>
      <el-form-item label="期间编码" prop="periodCode">
        <el-input v-model="formData.periodCode" class="!w-1/1" placeholder="请输入期间编码" />
      </el-form-item>
      <el-form-item label="部门编号" prop="departmentId">
        <el-input-number v-model="formData.departmentId" class="!w-1/1" :controls="false" placeholder="请输入部门编号" />
      </el-form-item>
      <el-form-item label="预算类型" prop="budgetType">
        <el-input-number v-model="formData.budgetType" class="!w-1/1" :controls="false" placeholder="请输入预算类型" />
      </el-form-item>
      <el-form-item label="预算类型名称" prop="budgetTypeName">
        <el-input v-model="formData.budgetTypeName" class="!w-1/1" placeholder="请输入预算类型名称" />
      </el-form-item>
      <el-form-item label="预算总额" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入预算总额" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="formData.statusName" class="!w-1/1" placeholder="请输入状态名称" />
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
import { ErpBudgetApi } from '@/api/erp/budget'

defineOptions({ name: 'ErpBudgetForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  budgetNo: [required],
  periodCode: [required],
  budgetTypeName: [required],
  statusName: [required],
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
      formData.value = await ErpBudgetApi.getErpBudget(id)
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
      await ErpBudgetApi.createErpBudget(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpBudgetApi.updateErpBudget(data)
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
