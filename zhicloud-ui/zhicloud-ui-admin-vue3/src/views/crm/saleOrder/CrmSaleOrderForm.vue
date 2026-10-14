<!-- CRM 销售订单表单（含产品明细子表；金额/税额由服务端计算，前端只做展示预览） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="900">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      :disabled="dialogType === 'detail'"
      label-width="110px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="客户" prop="customerId">
            <el-input-number
              v-model="formData.customerId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入客户编号"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合同" prop="contractId">
            <el-input-number
              v-model="formData.contractId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入合同编号（可空）"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商机" prop="businessId">
            <el-input-number
              v-model="formData.businessId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入商机编号（可空）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactId">
            <el-input-number
              v-model="formData.contactId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入联系人编号（可空）"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="负责人" prop="ownerUserId">
            <el-input-number
              v-model="formData.ownerUserId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入负责人编号"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="订单日期" prop="orderDate">
            <el-date-picker
              v-model="formData.orderDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择订单日期"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="交货日期" prop="deliveryDate">
        <el-date-picker
          v-model="formData.deliveryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择交货日期（可空）"
          class="!w-400px"
        />
      </el-form-item>
      <el-form-item label="整单折扣" prop="discountAmount">
        <el-input-number
          v-model="formData.discountAmount"
          :controls="false"
          :precision="2"
          class="!w-400px"
          placeholder="请输入整单折扣金额"
        />
      </el-form-item>
    </el-form>

    <!-- 产品明细子表 -->
    <div class="mb-8px font-bold">产品明细</div>
    <el-table :data="items" size="small" border stripe>
      <el-table-column align="center" label="#" type="index" width="50" />
      <el-table-column align="left" label="产品" min-width="180">
        <template #default="{ row }">
          <el-input-number
            v-model="row.productId"
            :controls="false"
            size="small"
            placeholder="产品编号"
          />
        </template>
      </el-table-column>
      <el-table-column align="left" label="产品名称" min-width="140">
        <template #default="{ row }">
          <el-input v-model="row.productName" size="small" placeholder="可空" />
        </template>
      </el-table-column>
      <el-table-column align="right" label="数量" min-width="110">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :controls="false" :min="0" size="small" placeholder="数量" />
        </template>
      </el-table-column>
      <el-table-column align="right" label="单价" min-width="110">
        <template #default="{ row }">
          <el-input-number v-model="row.unitPrice" :controls="false" :precision="2" :min="0" size="small" placeholder="单价" />
        </template>
      </el-table-column>
      <el-table-column align="right" label="折扣" min-width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.discount" :controls="false" :precision="2" :min="0" size="small" placeholder="折扣" />
        </template>
      </el-table-column>
      <el-table-column align="right" label="税率(%)" min-width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.taxRate" :controls="false" :precision="2" :min="0" size="small" placeholder="税率" />
        </template>
      </el-table-column>
      <el-table-column align="left" label="备注" min-width="120">
        <template #default="{ row }">
          <el-input v-model="row.remark" size="small" placeholder="可空" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="操作" width="70" fixed="right">
        <template #default="{ $index }">
          <el-button
            v-if="dialogType !== 'detail'"
            link
            type="danger"
            @click="items.splice($index, 1)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button
      v-if="dialogType !== 'detail'"
      class="mt-8px"
      plain
      type="primary"
      @click="items.push({})"
    >
      <Icon icon="ep:plus" class="mr-5px" /> 添加明细
    </el-button>
    <div class="mt-4px text-12px text-gray-400">
      总金额、各明细金额与税额由服务端计算（先删后插明细），此处仅提交原始录入值
    </div>

    <template #footer>
      <el-button v-if="dialogType !== 'detail'" :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { CrmSaleOrderApi } from '@/api/crm/saleOrder'
import type { CrmSaleOrderItemVO, CrmSaleOrderVO } from '@/api/crm/saleOrder'

defineOptions({ name: 'CrmSaleOrderForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('')
const formLoading = ref(false)
const formData = ref<CrmSaleOrderVO>({})
/** 明细行（提交时挂到 formData.items） */
const items = ref<CrmSaleOrderItemVO[]>([])
const formRules = reactive<FormRules>({
  customerId: [{ required: true, message: '客户不能为空', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '负责人不能为空', trigger: 'blur' }],
  orderDate: [{ required: true, message: '订单日期不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = type === 'detail' ? '销售订单详情' : t('action.' + type)
  dialogType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      const detail = await CrmSaleOrderApi.getCrmSaleOrder(id)
      formData.value = detail
      items.value = (detail.items || []).map((it) => ({ ...it }))
      if (items.value.length === 0) {
        items.value = [{}]
      }
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const resetForm = () => {
  formData.value = { discountAmount: undefined }
  items.value = [{}]
  formRef.value?.resetFields()
}

const emit = defineEmits<{ success: [] }>()

/** 提交表单（明细行 productId/quantity/unitPrice 必填由后端校验，这里只做基础过滤） */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  const validItems = items.value.filter(
    (it) => it.productId != null && it.quantity != null && it.unitPrice != null
  )
  if (validItems.length === 0) {
    message.warning('请至少填写一条完整的产品明细（产品、数量、单价）')
    return
  }
  formLoading.value = true
  try {
    const data: CrmSaleOrderVO = {
      id: formData.value.id,
      customerId: formData.value.customerId,
      contractId: formData.value.contractId,
      businessId: formData.value.businessId,
      contactId: formData.value.contactId,
      ownerUserId: formData.value.ownerUserId,
      orderDate: formData.value.orderDate,
      deliveryDate: formData.value.deliveryDate,
      discountAmount: formData.value.discountAmount,
      remark: formData.value.remark,
      items: validItems
    }
    if (formData.value.id) {
      await CrmSaleOrderApi.updateCrmSaleOrder(data)
      message.success(t('common.updateSuccess'))
    } else {
      await CrmSaleOrderApi.createCrmSaleOrder(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
