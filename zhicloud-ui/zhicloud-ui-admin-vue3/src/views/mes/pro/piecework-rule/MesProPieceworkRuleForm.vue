<!-- MES 计件规则 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="formData.ruleName" class="!w-1/1" placeholder="请输入规则名称" />
      </el-form-item>
      <el-form-item label="工序编号" prop="processId">
        <el-input-number v-model="formData.processId" class="!w-1/1" :controls="false" placeholder="请输入工序编号" />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="formData.processName" class="!w-1/1" placeholder="请输入工序名称" />
      </el-form-item>
      <el-form-item label="工艺路线编号" prop="routeId">
        <el-input-number v-model="formData.routeId" class="!w-1/1" :controls="false" placeholder="请输入工艺路线编号" />
      </el-form-item>
      <el-form-item label="产品物料编号" prop="itemId">
        <el-input-number v-model="formData.itemId" class="!w-1/1" :controls="false" placeholder="请输入产品物料编号" />
      </el-form-item>
      <el-form-item label="产品物料名称" prop="itemName">
        <el-input v-model="formData.itemName" class="!w-1/1" placeholder="请输入产品物料名称" />
      </el-form-item>
      <el-form-item label="工作站编号" prop="workstationId">
        <el-input-number v-model="formData.workstationId" class="!w-1/1" :controls="false" placeholder="请输入工作站编号" />
      </el-form-item>
      <el-form-item label="工作站名称" prop="workstationName">
        <el-input v-model="formData.workstationName" class="!w-1/1" placeholder="请输入工作站名称" />
      </el-form-item>
      <el-form-item label="合格品单价（元/件）" prop="qualifiedUnitPrice">
        <el-input-number v-model="formData.qualifiedUnitPrice" class="!w-1/1" :controls="false" placeholder="请输入合格品单价（元/件）" />
      </el-form-item>
      <el-form-item label="废品单价（元/件）" prop="scrapUnitPrice">
        <el-input-number v-model="formData.scrapUnitPrice" class="!w-1/1" :controls="false" placeholder="请输入废品单价（元/件）" />
      </el-form-item>
      <el-form-item label="阶梯单价配置（JSON）" prop="stepConfig">
        <el-input v-model="formData.stepConfig" class="!w-1/1" placeholder="请输入阶梯单价配置（JSON）" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-date-picker v-model="formData.effectiveDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择生效日期" />
      </el-form-item>
      <el-form-item label="失效日期" prop="expireDate">
        <el-date-picker v-model="formData.expireDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" placeholder="请选择失效日期" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input-number v-model="formData.enabled" class="!w-1/1" :controls="false" placeholder="请输入是否启用" />
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
import { MesProPieceworkRuleApi } from '@/api/mes/pro-piecework-rule'

defineOptions({ name: 'MesProPieceworkRuleForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  ruleName: [required],
  processName: [required],
  itemName: [required],
  workstationName: [required],
  stepConfig: [required],
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
      formData.value = await MesProPieceworkRuleApi.getMesProPieceworkRule(id)
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
      await MesProPieceworkRuleApi.createMesProPieceworkRule(data)
      message.success(t('common.createSuccess'))
    } else {
      await MesProPieceworkRuleApi.updateMesProPieceworkRule(data)
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
