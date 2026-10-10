<!-- ERP 会计科目 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="父级编号" prop="parentId">
        <el-input-number v-model="formData.parentId" class="!w-1/1" :controls="false" placeholder="请输入父级编号" />
      </el-form-item>
      <el-form-item label="科目编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入科目编码" />
      </el-form-item>
      <el-form-item label="科目名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入科目名称" />
      </el-form-item>
      <el-form-item label="科目类型" prop="type">
        <el-input-number v-model="formData.type" class="!w-1/1" :controls="false" placeholder="请输入科目类型" />
      </el-form-item>
      <el-form-item label="科目类型名称" prop="typeName">
        <el-input v-model="formData.typeName" class="!w-1/1" placeholder="请输入科目类型名称" />
      </el-form-item>
      <el-form-item label="余额方向" prop="balanceDirection">
        <el-input-number v-model="formData.balanceDirection" class="!w-1/1" :controls="false" placeholder="请输入余额方向" />
      </el-form-item>
      <el-form-item label="余额方向名称" prop="balanceDirectionName">
        <el-input v-model="formData.balanceDirectionName" class="!w-1/1" placeholder="请输入余额方向名称" />
      </el-form-item>
      <el-form-item label="层级" prop="level">
        <el-input-number v-model="formData.level" class="!w-1/1" :controls="false" placeholder="请输入层级" />
      </el-form-item>
      <el-form-item label="是否末级科目" prop="isLeaf">
        <el-switch v-model="formData.isLeaf" />
      </el-form-item>
      <el-form-item label="期初借方余额" prop="openingDebit">
        <el-input-number v-model="formData.openingDebit" class="!w-1/1" :controls="false" placeholder="请输入期初借方余额" />
      </el-form-item>
      <el-form-item label="期初贷方余额" prop="openingCredit">
        <el-input-number v-model="formData.openingCredit" class="!w-1/1" :controls="false" placeholder="请输入期初贷方余额" />
      </el-form-item>
      <el-form-item label="当前借方累计发生额" prop="currentDebit">
        <el-input-number v-model="formData.currentDebit" class="!w-1/1" :controls="false" placeholder="请输入当前借方累计发生额" />
      </el-form-item>
      <el-form-item label="当前贷方累计发生额" prop="currentCredit">
        <el-input-number v-model="formData.currentCredit" class="!w-1/1" :controls="false" placeholder="请输入当前贷方累计发生额" />
      </el-form-item>
      <el-form-item label="期末借方余额" prop="closingDebit">
        <el-input-number v-model="formData.closingDebit" class="!w-1/1" :controls="false" placeholder="请输入期末借方余额" />
      </el-form-item>
      <el-form-item label="期末贷方余额" prop="closingCredit">
        <el-input-number v-model="formData.closingCredit" class="!w-1/1" :controls="false" placeholder="请输入期末贷方余额" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
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
import { ErpGlAccountApi } from '@/api/erp/gl-account'

defineOptions({ name: 'ErpGlAccountForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<any>({})
const formRules = reactive({
  code: [required],
  name: [required],
  typeName: [required],
  balanceDirectionName: [required],
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
      formData.value = await ErpGlAccountApi.getErpGlAccount(id)
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
      await ErpGlAccountApi.createErpGlAccount(data)
      message.success(t('common.createSuccess'))
    } else {
      await ErpGlAccountApi.updateErpGlAccount(data)
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
