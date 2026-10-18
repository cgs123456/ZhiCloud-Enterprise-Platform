<template>
  <el-date-picker
    v-model="monthRange"
    type="monthrange"
    value-format="YYYY-MM"
    start-placeholder="开始月份"
    end-placeholder="结束月份"
    class="!w-240px"
    :clearable="false"
    :disabled-date="disabledDate"
    @change="emit('change', $event)"
  />
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { useFmsStore } from '@/views/fms/store/fms'

defineOptions({ name: 'FmsLedgerMonthRangePicker' })

const monthRange = defineModel<string[]>({ required: true }) // 会计期间范围
const emit = defineEmits<{ change: [value: string[] | undefined] }>()
const fmsStore = useFmsStore() // FMS 状态

/** 账套启用月份（yyyy-MM） */
const accountSetStartMonth = computed(() => {
  const accountSet = fmsStore.getAccountSetList.find((item) => item.id === fmsStore.getAccountSetId)
  const startTime = accountSet?.startTime
  // 账套启用期间是 yyyyMM 数字（如 202601）：不能直接交给 dayjs(number)（会被当作毫秒时间戳解析，
  // 重建时按字符串切月——与期初余额页 isJanuary 判定同源缺陷的修正）
  if (!startTime) {
    return undefined
  }
  const month = String(startTime)
  return month.length === 6 ? `${month.slice(0, 4)}-${month.slice(4, 6)}` : undefined
})

/** 禁用账套启用月份之前的日期 */
function disabledDate(date: Date) {
  return Boolean(
    accountSetStartMonth.value && dayjs(date).format('YYYY-MM') < accountSetStartMonth.value
  )
}
</script>
