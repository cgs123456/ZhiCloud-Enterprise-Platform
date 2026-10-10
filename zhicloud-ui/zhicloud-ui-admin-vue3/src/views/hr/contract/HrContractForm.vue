<!-- HR 劳动合同 表单 -->
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
      <el-form-item label="合同编号" prop="contractNo">
        <el-input v-model="formData.contractNo" class="!w-1/1" placeholder="请输入合同编号" />
      </el-form-item>
      <el-form-item label="合同类型" prop="contractType">
        <el-input-number v-model="formData.contractType" class="!w-1/1" :controls="false" placeholder="请输入合同类型" />
      </el-form-item>
      <el-form-item label="合同开始日期" prop="startDate">
        <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择合同开始日期" />
      </el-form-item>
      <el-form-item label="合同结束日期" prop="endDate">
        <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择合同结束日期" />
      </el-form-item>
      <el-form-item label="签订日期" prop="signDate">
        <el-date-picker v-model="formData.signDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择签订日期" />
      </el-form-item>
      <el-form-item label="试用期结束日期" prop="probationEndDate">
        <el-date-picker v-model="formData.probationEndDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择试用期结束日期" />
      </el-form-item>
      <el-form-item label="岗位 ID" prop="positionId">
        <el-input-number v-model="formData.positionId" class="!w-1/1" :controls="false" placeholder="请输入岗位 ID" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="departmentId">
        <el-input-number v-model="formData.departmentId" class="!w-1/1" :controls="false" placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="合同约定薪资" prop="salary">
        <el-input-number v-model="formData.salary" class="!w-1/1" :controls="false" placeholder="请输入合同约定薪资" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="合同附件" prop="fileUrl">
        <el-input v-model="formData.fileUrl" class="!w-1/1" placeholder="请输入合同附件" />
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
import { HrContractApi } from '@/api/hr/contract'

defineOptions({ name: 'HrContractForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  contractNo: [required],
  fileUrl: [required],
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
      formData.value = await HrContractApi.getHrContract(id)
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
      await HrContractApi.createHrContract(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrContractApi.updateHrContract(data)
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
