<!-- ERP 期末处理 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="年度" prop="year">
        <el-input-number v-model="formData.year" class="!w-1/1" :controls="false" placeholder="请输入年度" />
      </el-form-item>
      <el-form-item label="月份" prop="month">
        <el-input-number v-model="formData.month" class="!w-1/1" :controls="false" placeholder="请输入月份" />
      </el-form-item>
      <el-form-item label="期间编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入期间编码" />
      </el-form-item>
      <el-form-item label="起始日期" prop="startDate">
        <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择起始日期" />
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择结束日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="formData.statusName" class="!w-1/1" placeholder="请输入状态名称" />
      </el-form-item>
      <el-form-item label="关账人" prop="closedBy">
        <el-input v-model="formData.closedBy" class="!w-1/1" placeholder="请输入关账人" />
      </el-form-item>
      <el-form-item label="关账时间" prop="closedTime">
        <el-date-picker v-model="formData.closedTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择关账时间" />
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
import { ErpPeriodApi } from '@/api/erp/period'

defineOptions({ name: 'ErpPeriodForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  code: [required],
  statusName: [required],
  closedBy: [required],
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
      formData.value = await ErpPeriodApi.getErpPeriod(id)
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
      await ErpPeriodApi.createErpPeriod(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpPeriodApi.updateErpPeriod(data)
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
