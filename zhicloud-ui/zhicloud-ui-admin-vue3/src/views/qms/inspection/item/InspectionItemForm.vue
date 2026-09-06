<!-- 检验项目 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="检验项目编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入检验项目编码" />
      </el-form-item>
      <el-form-item label="检验项目名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入检验项目名称" />
      </el-form-item>
      <el-form-item label="检验类型" prop="type">
        <el-input-number v-model="formData.type" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验类型" />
      </el-form-item>
      <el-form-item label="检验方法" prop="method">
        <el-input-number v-model="formData.method" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验方法" />
      </el-form-item>
      <el-form-item label="检验标准" prop="standard">
        <el-input v-model="formData.standard" class="!w-1/1" placeholder="请输入检验标准" />
      </el-form-item>
      <el-form-item label="目标值" prop="target">
        <el-input v-model="formData.target" class="!w-1/1" placeholder="请输入目标值" />
      </el-form-item>
      <el-form-item label="上限" prop="upperLimit">
        <el-input-number v-model="formData.upperLimit" class="!w-1/1" :min="0" :controls="false" placeholder="请输入上限" />
      </el-form-item>
      <el-form-item label="下限" prop="lowerLimit">
        <el-input-number v-model="formData.lowerLimit" class="!w-1/1" :min="0" :controls="false" placeholder="请输入下限" />
      </el-form-item>
      <el-form-item label="单位" prop="unit">
        <el-input v-model="formData.unit" class="!w-1/1" placeholder="请输入单位" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { InspectionItemApi } from '@/api/qms/inspection/item'

defineOptions({ name: 'InspectionItemForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  name: [required],

  type: [required],

  method: [required],

  standard: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await InspectionItemApi.getInspectionItem(id)
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
    if (formType.value === 'create') {
      await InspectionItemApi.createInspectionItem(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await InspectionItemApi.updateInspectionItem(formData.value)
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
