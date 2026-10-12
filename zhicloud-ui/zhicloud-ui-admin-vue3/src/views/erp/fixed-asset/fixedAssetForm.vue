<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="category" prop="category">
        <el-input v-model="formData.category" placeholder="请输入category" />
      </el-form-item>
      <el-form-item label="specification" prop="specification">
        <el-input v-model="formData.specification" placeholder="请输入specification" />
      </el-form-item>
      <el-form-item label="departmentId" prop="departmentId">
        <el-input-number v-model="formData.departmentId" placeholder="请输入departmentId" />
      </el-form-item>
      <el-form-item label="location" prop="location">
        <el-input v-model="formData.location" placeholder="请输入location" />
      </el-form-item>
      <el-form-item label="responsiblePerson" prop="responsiblePerson">
        <el-input v-model="formData.responsiblePerson" placeholder="请输入responsiblePerson" />
      </el-form-item>
      <el-form-item label="originalValue" prop="originalValue">
        <el-input-number v-model="formData.originalValue" placeholder="请输入originalValue" />
      </el-form-item>
      <el-form-item label="salvageValue" prop="salvageValue">
        <el-input-number v-model="formData.salvageValue" placeholder="请输入salvageValue" />
      </el-form-item>
      <el-form-item label="usefulLifeMonths" prop="usefulLifeMonths">
        <el-input-number v-model="formData.usefulLifeMonths" placeholder="请输入usefulLifeMonths" />
      </el-form-item>
      <el-form-item label="depreciationMethod" prop="depreciationMethod">
        <el-input-number v-model="formData.depreciationMethod" placeholder="请输入depreciationMethod" />
      </el-form-item>
      <el-form-item label="capitalizationDate" prop="capitalizationDate">
        <el-input-number v-model="formData.capitalizationDate" placeholder="请输入capitalizationDate" />
      </el-form-item>
      <el-form-item label="assetAccountId" prop="assetAccountId">
        <el-input-number v-model="formData.assetAccountId" placeholder="请输入assetAccountId" />
      </el-form-item>
      <el-form-item label="accumulatedDepreciationAccountId" prop="accumulatedDepreciationAccountId">
        <el-input-number v-model="formData.accumulatedDepreciationAccountId" placeholder="请输入accumulatedDepreciationAccountId" />
      </el-form-item>
      <el-form-item label="depreciationExpenseAccountId" prop="depreciationExpenseAccountId">
        <el-input-number v-model="formData.depreciationExpenseAccountId" placeholder="请输入depreciationExpenseAccountId" />
      </el-form-item>
      <el-form-item label="depreciatedMonths" prop="depreciatedMonths">
        <el-input-number v-model="formData.depreciatedMonths" placeholder="请输入depreciatedMonths" />
      </el-form-item>
      <el-form-item label="accumulatedDepreciation" prop="accumulatedDepreciation">
        <el-input-number v-model="formData.accumulatedDepreciation" placeholder="请输入accumulatedDepreciation" />
      </el-form-item>
      <el-form-item label="netBookValue" prop="netBookValue">
        <el-input-number v-model="formData.netBookValue" placeholder="请输入netBookValue" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { FixedAssetApi } from '@/api/erp/fixed-asset'

defineOptions({ name: 'FixedAssetForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<any>({})
const formRules = reactive({})
const formRef = ref()

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await FixedAssetApi.getFixedAsset(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await FixedAssetApi.createFixedAsset(data)
      message.success(t('common.createSuccess'))
    } else {
      await FixedAssetApi.updateFixedAsset(data)
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
