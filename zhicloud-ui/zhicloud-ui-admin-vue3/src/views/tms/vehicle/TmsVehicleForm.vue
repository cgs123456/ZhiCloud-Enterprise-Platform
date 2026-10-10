<!-- TMS 车辆 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number v-model="formData.vehicleId" class="!w-1/1" :controls="false" placeholder="请输入车辆编号" />
      </el-form-item>
      <el-form-item label="运营日期" prop="operationDate">
        <el-date-picker v-model="formData.operationDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择运营日期" />
      </el-form-item>
      <el-form-item label="行驶里程（公里）" prop="mileage">
        <el-input-number v-model="formData.mileage" class="!w-1/1" :controls="false" placeholder="请输入行驶里程（公里）" />
      </el-form-item>
      <el-form-item label="油耗（升）" prop="fuelConsumption">
        <el-input-number v-model="formData.fuelConsumption" class="!w-1/1" :controls="false" placeholder="请输入油耗（升）" />
      </el-form-item>
      <el-form-item label="油费（元）" prop="fuelCost">
        <el-input-number v-model="formData.fuelCost" class="!w-1/1" :controls="false" placeholder="请输入油费（元）" />
      </el-form-item>
      <el-form-item label="维修保养费（元）" prop="maintenanceCost">
        <el-input-number v-model="formData.maintenanceCost" class="!w-1/1" :controls="false" placeholder="请输入维修保养费（元）" />
      </el-form-item>
      <el-form-item label="保险费（元）" prop="insuranceCost">
        <el-input-number v-model="formData.insuranceCost" class="!w-1/1" :controls="false" placeholder="请输入保险费（元）" />
      </el-form-item>
      <el-form-item label="年检费（元）" prop="inspectionCost">
        <el-input-number v-model="formData.inspectionCost" class="!w-1/1" :controls="false" placeholder="请输入年检费（元）" />
      </el-form-item>
      <el-form-item label="运营收入（元）" prop="revenue">
        <el-input-number v-model="formData.revenue" class="!w-1/1" :controls="false" placeholder="请输入运营收入（元）" />
      </el-form-item>
      <el-form-item label="总运营成本（元）" prop="totalCost">
        <el-input-number v-model="formData.totalCost" class="!w-1/1" :controls="false" placeholder="请输入总运营成本（元）" />
      </el-form-item>
      <el-form-item label="运营利润（元）" prop="profit">
        <el-input-number v-model="formData.profit" class="!w-1/1" :controls="false" placeholder="请输入运营利润（元）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { TmsVehicleApi } from '@/api/tms/vehicle'

defineOptions({ name: 'TmsVehicleForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  remark: [required],
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TmsVehicleApi.getTmsVehicle(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await TmsVehicleApi.createTmsVehicle(data)
      message.success(t('common.createSuccess'))
    } else {
      await TmsVehicleApi.updateTmsVehicle(data)
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
