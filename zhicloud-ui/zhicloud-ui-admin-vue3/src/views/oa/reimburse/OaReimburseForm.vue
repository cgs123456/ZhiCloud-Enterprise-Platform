<!-- OA 报销单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="报销单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入报销单号" />
      </el-form-item>
      <el-form-item label="报销主题" prop="reimburseName">
        <el-input v-model="formData.reimburseName" class="!w-1/1" placeholder="请输入报销主题" />
      </el-form-item>
      <el-form-item label="申请人 ID" prop="applicantUserId">
        <el-input-number v-model="formData.applicantUserId" class="!w-1/1" :controls="false" placeholder="请输入申请人 ID" />
      </el-form-item>
      <el-form-item label="部门 ID" prop="deptId">
        <el-input-number v-model="formData.deptId" class="!w-1/1" :controls="false" placeholder="请输入部门 ID" />
      </el-form-item>
      <el-form-item label="报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）" prop="reimburseType">
        <el-input-number v-model="formData.reimburseType" class="!w-1/1" :controls="false" placeholder="请输入报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）" />
      </el-form-item>
      <el-form-item label="报销日期" prop="reimburseDate">
        <el-date-picker v-model="formData.reimburseDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择报销日期" />
      </el-form-item>
      <el-form-item label="报销总额" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入报销总额" />
      </el-form-item>
      <el-form-item label="支付状态（10 未支付 20 部分支付 30 已支付）" prop="paymentStatus">
        <el-input-number v-model="formData.paymentStatus" class="!w-1/1" :controls="false" placeholder="请输入支付状态（10 未支付 20 部分支付 30 已支付）" />
      </el-form-item>
      <el-form-item label="工作流编号" prop="processInstanceId">
        <el-input v-model="formData.processInstanceId" class="!w-1/1" placeholder="请输入工作流编号" />
      </el-form-item>
      <el-form-item label="状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）" />
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
import { OaReimburseApi } from '@/api/oa/reimburse'

defineOptions({ name: 'OaReimburseForm' })

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
  reimburseName: [required],
  processInstanceId: [required],
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
      formData.value = await OaReimburseApi.getOaReimburse(id)
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
      await OaReimburseApi.createOaReimburse(data)
      message.success(t('common.createSuccess'))
    } else {
      await OaReimburseApi.updateOaReimburse(data)
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
