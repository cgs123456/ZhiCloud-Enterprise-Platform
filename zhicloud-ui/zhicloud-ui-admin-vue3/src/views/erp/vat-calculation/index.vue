<template>
  <ContentWrap>
    <!-- 增值税计算：销项税 / 进项税 / 应纳税额 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker v-model="queryParams.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker v-model="queryParams.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load" :loading="loading"><Icon icon="ep:search" class="mr-5px" /> 计算</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="outputTax !== null || inputTax !== null">
    <el-row :gutter="12">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="text-14px text-gray-500">销项税额</div>
          <div class="text-26px font-bold">{{ outputTax ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="text-14px text-gray-500">进项税额</div>
          <div class="text-26px font-bold">{{ inputTax ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="text-14px text-gray-500">应纳税额</div>
          <div class="text-26px font-bold text-danger">{{ payable?.payableTax ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-descriptions v-if="payable" :column="4" border class="mt-10px">
      <el-descriptions-item label="销项税额">{{ payable.outputTax ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="进项税额">{{ payable.inputTax ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="应纳税额">{{ payable.payableTax ?? '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap v-else>
    <el-empty description="请选择期间后计算" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { VatCalculationApi } from '@/api/erp/vat-calculation'

const message = useMessage()

defineOptions({ name: 'ErpVatCalculation' })

const loading = ref(false)
const outputTax = ref<any>(null)
const inputTax = ref<any>(null)
const payable = ref<any>(null)
const queryParams = reactive({
  startDate: undefined as any,
  endDate: undefined as any
})

const load = async () => {
  if (!queryParams.startDate || !queryParams.endDate) {
    message.warning('请选择开始与结束日期')
    return
  }
  loading.value = true
  try {
    const [out, inp, pay]: any = await Promise.all([
      VatCalculationApi.VatCalculationOutputTax(queryParams),
      VatCalculationApi.VatCalculationInputTax(queryParams),
      VatCalculationApi.VatCalculationPayableTax(queryParams)
    ])
    outputTax.value = out
    inputTax.value = inp
    payable.value = pay || null
  } finally {
    loading.value = false
  }
}
</script>
