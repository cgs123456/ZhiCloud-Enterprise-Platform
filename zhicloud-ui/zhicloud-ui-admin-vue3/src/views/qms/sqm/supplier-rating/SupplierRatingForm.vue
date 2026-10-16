<!-- 供应商评级 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="评级编号" prop="ratingNo">
        <el-input v-model="formData.ratingNo" class="!w-1/1" placeholder="请输入评级编号" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="formData.supplierId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="formData.supplierName" class="!w-1/1" placeholder="请输入供应商名称" />
      </el-form-item>
      <el-form-item label="评级周期" prop="ratingPeriod">
        <el-input v-model="formData.ratingPeriod" class="!w-1/1" placeholder="请输入评级周期" />
      </el-form-item>
      <el-form-item label="PPM 缺陷率" prop="ppm">
        <el-input-number v-model="formData.ppm" class="!w-1/1" :min="0" :controls="false" placeholder="请输入PPM 缺陷率" />
      </el-form-item>
      <el-form-item label="交期达成率" prop="onTimeRate">
        <el-input-number v-model="formData.onTimeRate" class="!w-1/1" :min="0" :controls="false" placeholder="请输入交期达成率" />
      </el-form-item>
      <el-form-item label="质量合格率" prop="qualityRate">
        <el-input-number v-model="formData.qualityRate" class="!w-1/1" :min="0" :controls="false" placeholder="请输入质量合格率" />
      </el-form-item>
        <!-- grade 只读：等级必须与 ppm/准时率/质量率一致（§12.236）。改为只读回显，避免"填了不生效"的静默欺骗。 -->
      <el-form-item label="供应商等级" prop="grade">
        <dict-tag v-if="formType === 'update'" :type="DICT_TYPE.QMS_SUPPLIER_GRADE" :value="formData.grade" />
        <span v-else class="text-gray-400">按 ppm/准时率/质量率自动计算</span>
      </el-form-item>
      <el-form-item label="评级日期" prop="ratingDate">
        <el-date-picker v-model="formData.ratingDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择评级日期" />
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
import { SupplierRatingApi } from '@/api/qms/sqm/supplier-rating'
import { DICT_TYPE } from '@/utils/dict'

defineOptions({ name: 'SupplierRatingForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  supplierId: [required],

  supplierName: [required],

  ratingPeriod: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await SupplierRatingApi.getSupplierRating(id)
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
      await SupplierRatingApi.createSupplierRating(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await SupplierRatingApi.updateSupplierRating(formData.value)
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
