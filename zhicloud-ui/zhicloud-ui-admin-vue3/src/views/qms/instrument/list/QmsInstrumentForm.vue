<!-- 计量器具台账 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="器具编号" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入器具编号" />
      </el-form-item>
      <el-form-item label="器具名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入器具名称" />
      </el-form-item>
      <el-form-item label="型号规格" prop="model">
        <el-input v-model="formData.model" class="!w-1/1" placeholder="请输入型号规格" />
      </el-form-item>
      <el-form-item label="生产厂家" prop="manufacturer">
        <el-input v-model="formData.manufacturer" class="!w-1/1" placeholder="请输入生产厂家" />
      </el-form-item>
      <el-form-item label="出厂编号" prop="serialNo">
        <el-input v-model="formData.serialNo" class="!w-1/1" placeholder="请输入出厂编号" />
      </el-form-item>
      <el-form-item label="类别" prop="category">
        <el-input-number v-model="formData.category" class="!w-1/1" :min="0" :controls="false" placeholder="请输入类别" />
      </el-form-item>
      <el-form-item label="精度等级" prop="accuracy">
        <el-input v-model="formData.accuracy" class="!w-1/1" placeholder="请输入精度等级" />
      </el-form-item>
      <el-form-item label="测量范围" prop="measureRange">
        <el-input v-model="formData.measureRange" class="!w-1/1" placeholder="请输入测量范围" />
      </el-form-item>
      <el-form-item label="计量单位" prop="unit">
        <el-input v-model="formData.unit" class="!w-1/1" placeholder="请输入计量单位" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="使用地点" prop="location">
        <el-input v-model="formData.location" class="!w-1/1" placeholder="请输入使用地点" />
      </el-form-item>
      <el-form-item label="负责人" prop="responsiblePerson">
        <el-input v-model="formData.responsiblePerson" class="!w-1/1" placeholder="请输入负责人" />
      </el-form-item>
      <el-form-item label="校准周期天数" prop="calibrationCycleDays">
        <el-input-number v-model="formData.calibrationCycleDays" class="!w-1/1" :min="0" :controls="false" placeholder="请输入校准周期天数" />
      </el-form-item>
      <el-form-item label="上次校准日期" prop="lastCalibrationDate">
        <el-date-picker v-model="formData.lastCalibrationDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择上次校准日期" />
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
import { QmsInstrumentApi } from '@/api/qms/instrument/list'

defineOptions({ name: 'QmsInstrumentForm' })

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

  model: [required],

  manufacturer: [required],

  accuracy: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsInstrumentApi.getQmsInstrument(id)
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
      await QmsInstrumentApi.createQmsInstrument(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsInstrumentApi.updateQmsInstrument(formData.value)
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
