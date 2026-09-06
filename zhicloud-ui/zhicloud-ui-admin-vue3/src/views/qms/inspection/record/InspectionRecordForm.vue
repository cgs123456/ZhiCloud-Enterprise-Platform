<!-- 检验记录 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="检验单 ID" prop="orderId">
        <el-input-number v-model="formData.orderId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验单 ID" />
      </el-form-item>
      <el-form-item label="检验项目 ID" prop="itemId">
        <el-input-number v-model="formData.itemId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验项目 ID" />
      </el-form-item>
      <el-form-item label="实测值" prop="measuredValue">
        <el-input v-model="formData.measuredValue" class="!w-1/1" placeholder="请输入实测值" />
      </el-form-item>
      <el-form-item label="检验结果" prop="result">
        <el-input-number v-model="formData.result" class="!w-1/1" :min="0" :controls="false" placeholder="请输入检验结果" />
      </el-form-item>
      <el-form-item label="缺陷严重度" prop="severity">
        <el-input-number v-model="formData.severity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入缺陷严重度" />
      </el-form-item>
      <el-form-item label="检验员" prop="inspector">
        <el-input v-model="formData.inspector" class="!w-1/1" placeholder="请输入检验员" />
      </el-form-item>
      <el-form-item label="检验时间" prop="inspectTime">
        <el-date-picker v-model="formData.inspectTime" class="!w-1/1" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="请选择检验时间" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { InspectionRecordApi } from '@/api/qms/inspection/record'

defineOptions({ name: 'InspectionRecordForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  itemId: [required],

  measuredValue: [required],

  result: [required],

  severity: [required],

  inspector: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await InspectionRecordApi.getInspectionRecord(id)
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
      await InspectionRecordApi.createInspectionRecord(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await InspectionRecordApi.updateInspectionRecord(formData.value)
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
