<!-- CRM 售后工单表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" maxlength="256" placeholder="请输入标题" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-input-number
          v-model="formData.customerId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入客户编号"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactId">
        <el-input-number
          v-model="formData.contactId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入联系人编号（可空）"
        />
      </el-form-item>
      <el-form-item label="产品" prop="productId">
        <el-input-number
          v-model="formData.productId"
          :controls="false"
          class="!w-1/1"
          placeholder="请输入产品编号（可空）"
        />
      </el-form-item>
      <el-form-item label="工单类型" prop="workOrderType">
        <el-select v-model="formData.workOrderType" class="!w-1/1" placeholder="请选择工单类型">
          <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-select v-model="formData.priority" class="!w-1/1" placeholder="请选择优先级">
          <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          maxlength="2000"
          placeholder="请输入问题描述"
        />
      </el-form-item>
      <el-form-item label="SLA 截止" prop="slaDeadline">
        <el-date-picker
          v-model="formData.slaDeadline"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择 SLA 截止时间（可空）"
          class="!w-1/1"
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
import { CrmWorkOrderApi } from '@/api/crm/workOrder'
import type { CrmWorkOrderVO } from '@/api/crm/workOrder'

defineOptions({ name: 'CrmWorkOrderForm' })

const { t } = useI18n()
const message = useMessage()

const TYPE_OPTIONS = [
  { value: 10, label: '安装' },
  { value: 20, label: '维修' },
  { value: 30, label: '投诉' },
  { value: 40, label: '咨询' },
  { value: 50, label: '退换货' }
]

const PRIORITY_OPTIONS = [
  { value: 10, label: '低' },
  { value: 20, label: '中' },
  { value: 30, label: '高' },
  { value: 40, label: '紧急' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<CrmWorkOrderVO>({})
const formRules = reactive<FormRules>({
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  customerId: [{ required: true, message: '客户不能为空', trigger: 'blur' }],
  workOrderType: [{ required: true, message: '工单类型不能为空', trigger: 'change' }],
  priority: [{ required: true, message: '优先级不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = {}
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CrmWorkOrderApi.getCrmWorkOrder(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits<{ success: [] }>()

/** 提交表单 */
async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data: CrmWorkOrderVO = {
      id: formData.value.id,
      title: formData.value.title,
      customerId: formData.value.customerId,
      contactId: formData.value.contactId,
      productId: formData.value.productId,
      workOrderType: formData.value.workOrderType,
      priority: formData.value.priority,
      description: formData.value.description,
      slaDeadline: formData.value.slaDeadline,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await CrmWorkOrderApi.updateCrmWorkOrder(data)
      message.success(t('common.updateSuccess'))
    } else {
      await CrmWorkOrderApi.createCrmWorkOrder(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
