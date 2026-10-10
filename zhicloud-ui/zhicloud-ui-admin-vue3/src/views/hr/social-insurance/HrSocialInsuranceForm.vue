<!-- HR 社保 表单 -->
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
      <el-form-item label="年份" prop="year">
        <el-input-number v-model="formData.year" class="!w-1/1" :controls="false" placeholder="请输入年份" />
      </el-form-item>
      <el-form-item label="养老基数" prop="pensionBase">
        <el-input-number v-model="formData.pensionBase" class="!w-1/1" :controls="false" placeholder="请输入养老基数" />
      </el-form-item>
      <el-form-item label="医疗基数" prop="medicalBase">
        <el-input-number v-model="formData.medicalBase" class="!w-1/1" :controls="false" placeholder="请输入医疗基数" />
      </el-form-item>
      <el-form-item label="失业基数" prop="unemploymentBase">
        <el-input-number v-model="formData.unemploymentBase" class="!w-1/1" :controls="false" placeholder="请输入失业基数" />
      </el-form-item>
      <el-form-item label="工伤基数" prop="workInjuryBase">
        <el-input-number v-model="formData.workInjuryBase" class="!w-1/1" :controls="false" placeholder="请输入工伤基数" />
      </el-form-item>
      <el-form-item label="生育基数" prop="maternityBase">
        <el-input-number v-model="formData.maternityBase" class="!w-1/1" :controls="false" placeholder="请输入生育基数" />
      </el-form-item>
      <el-form-item label="公积金基数" prop="housingFundBase">
        <el-input-number v-model="formData.housingFundBase" class="!w-1/1" :controls="false" placeholder="请输入公积金基数" />
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
import { HrSocialInsuranceApi } from '@/api/hr/social-insurance'

defineOptions({ name: 'HrSocialInsuranceForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
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
      formData.value = await HrSocialInsuranceApi.getHrSocialInsurance(id)
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
      await HrSocialInsuranceApi.createHrSocialInsurance(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrSocialInsuranceApi.updateHrSocialInsurance(data)
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
