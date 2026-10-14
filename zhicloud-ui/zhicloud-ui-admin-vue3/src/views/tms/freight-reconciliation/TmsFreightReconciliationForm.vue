<!-- TMS 运费对账表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item label="对账单号" prop="no">
        <el-input v-model="formData.no" maxlength="64" placeholder="请输入对账单号" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="承运商" prop="carrierId">
        <el-input-number
          v-model="formData.carrierId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入承运商编号"
        />
      </el-form-item>
      <el-form-item label="对账周期" prop="periodRange">
        <el-date-picker
          v-model="periodRange"
          type="daterange"
          range-separator="~"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="系统金额" prop="systemAmount">
        <el-input-number
          v-model="formData.systemAmount"
          :controls="false"
          :precision="2"
          class="!w-1/1"
          placeholder="请输入系统金额"
        />
      </el-form-item>
      <el-form-item label="承运商金额" prop="carrierAmount">
        <el-input-number
          v-model="formData.carrierAmount"
          :controls="false"
          :precision="2"
          class="!w-1/1"
          placeholder="请输入承运商金额"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="512" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { TmsFreightReconciliationApi } from '@/api/tms/freight-reconciliation'
import type { TmsFreightReconciliationVO } from '@/api/tms/freight-reconciliation'

defineOptions({ name: 'TmsFreightReconciliationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<TmsFreightReconciliationVO>({})
/** 对账周期（daterange，提交时拆成 periodStart/periodEnd） */
const periodRange = ref<[string, string]>()
const formRules = reactive<FormRules>({
  no: [{ required: true, message: '对账单号不能为空', trigger: 'blur' }],
  carrierId: [{ required: true, message: '承运商不能为空', trigger: 'blur' }],
  periodRange: [{ required: true, message: '对账周期不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = {}
  periodRange.value = undefined
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      const detail = await TmsFreightReconciliationApi.getTmsFreightReconciliation(id)
      formData.value = detail
      if (detail.periodStart && detail.periodEnd) {
        periodRange.value = [detail.periodStart, detail.periodEnd] as unknown as [string, string]
      }
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单（status/diffAmount 由服务端管理，不提交） */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: TmsFreightReconciliationVO = {
      id: formData.value.id,
      no: formData.value.no,
      carrierId: formData.value.carrierId,
      periodStart: periodRange.value?.[0] as unknown as Date,
      periodEnd: periodRange.value?.[1] as unknown as Date,
      systemAmount: formData.value.systemAmount,
      carrierAmount: formData.value.carrierAmount,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await TmsFreightReconciliationApi.updateTmsFreightReconciliation(data)
      message.success(t('common.updateSuccess'))
    } else {
      await TmsFreightReconciliationApi.createTmsFreightReconciliation(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
