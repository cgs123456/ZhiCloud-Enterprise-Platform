<template>
  <doc-alert title="AI 手册" url="https://doc.zhicloud.cn/ai/build/" />

  <!-- 数据集操作栏 -->
  <ContentWrap>
    <el-form :inline="true" class="-mb-15px">
      <el-form-item label="数据集">
        <el-select
          v-model="datasetId"
          placeholder="请选择评估数据集"
          filterable
          class="!w-280px"
          :loading="datasetLoading"
          @change="onDatasetChange"
        >
          <el-option v-for="item in datasets" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button class="ml-10px" @click="loadDatasets">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" plain @click="createDialogVisible = true" v-hasPermi="['airag:eval:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 新建数据集
        </el-button>
        <el-button
          type="primary"
          :loading="running"
          :disabled="datasetId == null"
          @click="handleRun"
          v-hasPermi="['airag:eval:manage']"
        >
          <Icon icon="ep:video-play" class="mr-5px" /> 批量评估
        </el-button>
      </el-form-item>
    </el-form>
    <div v-if="running && report" class="mt-10px text-13px text-gray-500">
      执行中：{{ report.doneCount ?? 0 }} / {{ report.totalCount ?? 0 }} 题
      <el-progress
        :percentage="progressPercent"
        :stroke-width="8"
        class="mt-5px !w-400px"
      />
    </div>
  </ContentWrap>

  <!-- 最新报告 -->
  <ContentWrap v-if="report">
    <div class="mb-10px text-14px font-600">
      最新报告
      <el-tag class="ml-10px" :type="reportStatusType">{{ reportStatusText }}</el-tag>
    </div>
    <el-descriptions :column="4" border v-if="report.status === 1">
      <el-descriptions-item label="综合得分">{{ fmt(report.overallScore) }}</el-descriptions-item>
      <el-descriptions-item label="与上次分差">{{ fmtDelta(report.overallDelta) }}</el-descriptions-item>
      <el-descriptions-item label="忠实度">{{ fmt(report.avgFaithfulness) }}</el-descriptions-item>
      <el-descriptions-item label="回答相关性">{{ fmt(report.avgAnswerRelevancy) }}</el-descriptions-item>
      <el-descriptions-item label="上下文精确率">{{ fmt(report.avgContextPrecision) }}</el-descriptions-item>
      <el-descriptions-item label="上下文召回率">{{ fmt(report.avgContextRecall) }}</el-descriptions-item>
      <el-descriptions-item label="退步题目">{{ report.regressedQuestions || '无' }}</el-descriptions-item>
      <el-descriptions-item label="进步题目">{{ report.improvedQuestions || '无' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert v-if="report.status === 2" :title="report.errorMsg || '评估失败'" type="error" :closable="false" show-icon />
  </ContentWrap>

  <!-- 历史趋势 -->
  <ContentWrap v-if="trend.length > 0">
    <div class="mb-10px text-14px font-600">历史趋势（综合得分）</div>
    <Echart :options="trendChartOptions" :height="280" />
  </ContentWrap>

  <!-- 新建数据集弹窗 -->
  <el-dialog v-model="createDialogVisible" title="新建评估数据集" width="480px">
    <el-form :model="createForm" label-width="100px" ref="createFormRef">
      <el-form-item label="数据集名称" prop="name">
        <el-input v-model="createForm.name" placeholder="如：售后问答基线 v1" />
      </el-form-item>
      <el-form-item label="关联知识库" prop="knowledgeId">
        <el-input-number v-model="createForm.knowledgeId" :min="1" class="!w-full" placeholder="知识库编号" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="覆盖范围说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="handleCreate">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { AiragEvalApi, type AiragEvalDatasetVO, type AiragEvalReportVO } from '@/api/airag/eval'

defineOptions({ name: 'AiRagEval' })

const message = useMessage()

const datasets = ref<AiragEvalDatasetVO[]>([])
const datasetLoading = ref(false)
const datasetId = ref<number | null>(null)
const report = ref<AiragEvalReportVO | null>(null)
const trend = ref<AiragEvalReportVO[]>([])
const running = ref(false)
const creating = ref(false)
const createDialogVisible = ref(false)
const createForm = reactive({ name: '', knowledgeId: undefined as number | undefined, description: '' })
let pollTimer: number | null = null

const progressPercent = computed(() => {
  if (!report.value?.totalCount) return 0
  return Math.round(((report.value.doneCount ?? 0) / report.value.totalCount) * 100)
})
const reportStatusText = computed(() => {
  return report.value?.status === 1 ? '已完成' : report.value?.status === 2 ? '失败' : '执行中'
})
const reportStatusType = computed(() => {
  return report.value?.status === 1 ? 'success' : report.value?.status === 2 ? 'danger' : 'warning'
})

const trendChartOptions = computed(() => {
  // 趋势按时间正序
  const rows = [...trend.value].reverse()
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['综合得分'], bottom: 0 },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: rows.map((r) => (r.createTime || '').substring(5, 16)) },
    yAxis: { type: 'value', min: 0, max: 1 },
    series: [
      {
        name: '综合得分',
        type: 'line',
        smooth: true,
        data: rows.map((r) => r.overallScore),
        itemStyle: { color: '#10B981' },
        areaStyle: { color: 'rgba(16,185,129,0.15)' }
      }
    ]
  }
})

const loadDatasets = async () => {
  datasetLoading.value = true
  try {
    const data: any = await AiragEvalApi.getDatasetList()
    datasets.value = data ?? []
  } finally {
    datasetLoading.value = false
  }
}

const onDatasetChange = async () => {
  stopPoll()
  report.value = null
  trend.value = []
  if (datasetId.value == null) return
  await refreshReport()
  await loadTrend()
}

const refreshReport = async () => {
  if (datasetId.value == null) return
  const data: any = await AiragEvalApi.getLatestReport(datasetId.value)
  report.value = data ?? null
}

const loadTrend = async () => {
  if (datasetId.value == null) return
  const data: any = await AiragEvalApi.getReportTrend(datasetId.value, 20)
  trend.value = (data ?? []).filter((r: any) => r.status === 1 && r.overallScore != null)
}

const handleCreate = async () => {
  if (!createForm.name.trim() || createForm.knowledgeId == null) {
    message.warning('请填写数据集名称与关联知识库')
    return
  }
  creating.value = true
  try {
    await AiragEvalApi.createDataset({
      name: createForm.name.trim(),
      description: createForm.description,
      knowledgeId: createForm.knowledgeId
    })
    message.success('创建成功，请导入题目后执行评估')
    createDialogVisible.value = false
    createForm.name = ''
    createForm.description = ''
    await loadDatasets()
  } finally {
    creating.value = false
  }
}

const handleRun = async () => {
  if (datasetId.value == null) return
  running.value = true
  try {
    const reportId: any = await AiragEvalApi.runBatch(datasetId.value)
    message.success('已触发批量评估，自动轮询进度')
    await pollReport(reportId)
  } catch {
    running.value = false
  }
}

const pollReport = async (reportId: number) => {
  stopPoll()
  const tick = async () => {
    try {
      const data: any = await AiragEvalApi.getReport(reportId)
      report.value = data
      if (data?.status !== 0) {
        running.value = false
        stopPoll()
        await loadTrend()
        return
      }
    } catch {
      // 轮询失败保留上次状态，下次继续
    }
    pollTimer = window.setTimeout(tick, 5000)
  }
  await tick()
}

const stopPoll = () => {
  if (pollTimer != null) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

const fmt = (v?: number | null) => (v == null ? '-' : v.toFixed(3))
const fmtDelta = (v?: number | null) => {
  if (v == null) return '-'
  return (v >= 0 ? '+' : '') + v.toFixed(3)
}

onMounted(() => {
  loadDatasets()
})
onUnmounted(() => {
  stopPoll()
})
</script>
