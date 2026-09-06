<!-- 器具校准记录 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="器具 ID" prop="instrumentId">
        <el-input-number v-model="formData.instrumentId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入器具 ID" />
      </el-form-item>
      <el-form-item label="校准证书编号" prop="calibrationNo">
        <el-input v-model="formData.calibrationNo" class="!w-1/1" placeholder="请输入校准证书编号" />
      </el-form-item>
      <el-form-item label="校准日期" prop="calibrationDate">
        <el-date-picker v-model="formData.calibrationDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择校准日期" />
      </el-form-item>
      <el-form-item label="校准机构" prop="calibrationOrganization">
        <el-input v-model="formData.calibrationOrganization" class="!w-1/1" placeholder="请输入校准机构" />
      </el-form-item>
      <el-form-item label="校准结果" prop="calibrationResult">
        <el-input-number v-model="formData.calibrationResult" class="!w-1/1" :min="0" :controls="false" placeholder="请输入校准结果" />
      </el-form-item>
      <el-form-item label="校准证书附件 URL" prop="calibrationCertificateUrl">
        <el-input v-model="formData.calibrationCertificateUrl" class="!w-1/1" placeholder="请输入校准证书附件 URL" />
      </el-form-item>
      <el-form-item label="偏差值" prop="deviation">
        <el-input-number v-model="formData.deviation" class="!w-1/1" :min="0" :controls="false" placeholder="请输入偏差值" />
      </el-form-item>
      <el-form-item label="下次校准日期" prop="nextCalibrationDate">
        <el-date-picker v-model="formData.nextCalibrationDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择下次校准日期" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :min="0" :controls="false" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { QmsInstrumentCalibrationApi } from '@/api/qms/instrument/calibration'

defineOptions({ name: 'QmsInstrumentCalibrationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  calibrationNo: [required],

  calibrationDate: [required],

  calibrationOrganization: [required],

  calibrationCertificateUrl: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsInstrumentCalibrationApi.getQmsInstrumentCalibration(id)
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
      await QmsInstrumentCalibrationApi.createQmsInstrumentCalibration(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsInstrumentCalibrationApi.updateQmsInstrumentCalibration(formData.value)
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
