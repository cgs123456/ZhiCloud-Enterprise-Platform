<!-- MES PDA 报工（手持端当前用户报工记录，只读） -->
<template>
  <ContentWrap>
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="本页展示手持端（PDA）当前登录用户的报工记录。报工动作在手持设备上完成。"
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
      <el-table-column align="center" label="报工单号" prop="code" min-width="150" />
      <el-table-column align="center" label="工单号" prop="workOrderCode" min-width="150" />
      <el-table-column align="center" label="任务号" prop="taskCode" min-width="140" />
      <el-table-column align="center" label="产品编码" prop="itemCode" min-width="130" />
      <el-table-column align="center" label="产品名称" prop="itemName" min-width="150" show-overflow-tooltip />
      <el-table-column align="center" label="工序" prop="processName" min-width="130" show-overflow-tooltip />
      <el-table-column align="center" label="报工数量" prop="feedbackQuantity" min-width="110" />
      <el-table-column align="center" label="合格数" prop="qualifiedQuantity" min-width="100" />
      <el-table-column align="center" label="不合格数" prop="unqualifiedQuantity" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="报工时间" prop="feedbackTime" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="140" show-overflow-tooltip />
    </el-table>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MesPdaApi } from '@/api/mes/pda'

defineOptions({ name: 'MesPda' })

const loading = ref(true)
const list = ref<any[]>([])

const getList = async () => {
  loading.value = true
  try {
    list.value = await MesPdaApi.getMyFeedbackList()
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
