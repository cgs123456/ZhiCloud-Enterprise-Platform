<!-- TMS 运费结算表单（新增/修改/详情；含运费试算回填） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      :disabled="dialogType === 'detail'"
      label-width="110px"
    >
      <el-form-item label="结算单号" prop="no">
        <el-input v-model="formData.no" maxlength="64" placeholder="请输入结算单号" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="运单编号" prop="shipmentId">
        <el-input-number
          v-model="formData.shipmentId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入运单编号"
        />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number
          v-model="formData.carrierId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入承运商编号（可空）"
        />
      </el-form-item>
      <el-form-item label="计费方式" prop="billingMethod">
        <el-select v-model="formData.billingMethod" class="!w-1/1" placeholder="请选择计费方式">
          <el-option v-for="item in BILLING_METHODS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="运单试算">
        <div class="w-1/1">
          <div class="flex items-center gap-8px">
            <el-button type="primary" plain :loading="calcLoading" @click="handleCalculate">
              <Icon icon="ep:calculator" class="mr-5px" /> 按运单试算运费
            </el-button>
            <span v-if="calcResult" class="text-13px text-gray-500">
              计费数量 {{ calcResult.billingQuantity }} × 单价 {{ calcResult.unitPrice ?? 0 }}
              + 附加 {{ calcResult.surcharge ?? 0 }} - 折扣 {{ calcResult.discountAmount ?? 0 }}
              = {{ calcResult.totalAmount }}
            </span>
          </div>
          <div class="text-12px text-gray-400 mt-4px">
            试算只读，不落库：按运单重量/体积/件数/里程自动取计费数量（整车一口价数量固定为 1）
          </div>
        </div>
      </el-form-item>
      <el-form-item label="单价" prop="unitPrice">
        <el-input-number
          v-model="formData.unitPrice"
          :controls="false"
          :precision="2"
          class="!w-1/1"
          placeholder="请输入单价（按重量/体积/件数时填写）"
        />
      </el-form-item>
      <el-form-item label="附加费" prop="surcharge">
        <el-input-number v-model="formData.surcharge" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入附加费" />
      </el-form-item>
      <el-form-item label="折扣金额" prop="discountAmount">
        <el-input-number v-model="formData.discountAmount" :controls="false" :precision="2" class="!w-1/1" placeholder="请输入折扣金额" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="512" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button v-if="dialogType !== 'detail'" :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { TmsFreightApi } from '@/api/tms/freight'
import type { TmsFreightVO, TmsFreightCalculateVO } from '@/api/tms/freight'

defineOptions({ name: 'TmsFreightForm' })

const { t } = useI18n()
const message = useMessage()

const BILLING_METHODS = [
  { value: 10, label: '按重量' },
  { value: 20, label: '按体积' },
  { value: 30, label: '按件数' },
  { value: 40, label: '整车一口价' },
  { value: 50, label: '里程计费' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('')
const formLoading = ref(false)
const formData = ref<TmsFreightVO>({})
const formRules = reactive<FormRules>({
  no: [{ required: true, message: '结算单号不能为空', trigger: 'blur' }],
  shipmentId: [{ required: true, message: '运单编号不能为空', trigger: 'blur' }],
  billingMethod: [{ required: true, message: '计费方式不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 运费试算结果（只读展示） */
const calcLoading = ref(false)
const calcResult = ref<TmsFreightCalculateVO | null>(null)

const resetForm = () => {
  formData.value = {
    no: undefined,
    shipmentId: undefined,
    carrierId: undefined,
    billingMethod: undefined,
    unitPrice: undefined,
    surcharge: undefined,
    discountAmount: undefined,
    remark: undefined
  }
  calcResult.value = null
  formRef.value?.resetFields()
}

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = type === 'detail' ? '运费结算详情' : t('action.' + type)
  dialogType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TmsFreightApi.getTmsFreight(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 运费试算（只读，回填数量/单价/金额） */
const handleCalculate = async () => {
  if (!formData.value.shipmentId || !formData.value.billingMethod) {
    message.warning('请先填写运单编号与计费方式')
    return
  }
  calcLoading.value = true
  try {
    const res = await TmsFreightApi.calculateTmsFreight({
      shipmentId: formData.value.shipmentId,
      billingMethod: formData.value.billingMethod,
      unitPrice: formData.value.unitPrice,
      surcharge: formData.value.surcharge,
      discountAmount: formData.value.discountAmount
    })
    calcResult.value = res
    if (res) {
      formData.value.unitPrice = res.unitPrice
      formData.value.surcharge = res.surcharge
      formData.value.discountAmount = res.discountAmount
      if ((res.totalAmount ?? 0) === 0) {
        message.warning('折扣金额过大，试算总额已被夹到 0，请调整后重试')
      }
    }
  } finally {
    calcLoading.value = false
  }
}

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as TmsFreightVO
    if (formData.value.id) {
      await TmsFreightApi.updateTmsFreight(data)
      message.success(t('common.updateSuccess'))
    } else {
      await TmsFreightApi.createTmsFreight(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
