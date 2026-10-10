<!-- 财务报表 - 分组明细 -->
<template>
  <div class="mb-16px">
    <div class="mb-8px font-bold">{{ title }}</div>
    <el-table :data="items" :show-overflow-tooltip="true" :stripe="true" size="small">
      <el-table-column align="center" label="项目编码" prop="itemCode" min-width="140" />
      <el-table-column align="left" label="项目名称" prop="itemName" min-width="220" show-overflow-tooltip />
      <el-table-column align="right" label="金额" prop="amount" min-width="140" />
    </el-table>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'StatementSection' })

defineProps<{
  title: string
  items: any[]
}>()
</script>
