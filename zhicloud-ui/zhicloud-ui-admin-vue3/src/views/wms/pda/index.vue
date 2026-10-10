<!-- WMS PDA 作业（手持端待执行任务，只读） -->
<template>
  <ContentWrap>
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="本页展示手持端（PDA）待执行任务：拣货 / 上架 / 盘点。执行动作在手持设备上完成。"
    />
    <el-form :inline="true" class="mb-12px">
      <el-form-item>
        <el-button @click="getList">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="任务类型" prop="taskType" min-width="110" />
      <el-table-column align="center" label="单据号" prop="orderNo" min-width="160" />
      <el-table-column align="center" label="仓库" prop="warehouseName" min-width="130" />
      <el-table-column align="center" label="SKU 编码" prop="skuCode" min-width="130" />
      <el-table-column align="center" label="SKU 名称" prop="skuName" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="物料编码" prop="itemCode" min-width="130" />
      <el-table-column align="center" label="物料名称" prop="itemName" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="单位" prop="unit" min-width="80" />
      <el-table-column align="center" label="需求数量" prop="demandQuantity" min-width="110" />
      <el-table-column align="center" label="已执行数量" prop="executedQuantity" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="单据时间" prop="orderTime" width="170" />
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { WmsPdaApi } from '@/api/wms/pda'

defineOptions({ name: 'WmsPda' })

const loading = ref(true)
const list = ref<any[]>([])

const getList = async () => {
  loading.value = true
  try {
    list.value = await WmsPdaApi.getTaskList()
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
