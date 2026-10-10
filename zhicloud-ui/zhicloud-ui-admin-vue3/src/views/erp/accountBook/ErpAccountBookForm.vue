<!-- ERP 账簿管理 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="账簿编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入账簿编码" />
      </el-form-item>
      <el-form-item label="账簿名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入账簿名称" />
      </el-form-item>
      <el-form-item label="会计准则" prop="accountingStandard">
        <el-input-number v-model="formData.accountingStandard" class="!w-1/1" :controls="false" placeholder="请输入会计准则" />
      </el-form-item>
      <el-form-item label="会计准则名称" prop="accountingStandardName">
        <el-input v-model="formData.accountingStandardName" class="!w-1/1" placeholder="请输入会计准则名称" />
      </el-form-item>
      <el-form-item label="本位币编号" prop="currencyId">
        <el-input-number v-model="formData.currencyId" class="!w-1/1" :controls="false" placeholder="请输入本位币编号" />
      </el-form-item>
      <el-form-item label="是否主账簿" prop="isPrimary">
        <el-switch v-model="formData.isPrimary" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="状态名称" prop="statusName">
        <el-input v-model="formData.statusName" class="!w-1/1" placeholder="请输入状态名称" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
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
import { ErpAccountBookApi } from '@/api/erp/account-book'

defineOptions({ name: 'ErpAccountBookForm' })

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
  name: [required],
  accountingStandardName: [required],
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
      formData.value = await ErpAccountBookApi.getErpAccountBook(id)
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
      await ErpAccountBookApi.createErpAccountBook(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpAccountBookApi.updateErpAccountBook(data)
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
