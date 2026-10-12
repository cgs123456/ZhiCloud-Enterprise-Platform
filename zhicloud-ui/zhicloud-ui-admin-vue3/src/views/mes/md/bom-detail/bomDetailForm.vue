<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="bomId" prop="bomId">
        <el-input-number v-model="formData.bomId" placeholder="请输入bomId" />
      </el-form-item>
      <el-form-item label="productId" prop="productId">
        <el-input-number v-model="formData.productId" placeholder="请输入productId" />
      </el-form-item>
      <el-form-item label="quantity" prop="quantity">
        <el-input-number v-model="formData.quantity" placeholder="请输入quantity" />
      </el-form-item>
      <el-form-item label="unit" prop="unit">
        <el-input v-model="formData.unit" placeholder="请输入unit" />
      </el-form-item>
      <el-form-item label="scrapRate" prop="scrapRate">
        <el-input-number v-model="formData.scrapRate" placeholder="请输入scrapRate" />
      </el-form-item>
      <el-form-item label="unitCost" prop="unitCost">
        <el-input-number v-model="formData.unitCost" placeholder="请输入unitCost" />
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
import { BomDetailApi } from '@/api/mes/md/bom-detail'

defineOptions({ name: 'BomDetailForm' })

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
      formData.value = await BomDetailApi.getBomDetail(id)
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
      await BomDetailApi.createBomDetail(data)
      message.success(t('common.createSuccess'))
    } else {
      await BomDetailApi.updateBomDetail(data)
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
