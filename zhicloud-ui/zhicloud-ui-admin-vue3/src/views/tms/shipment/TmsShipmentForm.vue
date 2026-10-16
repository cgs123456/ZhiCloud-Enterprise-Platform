<!-- TMS 运单 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="运单号" prop="no">
        <el-input v-model="formData.no" class="!w-1/1" placeholder="请输入运单号" />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number v-model="formData.carrierId" class="!w-1/1" :controls="false" placeholder="请输入承运商编号" />
      </el-form-item>
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number v-model="formData.vehicleId" class="!w-1/1" :controls="false" placeholder="请输入车辆编号" />
      </el-form-item>
      <el-form-item label="司机编号" prop="driverId">
        <el-input-number v-model="formData.driverId" class="!w-1/1" :controls="false" placeholder="请输入司机编号" />
      </el-form-item>
      <el-form-item label="起点地址" prop="originAddress">
        <el-input v-model="formData.originAddress" class="!w-1/1" placeholder="请输入起点地址" />
      </el-form-item>
      <el-form-item label="终点地址" prop="destinationAddress">
        <el-input v-model="formData.destinationAddress" class="!w-1/1" placeholder="请输入终点地址" />
      </el-form-item>
      <el-form-item label="运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）" prop="shipmentType">
        <el-input-number v-model="formData.shipmentType" class="!w-1/1" :controls="false" placeholder="请输入运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）" />
      </el-form-item>
      <el-form-item label="来源单据号" prop="sourceOrderNo">
        <el-input v-model="formData.sourceOrderNo" class="!w-1/1" placeholder="请输入来源单据号" />
      </el-form-item>
      <el-form-item label="合计数量" prop="totalQuantity">
        <el-input-number v-model="formData.totalQuantity" class="!w-1/1" :controls="false" placeholder="请输入合计数量" />
      </el-form-item>
      <el-form-item label="合计重量" prop="totalWeight">
        <el-input-number v-model="formData.totalWeight" class="!w-1/1" :controls="false" placeholder="请输入合计重量" />
      </el-form-item>
      <el-form-item label="合计体积" prop="totalVolume">
        <el-input-number v-model="formData.totalVolume" class="!w-1/1" :controls="false" placeholder="请输入合计体积" />
      </el-form-item>
      <el-form-item label="运费金额" prop="freightAmount">
        <el-input-number v-model="formData.freightAmount" class="!w-1/1" :controls="false" placeholder="请输入运费金额" />
      </el-form-item>
      <el-form-item label="发车时间" prop="departureTime">
        <el-date-picker v-model="formData.departureTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择发车时间" />
      </el-form-item>
      <el-form-item label="预计到达时间" prop="estimatedArrivalTime">
        <el-date-picker v-model="formData.estimatedArrivalTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择预计到达时间" />
      </el-form-item>
      <el-form-item label="实际到达时间" prop="actualArrivalTime">
        <el-date-picker v-model="formData.actualArrivalTime" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择实际到达时间" />
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
import { TmsShipmentApi, type TmsShipmentSaveVO } from '@/api/tms/shipment'

defineOptions({ name: 'TmsShipmentForm' })

// 运单状态不在表单里：它由「调度 / 确认到达 / 签收」推进，表单编辑状态会把运单
// 改回「待发车」从而绕过并派车互斥（后端 §12.231 的 CAS 前提是状态单调不回退）。
// 提交体因此刻意不含 status，详情回填到的多余字段也在这里剥掉。
const toSaveVO = (raw: any): TmsShipmentSaveVO => {
  const { status, createTime, ...rest } = raw
  return rest as TmsShipmentSaveVO
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  no: [required],
  originAddress: [required],
  destinationAddress: [required],
  sourceOrderNo: [required],
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
      formData.value = await TmsShipmentApi.getTmsShipment(id)
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
    const data = toSaveVO(formData.value)
    if (formType.value === 'create') {
      await TmsShipmentApi.createTmsShipment(data)
      message.success(t('common.createSuccess'))
    } else {
      await TmsShipmentApi.updateTmsShipment(data)
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
