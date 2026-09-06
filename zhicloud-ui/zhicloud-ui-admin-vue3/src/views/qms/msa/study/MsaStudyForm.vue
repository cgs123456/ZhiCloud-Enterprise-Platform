<!-- MSA研究 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="研究编号" prop="studyNo">
        <el-input v-model="formData.studyNo" class="!w-1/1" placeholder="请输入研究编号" />
      </el-form-item>
      <el-form-item label="研究类型" prop="studyType">
        <el-input-number v-model="formData.studyType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入研究类型" />
      </el-form-item>
      <el-form-item label="特性名称" prop="characteristicName">
        <el-input v-model="formData.characteristicName" class="!w-1/1" placeholder="请输入特性名称" />
      </el-form-item>
      <el-form-item label="测量设备 ID" prop="equipmentId">
        <el-input-number v-model="formData.equipmentId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入测量设备 ID" />
      </el-form-item>
      <el-form-item label="评价人数量" prop="appraiserCount">
        <el-input-number v-model="formData.appraiserCount" class="!w-1/1" :min="0" :controls="false" placeholder="请输入评价人数量" />
      </el-form-item>
      <el-form-item label="试验次数" prop="trialCount">
        <el-input-number v-model="formData.trialCount" class="!w-1/1" :min="0" :controls="false" placeholder="请输入试验次数" />
      </el-form-item>
      <el-form-item label="零件数量" prop="partCount">
        <el-input-number v-model="formData.partCount" class="!w-1/1" :min="0" :controls="false" placeholder="请输入零件数量" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
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
import { MsaStudyApi } from '@/api/qms/msa/study'

defineOptions({ name: 'MsaStudyForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  studyType: [required],

  characteristicName: [required],

  equipmentId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await MsaStudyApi.getMsaStudy(id)
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
      await MsaStudyApi.createMsaStudy(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await MsaStudyApi.updateMsaStudy(formData.value)
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
