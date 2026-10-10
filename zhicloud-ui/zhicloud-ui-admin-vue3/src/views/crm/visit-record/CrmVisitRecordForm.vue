<!-- CRM 拜访签到 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="客户编号" prop="customerId">
        <el-input-number v-model="formData.customerId" class="!w-1/1" :controls="false" placeholder="请输入客户编号" />
      </el-form-item>
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="formData.customerName" class="!w-1/1" placeholder="请输入客户名称" />
      </el-form-item>
      <el-form-item label="联系人编号" prop="contactId">
        <el-input-number v-model="formData.contactId" class="!w-1/1" :controls="false" placeholder="请输入联系人编号" />
      </el-form-item>
      <el-form-item label="联系人名称" prop="contactName">
        <el-input v-model="formData.contactName" class="!w-1/1" placeholder="请输入联系人名称" />
      </el-form-item>
      <el-form-item label="负责人的用户编号" prop="ownerUserId">
        <el-input-number v-model="formData.ownerUserId" class="!w-1/1" :controls="false" placeholder="请输入负责人的用户编号" />
      </el-form-item>
      <el-form-item label="负责人名字" prop="ownerUserName">
        <el-input v-model="formData.ownerUserName" class="!w-1/1" placeholder="请输入负责人名字" />
      </el-form-item>
      <el-form-item label="负责人部门" prop="ownerUserDeptName">
        <el-input v-model="formData.ownerUserDeptName" class="!w-1/1" placeholder="请输入负责人部门" />
      </el-form-item>
      <el-form-item label="拜访时间" prop="visitTime">
        <el-date-picker v-model="formData.visitTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择拜访时间" />
      </el-form-item>
      <el-form-item label="签到时间" prop="signInTime">
        <el-date-picker v-model="formData.signInTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择签到时间" />
      </el-form-item>
      <el-form-item label="签到纬度" prop="signInLatitude">
        <el-input-number v-model="formData.signInLatitude" class="!w-1/1" :controls="false" placeholder="请输入签到纬度" />
      </el-form-item>
      <el-form-item label="签到经度" prop="signInLongitude">
        <el-input-number v-model="formData.signInLongitude" class="!w-1/1" :controls="false" placeholder="请输入签到经度" />
      </el-form-item>
      <el-form-item label="签到地址" prop="signInAddress">
        <el-input v-model="formData.signInAddress" class="!w-1/1" placeholder="请输入签到地址" />
      </el-form-item>
      <el-form-item label="拜访类型" prop="visitType">
        <el-input-number v-model="formData.visitType" class="!w-1/1" :controls="false" placeholder="请输入拜访类型" />
      </el-form-item>
      <el-form-item label="拜访内容" prop="content">
        <el-input v-model="formData.content" class="!w-1/1" placeholder="请输入拜访内容" />
      </el-form-item>
      <el-form-item label="创建人名字" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建人名字" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { CrmVisitRecordApi } from '@/api/crm/visit-record'

defineOptions({ name: 'CrmVisitRecordForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  customerName: [required],
  contactName: [required],
  ownerUserName: [required],
  ownerUserDeptName: [required],
  signInAddress: [required],
  content: [required],
  creatorName: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CrmVisitRecordApi.getCrmVisitRecord(id)
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
      await CrmVisitRecordApi.createCrmVisitRecord(data)
      message.success(t('common.createSuccess'))
    } else {
      await CrmVisitRecordApi.updateCrmVisitRecord(data)
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
