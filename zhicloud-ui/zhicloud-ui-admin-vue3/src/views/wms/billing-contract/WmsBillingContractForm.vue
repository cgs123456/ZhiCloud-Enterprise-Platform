<!-- WMS 计费合同 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="合同号" prop="contractNo">
        <el-input v-model="formData.contractNo" class="!w-1/1" placeholder="请输入合同号" />
      </el-form-item>
      <el-form-item label="货主编号" prop="ownerId">
        <el-input-number v-model="formData.ownerId" class="!w-1/1" :controls="false" placeholder="请输入货主编号" />
      </el-form-item>
      <el-form-item label="货主名称" prop="ownerName">
        <el-input v-model="formData.ownerName" class="!w-1/1" placeholder="请输入货主名称" />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input v-model="formData.contractName" class="!w-1/1" placeholder="请输入合同名称" />
      </el-form-item>
      <el-form-item label="生效日期" prop="startDate">
        <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择生效日期" />
      </el-form-item>
      <el-form-item label="失效日期" prop="endDate">
        <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择失效日期" />
      </el-form-item>
      <el-form-item label="合同状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入合同状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="创建者名称" prop="creatorName">
        <el-input v-model="formData.creatorName" class="!w-1/1" placeholder="请输入创建者名称" />
      </el-form-item>
      <el-form-item label="总金额（仅用于展示）" prop="totalAmount">
        <el-input-number v-model="formData.totalAmount" class="!w-1/1" :controls="false" placeholder="请输入总金额（仅用于展示）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { WmsBillingContractApi } from '@/api/wms/billing-contract'

defineOptions({ name: 'WmsBillingContractForm' })

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
  ownerName: [required],
  contractName: [required],
  remark: [required],
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
      formData.value = await WmsBillingContractApi.getWmsBillingContract(id)
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
      await WmsBillingContractApi.createWmsBillingContract(data)
      message.success(t('common.createSuccess'))
    } else {
      await WmsBillingContractApi.updateWmsBillingContract(data)
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
