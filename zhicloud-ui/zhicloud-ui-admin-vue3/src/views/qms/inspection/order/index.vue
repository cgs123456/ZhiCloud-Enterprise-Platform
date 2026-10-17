<!-- QMS 检验单 -->
<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="100px"
    >
      <el-form-item label="检验单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" class="!w-240px" clearable placeholder="请输入检验单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验类型（10 IQC 来料检验 20 IPQC 过程检验 35 FQC 成品检验 30 OQC 出货检验）" prop="type">
        <el-input-number v-model="queryParams.type" class="!w-240px" :controls="false" clearable placeholder="请输入检验类型（10 IQC 来料检验 20 IPQC 过程检验 35 FQC 成品检验 30 OQC 出货检验）" />
      </el-form-item>
      <el-form-item label="供应商 ID" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" class="!w-240px" :controls="false" clearable placeholder="请输入供应商 ID" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="queryParams.batchNo" class="!w-240px" clearable placeholder="请输入批次号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单 ID" prop="workOrderId">
        <el-input-number v-model="queryParams.workOrderId" class="!w-240px" :controls="false" clearable placeholder="请输入工单 ID" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['qms:inspection-order:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="检验单号" prop="orderNo" min-width="110" />
      <el-table-column align="center" label="检验类型" prop="type" min-width="110" />
      <el-table-column align="center" label="供应商 ID" prop="supplierId" min-width="110" />
      <el-table-column align="center" label="批次号" prop="batchNo" min-width="110" />
      <el-table-column align="center" label="工单 ID" prop="workOrderId" min-width="110" />
      <el-table-column align="center" label="产品 ID" prop="productId" min-width="110" />
      <el-table-column align="center" label="检验员" prop="inspector" min-width="110" />
      <el-table-column align="center" label="检验时间" prop="inspectTime" width="170" />
      <el-table-column align="center" label="状态" min-width="110" prop="status">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.QMS_INSPECTION_ORDER_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="240">
        <template #default="scope">
          <!-- 10/20：提交检验（录入记录并按 AQL 判定） -->
          <el-button
            v-if="
              scope.row.status === InspectOrderStatus.PENDING ||
              scope.row.status === InspectOrderStatus.INSPECTING
            "
            v-hasPermi="['qms:inspection-order:submit']"
            link
            type="primary"
            @click="openSubmit(scope.row)"
          >
            提交检验
          </el-button>
          <el-button
            v-if="
              scope.row.status === InspectOrderStatus.PENDING ||
              scope.row.status === InspectOrderStatus.INSPECTING
            "
            v-hasPermi="['qms:inspection-order:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-if="
              scope.row.status === InspectOrderStatus.PENDING ||
              scope.row.status === InspectOrderStatus.INSPECTING
            "
            v-hasPermi="['qms:inspection-order:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            删除
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

  <!-- 表单弹窗 -->
  <InspectionOrderForm ref="formRef" @success="getList" />

  <!-- 提交检验（录入检验记录，按 AQL 判定为合格/不合格） -->
  <Dialog v-model="submitVisible" title="提交检验（录入检验记录）" width="920">
    <el-form label-width="90px">
      <el-form-item label="检验单号">
        <span>{{ currentRow?.orderNo }}（共 {{ submitRecords.length }} 条记录）</span>
      </el-form-item>
    </el-form>
    <el-table v-loading="submitLoading" :data="submitRecords" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="检验项 *" min-width="200">
        <template #default="scope">
          <el-select v-model="scope.row.itemId" filterable class="!w-1/1" placeholder="请选择检验项">
            <el-option v-for="item in itemOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column align="center" label="结果 *" min-width="110">
        <template #default="scope">
          <el-select v-model="scope.row.result" class="!w-1/1" placeholder="结果">
            <el-option v-for="item in RESULT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column align="center" label="严重度 *" min-width="110">
        <template #default="scope">
          <el-select v-model="scope.row.severity" class="!w-1/1" placeholder="严重度">
            <el-option v-for="item in SEVERITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column align="center" label="实测值" min-width="130">
        <template #default="scope">
          <el-input v-model="scope.row.measuredValue" placeholder="实测值" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="检验员" min-width="120">
        <template #default="scope">
          <el-input v-model="scope.row.inspector" placeholder="检验员" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="操作" width="80">
        <template #default="scope">
          <el-button link type="danger" @click="submitRecords.splice(scope.$index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button class="mt-10px" plain type="primary" @click="addRecord">
      <Icon class="mr-5px" icon="ep:plus" />
      添加记录
    </el-button>
    <template #footer>
      <el-button :disabled="submitLoading" type="primary" @click="submitInspection">提交检验</el-button>
      <el-button @click="submitVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { InspectionOrderApi } from '@/api/qms/inspection/order'
import {
  InspectionRecordApi,
  InspectionResultOptions as RESULT_OPTIONS,
  InspectionSeverityOptions as SEVERITY_OPTIONS
} from '@/api/qms/inspection/record'
import { InspectionItemApi } from '@/api/qms/inspection/item'
import { DICT_TYPE } from '@/utils/dict'
import InspectionOrderForm from './InspectionOrderForm.vue'

defineOptions({ name: 'InspectionOrder' })

const message = useMessage()

/** 检验单状态机（与后端 InspectionOrderStatusEnum 对齐；§12.239 接线判定闭环） */
const InspectOrderStatus = {
  PENDING: 10,
  INSPECTING: 20,
  PASSED: 30,
  FAILED: 40
} as const

// §12.255：检验结果/严重度选项常量已提升至 '@/api/qms/inspection/record'
// （InspectionResultOptions / InspectionSeverityOptions，与后端枚举单点对齐）。
const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  type: undefined,
  supplierId: undefined,
  batchNo: undefined,
  workOrderId: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InspectionOrderApi.getInspectionOrderPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()


const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await InspectionOrderApi.deleteInspectionOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

const currentRow = ref<any>({})

/** 提交检验：弹窗内录入/修订记录（预填已有记录） */
const submitVisible = ref(false)
const submitLoading = ref(false)
const submitRecords = ref<any[]>([])
const itemOptions = ref<any[]>([])

const addRecord = () => {
  submitRecords.value.push({
    itemId: undefined,
    result: undefined,
    severity: 20,
    measuredValue: undefined,
    inspector: undefined
  })
}

const openSubmit = async (row: any) => {
  currentRow.value = row
  submitRecords.value = []
  submitVisible.value = true
  try {
    const [records, items] = await Promise.all([
      InspectionRecordApi.getInspectionRecordPage({ orderId: row.id, pageNo: 1, pageSize: 100 }),
      InspectionItemApi.getInspectionItemPage({ pageNo: 1, pageSize: 100 })
    ])
    itemOptions.value = (items.list ?? []).map((it: any) => ({
      id: it.id,
      label: [it.code, it.name].filter(Boolean).join(' ')
    }))
    // 预填已有记录（§12.249：记录 RespVO 已返回 severity，此处仅对「空值」回退「严重」；
    // 用 ?? 而非 ||，让 null 之外的任何值都原样带回。severity 取值域由后端
    // InspectionSeverityEnum 约束为 [10, 20, 30]（致命/严重/轻微））
    submitRecords.value = (records.list ?? []).map((r: any) => ({
      itemId: r.itemId,
      result: r.result,
      severity: r.severity ?? 20,
      measuredValue: r.measuredValue,
      inspector: r.inspector
    }))
  } catch {
    // 读取失败时保持空表，由提交前校验兜底
  }
}

const submitInspection = async () => {
  if (submitRecords.value.length === 0) {
    message.warning('请至少添加一条检验记录')
    return
  }
  const invalid = submitRecords.value.findIndex((r: any) => !r.itemId || !r.result || !r.severity)
  if (invalid >= 0) {
    message.warning(`第 ${invalid + 1} 行记录不完整：检验项 / 结果 / 严重度均为必填`)
    return
  }
  submitLoading.value = true
  try {
    await InspectionOrderApi.submitInspectionOrder(
      currentRow.value.id,
      submitRecords.value.map((r: any) => ({
        orderId: currentRow.value.id,
        itemId: r.itemId,
        measuredValue: r.measuredValue || undefined,
        result: r.result,
        severity: r.severity,
        inspector: r.inspector || undefined
      }))
    )
    message.success('已提交检验')
    submitVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => getList())
</script>
