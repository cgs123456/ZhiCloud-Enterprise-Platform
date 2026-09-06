<!-- FMEA分析项 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="FMEA 文档 ID" prop="fmeaId">
        <el-input-number v-model="formData.fmeaId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入FMEA 文档 ID" />
      </el-form-item>
      <el-form-item label="功能" prop="function">
        <el-input v-model="formData.function" class="!w-1/1" placeholder="请输入功能" />
      </el-form-item>
      <el-form-item label="失效模式" prop="failureMode">
        <el-input v-model="formData.failureMode" class="!w-1/1" placeholder="请输入失效模式" />
      </el-form-item>
      <el-form-item label="失效后果" prop="failureEffect">
        <el-input v-model="formData.failureEffect" class="!w-1/1" placeholder="请输入失效后果" />
      </el-form-item>
      <el-form-item label="严重度 S（1-10）" prop="severity">
        <el-input-number v-model="formData.severity" class="!w-1/1" :min="0" :controls="false" placeholder="请输入严重度 S（1-10）" />
      </el-form-item>
      <el-form-item label="潜在失效原因" prop="potentialCause">
        <el-input v-model="formData.potentialCause" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入潜在失效原因" />
      </el-form-item>
      <el-form-item label="频度 O（1-10）" prop="occurrence">
        <el-input-number v-model="formData.occurrence" class="!w-1/1" :min="0" :controls="false" placeholder="请输入频度 O（1-10）" />
      </el-form-item>
      <el-form-item label="现行控制措施" prop="currentControls">
        <el-input v-model="formData.currentControls" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入现行控制措施" />
      </el-form-item>
      <el-form-item label="探测度 D（1-10）" prop="detection">
        <el-input-number v-model="formData.detection" class="!w-1/1" :min="0" :controls="false" placeholder="请输入探测度 D（1-10）" />
      </el-form-item>
      <el-form-item label="建议措施" prop="actionRecommended">
        <el-input v-model="formData.actionRecommended" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入建议措施" />
      </el-form-item>
      <el-form-item label="已采取措施" prop="actionTaken">
        <el-input v-model="formData.actionTaken" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入已采取措施" />
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
import { FmeaItemApi } from '@/api/qms/fmea/item'

defineOptions({ name: 'FmeaItemForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  function: [required],

  failureMode: [required],

  failureEffect: [required],

  severity: [required],

  potentialCause: [required],

  occurrence: [required],

  currentControls: [required],

  detection: [required],

  actionRecommended: [required],

  actionTaken: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await FmeaItemApi.getFmeaItem(id)
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
      await FmeaItemApi.createFmeaItem(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await FmeaItemApi.updateFmeaItem(formData.value)
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
