<!-- TMS 车队运营记录表单（totalCost/profit 只读：服务端重算） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number
          v-model="formData.vehicleId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入车辆编号"
        />
      </el-form-item>
      <el-form-item label="运营日期" prop="operationDate">
        <el-date-picker
          v-model="formData.operationDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择运营日期"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="行驶里程" prop="mileage">
        <el-input-number v-model="formData.mileage" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入行驶里程（公里）" />
      </el-form-item>
      <el-form-item label="油耗" prop="fuelConsumption">
        <el-input-number v-model="formData.fuelConsumption" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入油耗（升）" />
      </el-form-item>
      <el-form-item label="油费" prop="fuelCost">
        <el-input-number v-model="formData.fuelCost" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入油费（元）" />
      </el-form-item>
      <el-form-item label="维修保养费" prop="maintenanceCost">
        <el-input-number v-model="formData.maintenanceCost" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入维修保养费（元）" />
      </el-form-item>
      <el-form-item label="保险费" prop="insuranceCost">
        <el-input-number v-model="formData.insuranceCost" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入保险费（元）" />
      </el-form-item>
      <el-form-item label="年检费" prop="inspectionCost">
        <el-input-number v-model="formData.inspectionCost" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入年检费（元）" />
      </el-form-item>
      <el-form-item label="运营收入" prop="revenue">
        <el-input-number v-model="formData.revenue" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入运营收入（元）" />
      </el-form-item>
      <el-form-item label="总运营成本">
        <el-input :model-value="previewTotalCost" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="运营利润">
        <el-input :model-value="previewProfit" disabled class="!w-1/1" />
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
import { TmsFleetOperationApi } from '@/api/tms/fleet-operation'
import type { TmsFleetOperationVO } from '@/api/tms/fleet-operation'

defineOptions({ name: 'TmsFleetOperationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<TmsFleetOperationVO>({})
const formRules = reactive<FormRules>({
  vehicleId: [{ required: true, message: '车辆编号不能为空', trigger: 'blur' }],
  operationDate: [{ required: true, message: '运营日期不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 成本/利润预览（与服务端口径一致：成本为四项之和、利润为收入减成本；null 按 0） */
const previewTotalCost = computed(() => {
  const d = formData.value
  return (
    (d.fuelCost ?? 0) +
    (d.maintenanceCost ?? 0) +
    (d.insuranceCost ?? 0) +
    (d.inspectionCost ?? 0)
  ).toFixed(2)
})
const previewProfit = computed(() => ((formData.value.revenue ?? 0) - Number(previewTotalCost.value)).toFixed(2))

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = {}
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TmsFleetOperationApi.getTmsFleetOperation(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单（不提交 totalCost/profit：服务端重算覆盖） */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: TmsFleetOperationVO = {
      id: formData.value.id,
      vehicleId: formData.value.vehicleId,
      operationDate: formData.value.operationDate,
      mileage: formData.value.mileage,
      fuelConsumption: formData.value.fuelConsumption,
      fuelCost: formData.value.fuelCost,
      maintenanceCost: formData.value.maintenanceCost,
      insuranceCost: formData.value.insuranceCost,
      inspectionCost: formData.value.inspectionCost,
      revenue: formData.value.revenue,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await TmsFleetOperationApi.updateTmsFleetOperation(data)
      message.success(t('common.updateSuccess'))
    } else {
      await TmsFleetOperationApi.createTmsFleetOperation(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
