<!--
  HomeKpiCards - MES 首页核心 KPI 汇总卡片
  以 2×2 网格形式展示关键生产指标，
  符合 design-taste-frontend 设计规范：
  - 反中心偏差：分割布局而非三列等高
  - 色彩标定：zinc 中性基调 + 单一强调色
  - 形状一致性：统一圆角
  - 无纯黑阴影：使用 tinted 阴影
-->
<template>
  <div class="grid grid-cols-2 gap-6 sm:gap-8 md:gap-8 max-w-4xl mx-auto">
    <!-- KPI 卡片 1：生产工单 -->
    <div 
      class="rounded-2xl border border-zinc-200 dark:border-zinc-700 overflow-hidden transition-all duration-300 group kpi-card hover:border-emerald-300 dark:hover:border-emerald-800"
    >
      <div class="p-5 flex items-start justify-between">
        <div>
          <p class="text-zinc-500 text-sm font-medium uppercase tracking-wider mb-1">生产工单</p>
          <p class="text-2xl font-bold text-zinc-950 dark:text-zinc-100" :title="String(summary.workOrderActiveCount)">
            {{ summary.workOrderActiveCount }}
          </p>
        </div>
        
        <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 bg-emerald-100 dark:bg-emerald-900/30">
          <svg class="w-5 h-5 text-emerald-600 dark:text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      </div>
      
      <div class="p-3 flex items-baseline gap-2">
        <span class="text-zinc-400 text-xs uppercase tracking-wider">进行中</span>
        <span v-if="summary.workOrderActiveCount > 0" class="text-emerald-600 font-medium ml-1">×{{ summary.workOrderActiveCount }}</span>
        <span v-else class="text-zinc-500 ml-1">无</span>
      </div>
    </div>

    <!-- KPI 卡片 2：今日产量 -->
    <div 
      class="rounded-2xl border border-zinc-200 dark:border-zinc-700 overflow-hidden transition-all duration-300 group kpi-card hover:border-amber-300 dark:hover:border-amber-800"
    >
      <div class="p-5 flex items-start justify-between">
        <div>
          <p class="text-zinc-500 text-sm font-medium uppercase tracking-wider mb-1">今日产量</p>
          <p class="text-2xl font-bold text-zinc-950 dark:text-zinc-100" :title="String(summary.todayOutput)">
            {{ summary.todayOutput }} {{ summary.todayOutput > 0 ? '件' : '' }}
          </p>
        </div>
        
        <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 bg-amber-100 dark:bg-amber-900/30">
          <svg class="w-5 h-5 text-amber-600 dark:text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3-3m-3.3-2.3a4.5 4.5 0 00-1.8-.8M12 7v5m-5 1h5M12 15l3.5-3.5" />
          </svg>
        </div>
      </div>
      
      <div class="p-3 flex items-baseline gap-2">
        <span class="text-zinc-400 text-xs uppercase tracking-wider">昨日对比</span>
        <span v-if="summary.yesterdayOutput !== 0" class="text-amber-600 font-medium ml-1">{{ summary.yesterdayOutput > 0 ? '+' : '' }}{{ summary.yesterdayOutput }} 件</span>
        <span v-else class="text-zinc-500 ml-1">首次计量</span>
      </div>
    </div>

    <!-- KPI 卡片 3：质量合格率（常驻渲染，避免 2×2 网格出现空格子） -->
    <div
      class="rounded-2xl border border-zinc-200 dark:border-zinc-700 overflow-hidden transition-all duration-300 group kpi-card hover:border-rose-300 dark:hover:border-rose-800"
    >
      <div class="p-5 flex items-start justify-between">
        <div>
          <p class="text-zinc-500 text-sm font-medium uppercase tracking-wider mb-1">质量合格率</p>
          <p
            v-if="hasQualityData"
            class="text-2xl font-bold text-zinc-950 dark:text-zinc-100"
            :title="qualityRate.toFixed(1) + '%'"
          >
            {{ qualityRate.toFixed(1) }}%
          </p>
          <p v-else class="text-2xl font-bold text-zinc-400">--</p>
        </div>

        <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 bg-rose-100 dark:bg-rose-900/30">
          <svg class="w-5 h-5 text-rose-600 dark:text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      </div>

      <div class="p-3 flex items-baseline gap-2">
        <template v-if="hasQualityData">
          <span class="text-zinc-400 text-xs">合计</span>
          <span v-if="summary.todayQualifiedQuantity > 0" class="text-rose-600 font-medium ml-2" :title="`合格 ${summary.todayQualifiedQuantity}`">×{{ summary.todayQualifiedQuantity }}</span>
          <el-divider direction="vertical" class="text-zinc-500 opacity-50 w-2" v-if="summary.todayUnqualifiedQuantity > 0" />
          <span v-if="summary.todayUnqualifiedQuantity > 0" class="text-amber-600 font-medium ml-2" :title="`不良 ${summary.todayUnqualifiedQuantity}`">×{{ summary.todayUnqualifiedQuantity }}</span>
          <span v-else class="text-zinc-500 ml-2">优良</span>
        </template>
        <span v-else class="text-zinc-500 text-xs">暂无检验数据</span>
      </div>
    </div>

    <!-- KPI 卡片 4：设备状态 -->
    <div 
      class="rounded-2xl border border-zinc-200 dark:border-zinc-700 overflow-hidden transition-all duration-300 group kpi-card hover:border-zinc-300 dark:hover:border-zinc-400"
    >
      <div class="p-5 flex items-start justify-between">
        <div>
          <p class="text-zinc-500 text-sm font-medium uppercase tracking-wider mb-1">设备状态</p>
          <p class="text-2xl font-bold text-zinc-950 dark:text-zinc-100" :title="String(summary.machineryProducing)">
            {{ summary.machineryProducing }}
          </p>
        </div>
        
        <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 bg-zinc-900 dark:bg-zinc-100">
          <svg class="w-5 h-5 text-zinc-400 dark:text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h1v4h1m-1-4h1v4h1m4 0v12a8 8 0 01-16 0v-4m16 0v4a8 8 0 01-16 0v-4M5 13h14M5 13l14-1v1a2 2 0 002 2v1a2 2 0 002-2v-1m-14 4v-1a2 2 0 012-2h14a2 2 0 012 2v1m0-12V5a2 2 0 012-2h14a2 2 0 012 2v1M5 13l14 1v1a2 2 0 002 2v1a2 2 0 002-2v-1m14 0h-2.286m0 2H5.286m5.964 0a1 1 0 010 2.028L15.728 21a1 1 0 01-2 0L5.286 13a1 1 0 01-.964-0.8l1.321-1.87A1 1 0 0112 12.05l.964.695a1 1 0 01.736 1.087z" />
          </svg>
        </div>
      </div>
      
      <div class="p-3 text-sm text-zinc-500">
        <div class="flex items-baseline gap-1">
          <span>运行中：{{ summary.machineryProducing }} / {{ summary.machineryTotal }}</span>
          <el-divider direction="vertical" class="text-zinc-500 opacity-50 w-2" />
          <span class="text-red-400">停机：{{ summary.machineryStop }}</span>
          <el-divider direction="vertical" class="text-zinc-500 opacity-50 w-2" />
          <span class="text-orange-400">维护：{{ summary.machineryMaintenance }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { MesHomeSummaryVO } from '@/api/mes/home'

defineOptions({ name: 'HomeKpiCards' })

const props = defineProps<{
  summary: MesHomeSummaryVO
}>()

const hasQualityData = computed(() => {
  return props.summary.todayQualifiedQuantity + props.summary.todayUnqualifiedQuantity > 0
})

// Quality rate calculation (same as before)
const qualityRate = computed(() => {
  const total = props.summary.todayQualifiedQuantity + props.summary.todayUnqualifiedQuantity
  if (total === 0) return 0
  return (props.summary.todayQualifiedQuantity / total) * 100
})
</script>

<style lang="scss" scoped>
/* Motion transition for fade */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 悬停阴影：单一、低透明度的冷灰蓝色调阴影（tinted，不用纯黑） */
@media (prefers-reduced-motion: no-preference) {
  .kpi-card:hover {
    box-shadow: 0 4px 20px rgba(63, 78, 101, 0.16);
  }
}
</style>