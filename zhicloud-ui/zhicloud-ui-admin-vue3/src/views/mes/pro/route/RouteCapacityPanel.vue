<!-- MES 工艺路线产能面板（route 维度产能：APS 时长估算优先取此值，未配置回退工位平均） -->
<template>
  <div v-loading="loading">
    <el-form label-width="140px" :disabled="!isEditable">
      <el-form-item label="产能（件/小时）">
        <el-input-number v-model="formData.capacity" :min="0" :precision="2" :step="1" class="!w-240px" />
        <span class="ml-10px text-gray-500">0 = 未配置（排产时长回退按工位平均产能估算）</span>
      </el-form-item>
      <el-form-item label="效率系数">
        <el-input-number v-model="formData.efficiency" :min="0.001" :precision="3" :step="0.1" class="!w-240px" />
        <span class="ml-10px text-gray-500">须大于 0（未配置按 1.0 处理）</span>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" placeholder="请输入备注" class="!w-360px" />
      </el-form-item>
      <el-form-item v-if="isEditable">
        <el-button type="primary" :disabled="loading" @click="handleSave">保 存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ProRouteCapacityApi, ProRouteCapacityVO } from '@/api/mes/pro/route/capacity'

defineOptions({ name: 'RouteCapacityPanel' })

const props = defineProps<{
  routeId: number
  formType: string
}>()

const message = useMessage() // 消息弹窗
const loading = ref(false) // 加载中
const isEditable = computed(() => ['create', 'update'].includes(props.formType)) // 是否为编辑模式
const formData = ref<ProRouteCapacityVO>({
  routeId: props.routeId,
  capacity: 0,
  efficiency: 1,
  remark: ''
})

/** 查询产能配置（未配置时展示默认哨兵：0 产能 / 1 效率） */
const getCapacity = async () => {
  loading.value = true
  try {
    const data = await ProRouteCapacityApi.getRouteCapacityByRoute(props.routeId)
    formData.value = data
      ? {
          routeId: props.routeId,
          capacity: Number(data.capacity ?? 0),
          efficiency: Number(data.efficiency ?? 1),
          remark: data.remark ?? ''
        }
      : { routeId: props.routeId, capacity: 0, efficiency: 1, remark: '' }
  } finally {
    loading.value = false
  }
}

/** 保存产能配置（upsert） */
const handleSave = async () => {
  loading.value = true
  try {
    await ProRouteCapacityApi.saveRouteCapacity({
      routeId: props.routeId,
      capacity: formData.value.capacity,
      efficiency: formData.value.efficiency,
      remark: formData.value.remark
    })
    message.success('产能配置已保存')
    await getCapacity()
  } finally {
    loading.value = false
  }
}

/** 监听路线编号变化加载数据 */
watch(
  () => props.routeId,
  (val) => {
    if (val) {
      getCapacity()
    }
  },
  { immediate: true }
)
</script>
