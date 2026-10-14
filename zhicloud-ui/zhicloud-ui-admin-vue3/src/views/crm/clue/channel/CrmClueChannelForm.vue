<!-- CRM 线索渠道表单（channelCode 唯一） -->
<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="渠道编码" prop="channelCode">
        <el-input v-model="formData.channelCode" maxlength="64" placeholder="请输入渠道编码（唯一，外部线索按此编码对接）" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="渠道名称" prop="channelName">
        <el-input v-model="formData.channelName" maxlength="128" placeholder="请输入渠道名称" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="渠道类型" prop="channelType">
        <el-select v-model="formData.channelType" class="!w-1/1" placeholder="请选择渠道类型">
          <el-option v-for="item in TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="API 地址" prop="apiUrl">
        <el-input v-model="formData.apiUrl" maxlength="512" placeholder="请输入 API 地址（可空）" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="API 密钥" prop="apiKey">
        <el-input v-model="formData.apiKey" maxlength="255" placeholder="请输入 API 密钥（可空）" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="自动分配" prop="autoAssignUserId">
        <el-input-number
          v-model="formData.autoAssignUserId"
          :controls="false"
          class="!w-1/1"
          placeholder="该渠道新线索自动分配给此负责人（可空）"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="10">启用</el-radio>
          <el-radio :value="20">停用</el-radio>
        </el-radio-group>
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
import { CrmClueChannelApi } from '@/api/crm/clue/channel'
import type { CrmClueChannelVO } from '@/api/crm/clue/channel'

defineOptions({ name: 'CrmClueChannelForm' })

const { t } = useI18n()
const message = useMessage()

const TYPE_OPTIONS = [
  { value: 10, label: '官网' },
  { value: 20, label: '广告' },
  { value: 30, label: '企微' },
  { value: 40, label: '小程序' },
  { value: 50, label: 'API' },
  { value: 60, label: '线下' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<CrmClueChannelVO>({})
const formRules = reactive<FormRules>({
  channelCode: [{ required: true, message: '渠道编码不能为空', trigger: 'blur' }],
  channelName: [{ required: true, message: '渠道名称不能为空', trigger: 'blur' }],
  channelType: [{ required: true, message: '渠道类型不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref<FormInstance>()

/** 打开弹窗 */
async function open(type: string, id?: number) {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formData.value = { status: 10 }
  formRef.value?.resetFields()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await CrmClueChannelApi.getCrmClueChannel(id)
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
    const data: CrmClueChannelVO = {
      id: formData.value.id,
      channelName: formData.value.channelName,
      channelType: formData.value.channelType,
      channelCode: formData.value.channelCode,
      apiUrl: formData.value.apiUrl,
      apiKey: formData.value.apiKey,
      autoAssignUserId: formData.value.autoAssignUserId,
      status: formData.value.status,
      remark: formData.value.remark
    }
    if (formData.value.id) {
      await CrmClueChannelApi.updateCrmClueChannel(data)
      message.success(t('common.updateSuccess'))
    } else {
      await CrmClueChannelApi.createCrmClueChannel(data)
      message.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
