<!-- 系统 - 主数据跨域对照 表单（登记/覆盖一条映射） -->
<template>
  <Dialog v-model="dialogVisible" title="登记主数据对照">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="源系统" prop="sourceSystem">
        <el-input v-model="formData.sourceSystem" class="!w-1/1" placeholder="如 ERP" />
      </el-form-item>
      <el-form-item label="源实体" prop="sourceEntity">
        <el-input v-model="formData.sourceEntity" class="!w-1/1" placeholder="如 PRODUCT" />
      </el-form-item>
      <el-form-item label="源编号" prop="sourceId">
        <el-input v-model="formData.sourceId" class="!w-1/1" placeholder="如 100" />
      </el-form-item>
      <el-form-item label="目标系统" prop="targetSystem">
        <el-input v-model="formData.targetSystem" class="!w-1/1" placeholder="如 WMS" />
      </el-form-item>
      <el-form-item label="目标实体" prop="targetEntity">
        <el-input v-model="formData.targetEntity" class="!w-1/1" placeholder="如 SKU" />
      </el-form-item>
      <el-form-item label="目标编号" prop="targetId">
        <el-input v-model="formData.targetId" class="!w-1/1" placeholder="如 1" />
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
import { MdmCrossRefApi } from '@/api/system/mdm-cross-ref'

defineOptions({ name: 'MdmCrossRefForm' })

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = ref<any>({})
const formRules = reactive({
  sourceSystem: [required],
  sourceEntity: [required],
  sourceId: [required],
  targetSystem: [required],
  targetEntity: [required],
  targetId: [required]
})

const emit = defineEmits(['success'])

const open = () => {
  dialogVisible.value = true
  formData.value = {}
}

const submitForm = async () => {
  if (!formRef) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    await MdmCrossRefApi.saveCrossRef(formData.value)
    message.success('登记成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
