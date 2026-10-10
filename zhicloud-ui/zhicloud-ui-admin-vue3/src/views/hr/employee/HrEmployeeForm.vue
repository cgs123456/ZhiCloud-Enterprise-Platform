<!-- HR 员工 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="工号" prop="empNo">
        <el-input v-model="formData.empNo" class="!w-1/1" placeholder="请输入工号" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-input-number v-model="formData.gender" class="!w-1/1" :controls="false" placeholder="请输入性别" />
      </el-form-item>
      <el-form-item label="出生日期" prop="birthDate">
        <el-date-picker v-model="formData.birthDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择出生日期" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard">
        <el-input v-model="formData.idCard" class="!w-1/1" placeholder="请输入身份证号" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="formData.phone" class="!w-1/1" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" class="!w-1/1" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="deptId">
        <el-input-number v-model="formData.deptId" class="!w-1/1" :controls="false" placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="职位 ID" prop="positionId">
        <el-input-number v-model="formData.positionId" class="!w-1/1" :controls="false" placeholder="请输入职位 ID" />
      </el-form-item>
      <el-form-item label="入职日期" prop="hireDate">
        <el-date-picker v-model="formData.hireDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择入职日期" />
      </el-form-item>
      <el-form-item label="离职日期" prop="leaveDate">
        <el-date-picker v-model="formData.leaveDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择离职日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="用工类型" prop="employmentType">
        <el-input-number v-model="formData.employmentType" class="!w-1/1" :controls="false" placeholder="请输入用工类型" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrEmployeeApi } from '@/api/hr/employee'

defineOptions({ name: 'HrEmployeeForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  empNo: [required],
  name: [required],
  idCard: [required],
  phone: [required],
  email: [required],
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
      formData.value = await HrEmployeeApi.getHrEmployee(id)
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
      await HrEmployeeApi.createHrEmployee(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrEmployeeApi.updateHrEmployee(data)
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
