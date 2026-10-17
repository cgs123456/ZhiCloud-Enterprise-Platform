<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="计划编号" prop="planNo">
        <el-input v-model="queryParams.planNo" placeholder="请输入计划编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-240px">
          <el-option
            v-for="(label, value) in MRP_PLAN_STATUS_LABEL"
            :key="value"
            :label="label"
            :value="Number(value)"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="计划编号" align="center" prop="planNo" />
      <el-table-column label="计划日期" align="center" prop="planDate" width="180" :formatter="dateFormatter" />
      <el-table-column label="状态" align="center" prop="status" :formatter="statusFormatter" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <!-- 0 草稿：计算（计算后方可确认） -->
          <el-button
            v-if="scope.row.status === MrpPlanStatus.DRAFT"
            v-hasPermi="['mes:pro-mrp:calculate']"
            link
            type="primary"
            @click="handleCalculate(scope.row)"
          >
            计算
          </el-button>
          <!-- 1 已计算：确认（确认后锁定为已确认，不可再计算） -->
          <el-button
            v-if="scope.row.status === MrpPlanStatus.CALCULATED"
            v-hasPermi="['mes:pro-mrp:calculate']"
            link
            type="primary"
            @click="handleConfirm(scope.row)"
          >
            确认
          </el-button>
          <!-- 2 已确认：转采购/生产建议（一次性终态；逐行落地草稿工单） -->
          <el-button
            v-if="scope.row.status === MrpPlanStatus.CONFIRMED"
            v-hasPermi="['mes:pro-mrp:calculate']"
            link
            type="primary"
            @click="handleConvert(scope.row)"
          >
            转建议
          </el-button>
          <el-button v-hasPermi="['mes:pro-mrp:query']" link type="primary" @click="openResult(scope.row)">
            结果
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- MRP 计算结果弹窗 -->
  <Dialog v-model="resultVisible" :title="`MRP 计算结果 - ${currentRow?.planNo || ''}`" width="900">
    <el-table v-loading="resultLoading" :data="resultList" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="物料" align="center" prop="productId" />
      <el-table-column label="需求量" align="center" prop="requirementQty" />
      <el-table-column label="库存量" align="center" prop="stockQty" />
      <el-table-column label="净需求" align="center" prop="netRequirement" />
      <el-table-column label="安全库存" align="center" prop="safetyStock" />
      <el-table-column label="批量规则" align="center" prop="lotSizeRule" />
      <el-table-column label="计划订单量" align="center" prop="plannedOrderQty" />
      <el-table-column label="计划订单日期" align="center" prop="plannedOrderDate" width="180" :formatter="dateFormatter" />
      <el-table-column label="供应商" align="center" prop="supplierId" />
    </el-table>
  </Dialog>
</template>
<script setup lang="ts">
import { MrpApi } from '@/api/mes/pro/mrp'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'MesMrp' })

// MRP 计划状态（对齐 MesProMrpPlanStatusEnum，字典无种子故用页内映射）
const MrpPlanStatus = {
  DRAFT: 0,
  CALCULATED: 1,
  CONFIRMED: 2,
  CONVERTED: 3
}
const MRP_PLAN_STATUS_LABEL: Record<number, string> = {
  0: '草稿',
  1: '已计算',
  2: '已确认',
  3: '已转建议'
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  planNo: undefined,
  status: undefined
})
const queryFormRef = ref()
const message = useMessage()

const statusFormatter = (_row: any, _column: any, cellValue: any) =>
  MRP_PLAN_STATUS_LABEL[cellValue] ?? cellValue

const getList = async () => {
  loading.value = true
  try {
    const data = await MrpApi.MrpPlanList(queryParams)
    list.value = data.list || data
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 计算（0 草稿 → 1 已计算；计算后方可确认） */
const handleCalculate = async (row: any) => {
  try {
    await message.confirm(`是否对 MRP 计划「${row.planNo}」执行计算？`)
    await MrpApi.MrpCalculate(row.id)
    message.success('MRP 计算完成')
    await getList()
  } catch {}
}

/** 确认（1 已计算 → 2 已确认；确认后计划锁定，不可再计算） */
const handleConfirm = async (row: any) => {
  try {
    await message.confirm(`是否确认 MRP 计划「${row.planNo}」？确认后计划将锁定。`)
    await MrpApi.MrpPlanConfirm(row.id)
    message.success('MRP 计划已确认')
    await getList()
  } catch {}
}

/** 转采购/生产建议（2 已确认 → 3 已转建议；有 BOM→生产型、无 BOM→采购型草稿工单） */
const handleConvert = async (row: any) => {
  try {
    await message.confirm(`是否将 MRP 计划「${row.planNo}」转采购/生产建议？计划将锁定为「已转建议」。`)
    const data = await MrpApi.MrpPlanConvert(row.id)
    message.success(`已转建议 ${data?.totalCount ?? 0} 张（生产 ${data?.produceCount ?? 0} / 采购 ${data?.purchaseCount ?? 0}）`)
    await getList()
  } catch {}
}

/** 计算结果弹窗 */
const resultVisible = ref(false)
const resultLoading = ref(false)
const resultList = ref<any[]>([])
const currentRow = ref<any>({})

const openResult = async (row: any) => {
  currentRow.value = row
  resultVisible.value = true
  resultLoading.value = true
  try {
    const data = await MrpApi.getMrpResult(row.id)
    resultList.value = data || []
  } finally {
    resultLoading.value = false
  }
}

onMounted(() => getList())
</script>
