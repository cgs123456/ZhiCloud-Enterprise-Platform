<!-- 质量成本 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="成本类型" prop="costType">
        <el-input v-model="formData.costType" class="!w-1/1" placeholder="请输入成本类型" />
      </el-form-item>
      <el-form-item label="成本类别" prop="costCategory">
        <el-input v-model="formData.costCategory" class="!w-1/1" placeholder="请输入成本类别" />
      </el-form-item>
      <el-form-item label="成本项目" prop="costItem">
        <el-input v-model="formData.costItem" class="!w-1/1" placeholder="请输入成本项目" />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input-number v-model="formData.amount" class="!w-1/1" :min="0" :controls="false" placeholder="请输入金额" />
      </el-form-item>
      <el-form-item label="年度" prop="periodYear">
        <el-input-number v-model="formData.periodYear" class="!w-1/1" :min="0" :controls="false" placeholder="请输入年度" />
      </el-form-item>
      <el-form-item label="月份（1-12）" prop="periodMonth">
        <el-input-number v-model="formData.periodMonth" class="!w-1/1" :min="0" :controls="false" placeholder="请输入月份（1-12）" />
      </el-form-item>
      <el-form-item label="关联业务 ID（如 8D 报告 ID/NCR ID/CAPA ID）" prop="relatedId">
        <el-input-number v-model="formData.relatedId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入关联业务 ID（如 8D 报告 ID/NCR ID/CAPA ID）" />
      </el-form-item>
      <el-form-item label="关联业务类型（EIGHT_D/NCR/CAPA）" prop="relatedType">
        <el-input v-model="formData.relatedType" class="!w-1/1" placeholder="请输入关联业务类型（EIGHT_D/NCR/CAPA）" />
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
import { QmsQualityCostApi } from '@/api/qms/quality-cost'

defineOptions({ name: 'QmsQualityCostForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  costCategory: [required],

  costItem: [required],

  amount: [required],

  periodYear: [required],

  periodMonth: [required],

  relatedId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsQualityCostApi.getQmsQualityCost(id)
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
      await QmsQualityCostApi.createQmsQualityCost(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsQualityCostApi.updateQmsQualityCost(formData.value)
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
