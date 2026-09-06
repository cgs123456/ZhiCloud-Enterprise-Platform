<!-- 8D报告 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="8D 报告编号" prop="reportNo">
        <el-input v-model="formData.reportNo" class="!w-1/1" placeholder="请输入8D 报告编号" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="关联 NCR 编号 ID" prop="ncrId">
        <el-input-number v-model="formData.ncrId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入关联 NCR 编号 ID" />
      </el-form-item>
      <el-form-item label="关联 CAPA 编号 ID" prop="capaId">
        <el-input-number v-model="formData.capaId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入关联 CAPA 编号 ID" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :min="0" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="D1 团队成员" prop="d1TeamMembers">
        <el-input v-model="formData.d1TeamMembers" class="!w-1/1" placeholder="请输入D1 团队成员" />
      </el-form-item>
      <el-form-item label="D2 问题描述" prop="d2ProblemDescription">
        <el-input v-model="formData.d2ProblemDescription" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入D2 问题描述" />
      </el-form-item>
      <el-form-item label="D3 临时遏制措施" prop="d3InterimAction">
        <el-input v-model="formData.d3InterimAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入D3 临时遏制措施" />
      </el-form-item>
      <el-form-item label="D4 根本原因分析" prop="d4RootCause">
        <el-input v-model="formData.d4RootCause" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入D4 根本原因分析" />
      </el-form-item>
      <el-form-item label="D5 永久纠正措施" prop="d5PermanentAction">
        <el-input v-model="formData.d5PermanentAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入D5 永久纠正措施" />
      </el-form-item>
      <el-form-item label="D6 实施并验证结果" prop="d6ImplementationResult">
        <el-input v-model="formData.d6ImplementationResult" class="!w-1/1" placeholder="请输入D6 实施并验证结果" />
      </el-form-item>
      <el-form-item label="D7 预防再发生措施" prop="d7PreventionAction">
        <el-input v-model="formData.d7PreventionAction" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入D7 预防再发生措施" />
      </el-form-item>
      <el-form-item label="D8 团队表彰" prop="d8TeamRecognition">
        <el-input v-model="formData.d8TeamRecognition" class="!w-1/1" placeholder="请输入D8 团队表彰" />
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
import { EightDReportApi } from '@/api/qms/eightd/report'

defineOptions({ name: 'EightDReportForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  title: [required],

  ncrId: [required],

  capaId: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await EightDReportApi.getEightDReport(id)
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
      await EightDReportApi.createEightDReport(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await EightDReportApi.updateEightDReport(formData.value)
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
