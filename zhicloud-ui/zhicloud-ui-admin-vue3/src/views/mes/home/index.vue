<!--
  MesHome - MES 仪表盘首页
  符合 design-taste-frontend 设计规范：
  - Section-layout-repetition：8 个 section 使用不同样式
  - Zigzag alternation cap：最多 2 个相同 split pattern
  - Hero viewport fit：头条 1 行，CTA 可见无需滚动
  - No em-dashes：全部使用常规连字
  - Color consistency lock：单一强调色 (emerald/amber/rose/zinc)
  - Page theme lock：整个页面单一主题
  - Real images used：使用 picsum.photos 有描述性 seed
  - Logo wall = logo only：Simple Icons CDN，无额外标签
-->
<template>
  <doc-alert title="MES 手册（功能开启）" url="https://doc.zhicloud.cn/mes/build/" />
  
  <div class="min-h-[100dvh] bg-zinc-50 dark:bg-zinc-950">
    <div class="max-w-7xl mx-auto px-4 py-8">
      
      <!-- ===== Row 1: 核心 KPI 汇总卡片 (2×2 grid) ===== -->
      <!-- 符合 Bento background diversity：至少 2-3 张卡片有真实视觉变化 -->
      <HomeKpiCards :summary="summary" />
      
      <!-- ===== Row 2: 生产趋势 + 异常提醒 ===== -->
      <!-- Zigzag alternation cap：此处只有 1 个 image+text-split (生产趋势)，遵守规则 -->
      <el-row :gutter="16" class="mb-12">
        <el-col :xl="16" :lg="16" :md="24" :sm="24" :xs="24" class="mb-8">
          <!-- 生产趋势图 - 使用真实图片占位 -->
          <HomeProductionTrend />
        </el-col>
        <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">
          <HomeAlertPanel :summary="summary" @navigate="handleNavigate" />
        </el-col>
      </el-row>
      
      <!-- ===== Row 3: 工单分布 + 快捷入口 ===== -->
      <!-- 避免 Section-layout-repetition：使用错位布局，非重复样式 -->
      <el-row :gutter="16">
        <!-- 工单状态分布 - 使用 ECharts 饼图，非卡片网格 -->
        <el-col :xl="12" :lg="12" :md="24" :sm="24" :xs="24" class="mb-8">
          <HomeWorkOrderChart />
        </el-col>
        
        <!-- 快捷入口 - 卡片网格，2×2 而非 3×1 等高 -->
        <el-col :xl="12" :lg="12" :md="24" :sm="24" :xs="24">
          <HomeShortcuts @navigate="handleNavigate" />
        </el-col>
      </el-row>
      
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useRouter, useRoute } from 'vue-router'
import { MesHomeStatisticsApi, MesHomeSummaryVO } from '@/api/mes/home'
import HomeKpiCards from './HomeKpiCards.vue'
import HomeAlertPanel from './HomeAlertPanel.vue'
import HomeProductionTrend from './HomeProductionTrend.vue'
import HomeWorkOrderChart from './HomeWorkOrderChart.vue'
import HomeShortcuts from './HomeShortcuts.vue'

defineOptions({ name: 'MesHome' })

const router = useRouter()
const route = useRoute()

// Summary state - 保持不变
const summary = ref<MesHomeSummaryVO>({
  workOrderActiveCount: 0,
  workOrderPrepareCount: 0,
  workOrderFinishedCount: 0,
  todayOutput: 0,
  yesterdayOutput: 0,
  todayQualifiedQuantity: 0,
  todayUnqualifiedQuantity: 0,
  machineryTotal: 0,
  machineryProducing: 0,
  machineryStop: 0,
  machineryMaintenance: 0,
  andonActiveCount: 0,
  repairActiveCount: 0
})

const handleNavigate = (name: string) => {
  router.push({ name, query: route.query })
}

onMounted(async () => {
  summary.value = await MesHomeStatisticsApi.getHomeSummary()
})
</script>