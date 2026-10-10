<!-- HR 部门 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="上级部门" prop="parentId">
        <el-input-number v-model="formData.parentId" class="!w-1/1" :controls="false" placeholder="0 表示顶级" />
      </el-form-item>
      <el-form-item label="部门名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入部门名称" />
      </el-form-item>
      <el-form-item label="部门编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入部门编码" />
      </el-form-item>
      <el-form-item label="负责人 ID" prop="leaderId">
        <el-input-number v-model="formData.leaderId" class="!w-1/1" :controls="false" placeholder="请输入负责人 ID" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="formData.status" class="!w-1/1" :controls="false" placeholder="0 开启 1 关闭" />
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
import { HrDepartmentApi } from '@/api/hr/department'

defineOptions({ name: 'HrDepartmentForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<any>({})
const formRules = reactive({
  name: [required]
})

const emit = defineEmits(['success'])

const open = async (type: string, id?: number, parentId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (parentId !== undefined) {
    formData.value.parentId = parentId
  }
  if (id) {
    formLoading.value = true
    try {
      formData.value = await HrDepartmentApi.getDepartment(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await HrDepartmentApi.createDepartment(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await HrDepartmentApi.updateDepartment(formData.value)
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
