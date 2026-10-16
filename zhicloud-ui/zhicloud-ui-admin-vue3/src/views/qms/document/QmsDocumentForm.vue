<!-- 受控文档 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="文件编号" prop="docNo">
        <el-input v-model="formData.docNo" class="!w-1/1" placeholder="请输入文件编号" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" class="!w-1/1" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="文件类型" prop="docType">
        <el-input-number v-model="formData.docType" class="!w-1/1" :min="0" :controls="false" placeholder="请输入文件类型" />
      </el-form-item>
      <el-form-item label="版本号" prop="version">
        <el-input v-model="formData.version" class="!w-1/1" placeholder="请输入版本号" />
      </el-form-item>
      <!-- 状态只读：新增由服务端强制 DRAFT，修改时状态变更必须走 submit/approve/reject 流转方法（§12.236）。
           这里改为只读回显，避免"填了不生效"的静默欺骗；编辑态展示当前状态，新建态提示将置为草稿。 -->
      <el-form-item label="状态" prop="status">
        <dict-tag v-if="formType === 'update'" :type="DICT_TYPE.QMS_DOC_STATUS" :value="formData.status" />
        <span v-else class="text-gray-400">保存后自动置为「草稿」</span>
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-date-picker v-model="formData.effectiveDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择生效日期" />
      </el-form-item>
      <el-form-item label="失效日期" prop="expiryDate">
        <el-date-picker v-model="formData.expiryDate" class="!w-1/1" value-format="YYYY-MM-DD" type="date" placeholder="请选择失效日期" />
      </el-form-item>
      <!-- 审批留痕只读：approveDate/approverId 由 approveDocument 产生（该端点挂 @ElectronicSignature），
           通用表单不接受外部写入（§12.236.1 真机实证：建单直传 approverId 会被强制置空）。 -->
      <el-form-item label="审批人" prop="approverId">
        <span v-if="formData.approverId" class="text-gray-700">{{ formData.approverId }}</span>
        <span v-else class="text-gray-400">待审批</span>
      </el-form-item>
      <el-form-item label="归属部门 ID" prop="ownerDeptId">
        <el-input-number v-model="formData.ownerDeptId" class="!w-1/1" :min="0" :controls="false" placeholder="请输入归属部门 ID" />
      </el-form-item>
      <el-form-item label="文件 URL" prop="fileUrl">
        <el-input v-model="formData.fileUrl" class="!w-1/1" placeholder="请输入文件 URL" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :min="0" :controls="false" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { QmsDocumentApi } from '@/api/qms/document'
import { DICT_TYPE } from '@/utils/dict'

defineOptions({ name: 'QmsDocumentForm' })

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

  docType: [required],

  version: [required]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QmsDocumentApi.getQmsDocument(id)
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
      await QmsDocumentApi.createQmsDocument(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QmsDocumentApi.updateQmsDocument(formData.value)
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
