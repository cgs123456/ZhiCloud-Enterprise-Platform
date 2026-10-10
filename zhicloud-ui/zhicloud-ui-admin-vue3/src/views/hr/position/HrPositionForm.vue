<!-- HR 岗位 表单 -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="职位编码" prop="code">
        <el-input v-model="formData.code" class="!w-1/1" placeholder="请输入职位编码" />
      </el-form-item>
      <el-form-item label="职位名称" prop="name">
        <el-input v-model="formData.name" class="!w-1/1" placeholder="请输入职位名称" />
      </el-form-item>
      <el-form-item label="所属部门 ID" prop="deptId">
        <el-input-number v-model="formData.deptId" class="!w-1/1" :controls="false" placeholder="请输入所属部门 ID" />
      </el-form-item>
      <el-form-item label="职级" prop="level">
        <el-input-number v-model="formData.level" class="!w-1/1" :controls="false" placeholder="请输入职级" />
      </el-form-item>
      <el-form-item label="基本工资" prop="baseSalary">
        <el-input-number v-model="formData.baseSalary" class="!w-1/1" :controls="false" placeholder="请输入基本工资" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" class="!w-1/1" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" class="!w-1/1" :controls="false" placeholder="请输入排序" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { HrPositionApi } from '@/api/hr/position'

defineOptions({ name: 'HrPositionForm' })

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
      formData.value = await HrPositionApi.getHrPosition(id)
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
      await HrPositionApi.createHrPosition(data)
      message.success(t('common.createSuccess'))
    } else {
      await HrPositionApi.updateHrPosition(data)
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
