<template>
  <el-select
    :clearable="clearable"
    :disabled="disabled"
    :filterable="filterable"
    :loading="loading"
    :model-value="modelValue"
    :placeholder="placeholder"
    @update:model-value="handleChange"
  >
    <el-option
      v-for="subject in subjectOptions"
      :key="subject.id"
      :label="formatSubjectOption(subject)"
      :value="subject.id as number"
    />
  </el-select>
</template>

<script lang="ts" setup>
import { FmsSubjectApi } from '@/api/fms/config/subject'
import type { FmsSubjectVO } from '@/api/fms/config/subject'
import { handleTree, treeToList } from '@/utils/tree'
import { FMS_SUBJECT_STATUS } from '@/views/fms/utils/constants'

defineOptions({ name: 'FmsSubjectSelect' })

const props = withDefaults(
  defineProps<{
    modelValue?: number
    options?: FmsSubjectVO[]
    disabled?: boolean
    clearable?: boolean
    filterable?: boolean
    placeholder?: string
  }>(),
  {
    disabled: false,
    clearable: false,
    filterable: true,
    placeholder: '请选择科目'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
  change: [value: number | undefined]
}>()

const loading = ref(false) // 会计科目列表的加载中
const subjectList = ref<FmsSubjectVO[]>([]) // 自动加载的会计科目列表（已按树序平铺）
const subjectOptions = computed(() =>
  props.options ? treeToList<FmsSubjectVO[]>(props.options) : subjectList.value
) // 平铺会计科目列表

/** 格式化会计科目选项 */
function formatSubjectOption(subject: FmsSubjectVO) {
  const indent = '　'.repeat(Math.max((subject.level || 1) - 1, 0))
  const statusLabel = subject.status === FMS_SUBJECT_STATUS.DISABLED ? '（已停用）' : ''
  return `${indent}${subject.code} ${subject.name}${statusLabel}`
}

/** 选中变化 */
function handleChange(value: unknown) {
  const subjectId = typeof value === 'number' ? value : undefined
  emit('update:modelValue', subjectId)
  emit('change', subjectId)
}

/** 获得会计科目列表（未传入选项时自加载；科目单一真相在 ERP /erp/gl-account/list） */
async function getSubjectList() {
  if (props.options) return
  loading.value = true
  try {
    subjectList.value = treeToList<FmsSubjectVO[]>(
      handleTree(await FmsSubjectApi.getSubjectList())
    )
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getSubjectList()
})
</script>
