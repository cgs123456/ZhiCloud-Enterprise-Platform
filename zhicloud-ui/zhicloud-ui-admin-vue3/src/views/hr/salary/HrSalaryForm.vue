<!-- HR 薪资 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="员工 ID" prop="employeeId">
        <el-input-number v-model="formData.employeeId" class="!w-1/1" :controls="false" placeholder="请输入员工 ID" />
      </el-form-item>
      <el-form-item label="薪资月份" prop="salaryMonth">
        <el-input v-model="formData.salaryMonth" class="!w-1/1" placeholder="请输入薪资月份" />
      </el-form-item>
      <el-form-item label="基本工资" prop="baseSalary">
        <el-input-number v-model="formData.baseSalary" class="!w-1/1" :controls="false" placeholder="请输入基本工资" />
      </el-form-item>
      <el-form-item label="加班费" prop="overtimePay">
        <el-input-number v-model="formData.overtimePay" class="!w-1/1" :controls="false" placeholder="请输入加班费" />
      </el-form-item>
      <el-form-item label="奖金" prop="bonus">
        <el-input-number v-model="formData.bonus" class="!w-1/1" :controls="false" placeholder="请输入奖金" />
      </el-form-item>
      <el-form-item label="扣款" prop="deduction">
        <el-input-number v-model="formData.deduction" class="!w-1/1" :controls="false" placeholder="请输入扣款" />
      </el-form-item>
      <el-form-item label="社保" prop="socialInsurance">
        <el-input-number v-model="formData.socialInsurance" class="!w-1/1" :controls="false" placeholder="请输入社保" />
      </el-form-item>
      <el-form-item label="公积金" prop="housingFund">
        <el-input-number v-model="formData.housingFund" class="!w-1/1" :controls="false" placeholder="请输入公积金" />
      </el-form-item>
      <el-form-item label="个税" prop="tax">
        <el-input-number v-model="formData.tax" class="!w-1/1" :controls="false" placeholder="请输入个税" />
      </el-form-item>
      <el-form-item label="实发工资" prop="netSalary">
        <el-input-number v-model="formData.netSalary" class="!w-1/1" :controls="false" placeholder="请输入实发工资" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
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
import { HrSalaryApi } from '@/api/hr/salary'

defineOptions({ name: 'HrSalaryForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  salaryMonth: [required],
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
      formData.value = await HrSalaryApi.getHrSalary(id)
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
      await HrSalaryApi.createHrSalary(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrSalaryApi.updateHrSalary(data)
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
