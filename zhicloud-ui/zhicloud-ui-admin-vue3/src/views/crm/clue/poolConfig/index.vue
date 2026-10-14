<!-- CRM 线索公海配置（单例：GET + PUT） -->
<template>
  <ContentWrap>
    <doc-alert title="【线索】公海配置" url="https://doc.zhicloud.cn/crm/clue-pool-config/" />

    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="160px"
      class="max-w-640px"
    >
      <el-form-item label="开启线索公海" prop="enabled">
        <el-switch v-model="formData.enabled" />
      </el-form-item>
      <el-form-item label="未联系回收天数" prop="contactExpireDays">
        <el-input-number
          v-model="formData.contactExpireDays"
          :controls="false"
          :min="1"
          class="!w-240px"
          placeholder="开启公海后必填"
        />
        <div class="text-12px text-gray-400 mt-4px">线索超过该天数未联系将自动回收到公海</div>
      </el-form-item>
      <el-form-item label="开启回收通知" prop="notifyEnabled">
        <el-switch v-model="formData.notifyEnabled" />
      </el-form-item>
      <el-form-item label="提前通知天数" prop="notifyDays">
        <el-input-number
          v-model="formData.notifyDays"
          :controls="false"
          :min="1"
          class="!w-240px"
          placeholder="开启通知后必填"
        />
        <div class="text-12px text-gray-400 mt-4px">回收前提前多少天通知原负责人</div>
      </el-form-item>
      <el-form-item>
        <el-button :disabled="formLoading" type="primary" @click="submitForm">
          <Icon icon="ep:check" class="mr-5px" /> 保 存
        </el-button>
        <el-button :loading="formLoading" @click="loadConfig">重 置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { CrmCluePoolConfigApi } from '@/api/crm/clue/poolConfig'
import type { CrmCluePoolConfigVO } from '@/api/crm/clue/poolConfig'

defineOptions({ name: 'CrmCluePoolConfig' })

const message = useMessage()
const { t } = useI18n()

const formLoading = ref(false)
const formData = ref<CrmCluePoolConfigVO>({})
const formRules = reactive<FormRules>({
  contactExpireDays: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (formData.value.enabled && (value == null || value < 1)) {
          callback(new Error('开启公海后必须填写未联系回收天数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  notifyDays: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (formData.value.notifyEnabled && (value == null || value < 1)) {
          callback(new Error('开启通知后必须填写提前通知天数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref<FormInstance>()

const loadConfig = async () => {
  formLoading.value = true
  try {
    formData.value = await CrmCluePoolConfigApi.getCrmCluePoolConfig()
  } finally {
    formLoading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    await CrmCluePoolConfigApi.saveCrmCluePoolConfig({
      enabled: formData.value.enabled,
      contactExpireDays: formData.value.contactExpireDays,
      notifyEnabled: formData.value.notifyEnabled,
      notifyDays: formData.value.notifyDays
    })
    message.success(t('common.updateSuccess'))
    await loadConfig()
  } finally {
    formLoading.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>
