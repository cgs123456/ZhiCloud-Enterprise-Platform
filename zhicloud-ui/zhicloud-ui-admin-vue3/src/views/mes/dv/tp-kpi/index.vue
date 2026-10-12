<template>
  <ContentWrap>
    <!-- TPM KPI：计算设备 MTBF/MTTR 等指标，并查看历史 -->
    <el-form class="-mb-15px" :model="calcForm" :inline="true" label-width="90px">
      <el-form-item label="设备" prop="equipmentId">
        <el-input-number v-model="calcForm.equipmentId" :controls="false" placeholder="设备 ID" class="!w-160px" />
      </el-form-item>
      <el-form-item label="期间" prop="period">
        <el-input v-model="calcForm.period" placeholder="如 2026-09" class="!w-160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleCalculate" :loading="calcLoading">计算 KPI</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 计算结果 -->
  <ContentWrap v-if="current">
    <el-descriptions :column="4" border title="最新计算结果">
      <el-descriptions-item label="设备">{{ current.equipmentId }}</el-descriptions-item>
      <el-descriptions-item label="期间">{{ current.period }}</el-descriptions-item>
      <el-descriptions-item label="MTBF（平均故障间隔）">{{ current.mtbf ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="MTTR（平均修复时间）">{{ current.mttr ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="OEE 提升">{{ current.oeeImprovement ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="计划停机">{{ current.plannedDowntime ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="非计划停机">{{ current.unplannedDowntime ?? '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <!-- 历史查询 -->
  <ContentWrap>
    <el-form class="-mb-15px" :model="historyForm" :inline="true" label-width="90px">
      <el-form-item label="设备" prop="equipmentId">
        <el-input-number v-model="historyForm.equipmentId" :controls="false" placeholder="设备 ID" class="!w-160px" />
      </el-form-item>
      <el-form-item label="期间列表" prop="periodsText">
        <el-input v-model="historyForm.periodsText" placeholder="逗号分隔，如 2026-07,2026-08,2026-09" class="!w-320px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="loadHistory" :loading="historyLoading">查询历史</el-button>
      </el-form-item>
    </el-form>
    <el-table v-if="history.length" :data="history" class="mt-10px" stripe>
      <el-table-column label="期间" align="center" prop="period" />
      <el-table-column label="MTBF" align="center" prop="mtbf" />
      <el-table-column label="MTTR" align="center" prop="mttr" />
      <el-table-column label="OEE 提升" align="center" prop="oeeImprovement" />
      <el-table-column label="计划停机" align="center" prop="plannedDowntime" />
      <el-table-column label="非计划停机" align="center" prop="unplannedDowntime" />
    </el-table>
    <el-empty v-else description="输入设备与间列表询历史" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { TpKpiApi } from '@/api/mes/dv/tp-kpi'

defineOptions({ name: 'MesTpKpi' })

const message = useMessage()

const calcLoading = ref(false)
const historyLoading = ref(false)
const current = ref<any>(null)
const history = ref<any[]>([])
const calcForm = reactive({
  equipmentId: undefined as any,
  period: undefined as any
})
const historyForm = reactive({
  equipmentId: undefined as any,
  periodsText: ''
})

const handleCalculate = async () => {
  if (!calcForm.equipmentId || !calcForm.period) {
    message.warning('请输入设备与期间')
    return
  }
  calcLoading.value = true
  try {
    current.value = await TpKpiApi.TpKpiCalculate({ ...calcForm })
  } finally {
    calcLoading.value = false
  }
}

const loadHistory = async () => {
  if (!historyForm.equipmentId || !historyForm.periodsText) {
    message.warning('请输入设备与期间列表')
    return
  }
  historyLoading.value = true
  try {
    const res = await TpKpiApi.TpKpiHistory({
      equipmentId: historyForm.equipmentId,
      periods: historyForm.periodsText.split(',').map((s) => s.trim()).filter(Boolean)
    })
    history.value = Array.isArray(res) ? res : []
  } finally {
    historyLoading.value = false
  }
}
</script>
