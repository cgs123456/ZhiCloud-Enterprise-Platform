<!-- QMS SPC 控制图（过程能力分析，只读） -->
<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-12px" label-width="90px">
      <el-form-item label="产品 ID">
        <el-input-number v-model="analyzeParams.itemId" class="!w-200px" :controls="false" placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="runAnalyze">
          <Icon class="mr-5px" icon="ep:data-analysis" />
          分析
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="analysis" v-loading="loading" title="过程能力分析">
    <el-descriptions :column="4" border>
      <el-descriptions-item label="样本数">{{ analysis.sampleCount }}</el-descriptions-item>
      <el-descriptions-item label="均值">{{ analysis.mean }}</el-descriptions-item>
      <el-descriptions-item label="标准差">{{ analysis.stdDev }}</el-descriptions-item>
      <el-descriptions-item label="控制图类型">{{ analysis.chartType }}</el-descriptions-item>
      <el-descriptions-item label="上控制限">{{ analysis.upperControlLimit }}</el-descriptions-item>
      <el-descriptions-item label="下控制限">{{ analysis.lowerControlLimit }}</el-descriptions-item>
      <el-descriptions-item label="Cp">{{ analysis.cp }}</el-descriptions-item>
      <el-descriptions-item label="Cpk">{{ analysis.cpk }}</el-descriptions-item>
      <el-descriptions-item label="能力等级">{{ analysis.capabilityLevel }}</el-descriptions-item>
      <el-descriptions-item label="失控点数">{{ analysis.outOfControlCount }}</el-descriptions-item>
      <el-descriptions-item label="上规格限">{{ analysis.upperSpecLimit }}</el-descriptions-item>
      <el-descriptions-item label="下规格限">{{ analysis.lowerSpecLimit }}</el-descriptions-item>
    </el-descriptions>

    <div class="mt-12px mb-8px font-bold">规则违例</div>
    <el-table :data="analysis.ruleViolations || []" :stripe="true" size="small">
      <el-table-column align="center" label="规则" prop="ruleCode" min-width="120" />
      <el-table-column align="left" label="说明" prop="ruleName" min-width="240" show-overflow-tooltip />
      <el-table-column align="center" label="命中点数" prop="violationCount" min-width="120" />
    </el-table>
  </ContentWrap>

  <ContentWrap title="抽样计划（GB/T 2828.1）">
    <el-form :inline="true" class="mb-12px" label-width="90px">
      <el-form-item label="批量">
        <el-input-number v-model="planParams.lotSize" class="!w-200px" :controls="false" placeholder="请输入批量" />
      </el-form-item>
      <el-form-item>
        <el-button @click="runPlan">查询抽样方案</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="plan" :column="4" border>
      <el-descriptions-item label="批量下限">{{ plan.lotSizeFrom }}</el-descriptions-item>
      <el-descriptions-item label="批量上限">{{ plan.lotSizeTo }}</el-descriptions-item>
      <el-descriptions-item label="样本量字码">{{ plan.codeLetter }}</el-descriptions-item>
      <el-descriptions-item label="检验水平">{{ plan.inspectionLevel }}</el-descriptions-item>
      <el-descriptions-item label="样本量">{{ plan.sampleSize }}</el-descriptions-item>
      <el-descriptions-item label="AQL">{{ plan.aql }}</el-descriptions-item>
      <el-descriptions-item label="正常-接收">{{ plan.normalAccept }}</el-descriptions-item>
      <el-descriptions-item label="正常-拒收">{{ plan.normalReject }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { SpcApi } from '@/api/qms/spc'

defineOptions({ name: 'Spc' })

const loading = ref(false)
const analysis = ref<any>(null)
const plan = ref<any>(null)

const analyzeParams = reactive<any>({ itemId: undefined })
const planParams = reactive<any>({ lotSize: undefined })

const runAnalyze = async () => {
  if (!analyzeParams.itemId) return
  loading.value = true
  try {
    analysis.value = await SpcApi.analyzeSpc({ itemId: analyzeParams.itemId })
  } finally {
    loading.value = false
  }
}

const runPlan = async () => {
  if (!planParams.lotSize) return
  plan.value = await SpcApi.getSamplingPlan({ lotSize: planParams.lotSize })
}
</script>
