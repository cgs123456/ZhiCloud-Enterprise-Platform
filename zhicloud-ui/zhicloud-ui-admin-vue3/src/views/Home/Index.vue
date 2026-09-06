<template>
  <div>
    <!-- 欢迎卡片 -->
    <el-card shadow="never">
      <div class="flex items-center">
        <el-avatar :src="avatar" :size="70" class="mr-16px">
          <img src="@/assets/imgs/avatar.gif" alt="" />
        </el-avatar>
        <div>
          <div class="text-20px font-600">{{ greeting }}，{{ username }}</div>
          <div class="mt-10px text-14px text-gray-500">{{ todayText }}</div>
        </div>
      </div>
    </el-card>

    <!-- 快捷入口 -->
    <el-card shadow="never" class="mt-8px">
      <template #header>
        <span>快捷入口</span>
      </template>
      <el-row :gutter="8">
        <el-col
          v-for="item in shortcuts"
          :key="item.name"
          :xl="6"
          :lg="6"
          :md="8"
          :sm="12"
          :xs="12"
          class="mb-8px"
        >
          <div
            class="shortcut-item flex cursor-pointer items-center rounded p-12px transition-colors hover:bg-[var(--el-fill-color-light)]"
            @click="router.push(item.url)"
          >
            <Icon
              :icon="item.icon"
              :size="24"
              class="mr-10px"
              color="var(--el-color-primary)"
            />
            <span class="text-15px">{{ item.name }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'
import { useRouter } from 'vue-router'

defineOptions({ name: 'Index' })

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const avatar = userStore.getUser.avatar
const username = userStore.getUser.nickname

// 按时间段问候
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 12) return t('workplace.greetingMorning')
  if (hour >= 12 && hour < 18) return t('workplace.greetingAfternoon')
  return t('workplace.greetingEvening')
})

const todayText = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `今天是 ${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 星期${week}`
})

// 快捷入口（对应系统真实模块菜单）；图标统一主色，保证全页单一强调色
const shortcuts = [
  { name: 'ERP 系统', icon: 'ep:data-analysis', url: '/erp' },
  { name: 'WMS 系统', icon: 'ep:box', url: '/wms' },
  { name: 'MES 系统', icon: 'ep:cpu', url: '/mes' },
  { name: 'CRM 系统', icon: 'ep:user', url: '/crm' },
  { name: 'OA 办公', icon: 'ep:office-building', url: '/oa' },
  { name: '流程管理', icon: 'ep:connection', url: '/bpm' },
  { name: 'AI 大模型', icon: 'tabler:ai', url: '/ai' },
  { name: '系统管理', icon: 'ep:setting', url: '/system' }
]
</script>

<style lang="scss" scoped>
@media (prefers-reduced-motion: no-preference) {
  // 快捷入口按压反馈：轻微下沉模拟物理按压
  .shortcut-item:active {
    transform: translateY(1px);
  }
}
</style>
