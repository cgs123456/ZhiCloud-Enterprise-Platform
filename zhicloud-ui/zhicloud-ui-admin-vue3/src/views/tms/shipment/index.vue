<!-- TMS 运单 -->
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
      <el-form-item label="运单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入运单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="承运商编号" prop="carrierId">
        <el-input-number v-model="queryParams.carrierId" class="!w-240px" :controls="false" clearable placeholder="请输入承运商编号" />
      </el-form-item>
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number v-model="queryParams.vehicleId" class="!w-240px" :controls="false" clearable placeholder="请输入车辆编号" />
      </el-form-item>
      <el-form-item label="司机编号" prop="driverId">
        <el-input-number v-model="queryParams.driverId" class="!w-240px" :controls="false" clearable placeholder="请输入司机编号" />
      </el-form-item>
      <el-form-item label="起点地址" prop="originAddress">
        <el-input v-model="queryParams.originAddress" class="!w-240px" clearable placeholder="请输入起点地址" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="终点地址" prop="destinationAddress">
        <el-input v-model="queryParams.destinationAddress" class="!w-240px" clearable placeholder="请输入终点地址" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）" prop="shipmentType">
        <el-input-number v-model="queryParams.shipmentType" class="!w-240px" :controls="false" clearable placeholder="请输入运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）" />
      </el-form-item>
      <el-form-item label="来源单据号" prop="sourceOrderNo">
        <el-input v-model="queryParams.sourceOrderNo" class="!w-240px" clearable placeholder="请输入来源单据号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合计数量" prop="totalQuantity">
        <el-input-number v-model="queryParams.totalQuantity" class="!w-240px" :controls="false" clearable placeholder="请输入合计数量" />
      </el-form-item>
      <el-form-item label="合计重量" prop="totalWeight">
        <el-input-number v-model="queryParams.totalWeight" class="!w-240px" :controls="false" clearable placeholder="请输入合计重量" />
      </el-form-item>
      <el-form-item label="合计体积" prop="totalVolume">
        <el-input-number v-model="queryParams.totalVolume" class="!w-240px" :controls="false" clearable placeholder="请输入合计体积" />
      </el-form-item>
      <el-form-item label="运费金额" prop="freightAmount">
        <el-input-number v-model="queryParams.freightAmount" class="!w-240px" :controls="false" clearable placeholder="请输入运费金额" />
      </el-form-item>
      <el-form-item label="状态（10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消）" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态（10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
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
          v-hasPermi="['tms:shipment:create']"
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
      <el-table-column align="center" label="运单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="承运商编号" prop="carrierId" min-width="120" />
      <el-table-column align="center" label="车辆编号" prop="vehicleId" min-width="120" />
      <el-table-column align="center" label="司机编号" prop="driverId" min-width="120" />
      <el-table-column align="center" label="起点地址" prop="originAddress" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="终点地址" prop="destinationAddress" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）" prop="shipmentType" min-width="120" />
      <el-table-column align="center" label="来源单据号" prop="sourceOrderNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="合计数量" prop="totalQuantity" min-width="120" />
      <el-table-column align="center" label="合计重量" prop="totalWeight" min-width="120" />
      <el-table-column align="center" label="合计体积" prop="totalVolume" min-width="120" />
      <el-table-column align="center" label="运费金额" prop="freightAmount" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="发车时间" prop="departureTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="预计到达时间" prop="estimatedArrivalTime" width="170" />
      <el-table-column align="center" :formatter="dateFormatter" label="实际到达时间" prop="actualArrivalTime" width="170" />
      <el-table-column align="center" label="状态（10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消）" prop="status" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="330">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:shipment:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-if="scope.row.status === 10"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:update']"
            link
            type="warning"
            @click="openDispatch(scope.row)"
            v-if="scope.row.status === 10"
          >
            调度派车
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:update']"
            link
            type="success"
            @click="handleConfirmArrival(scope.row)"
            v-if="scope.row.status === 20"
          >
            确认到达
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:update']"
            link
            type="success"
            @click="handleConfirmSign(scope.row)"
            v-if="scope.row.status === 30"
          >
            确认签收
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:query']"
            link
            type="info"
            @click="openStops(scope.row)"
          >
            途经站点
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-if="scope.row.status === 10"
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
  <TmsShipmentForm ref="formRef" @success="getList" />

  <!-- 调度派车弹窗：为运单匹配车辆与司机 -->
  <Dialog v-model="dispatchVisible" title="调度派车" width="480">
    <el-form
      ref="dispatchFormRef"
      v-loading="dispatchLoading"
      :model="dispatchForm"
      :rules="dispatchRules"
      label-width="110px"
    >
      <el-form-item label="运单号">
        <el-input :model-value="currentRow?.no" disabled class="!w-1/1" />
      </el-form-item>
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number v-model="dispatchForm.vehicleId" :controls="false" :min="1" placeholder="请输入车辆编号" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="司机编号" prop="driverId">
        <el-input-number v-model="dispatchForm.driverId" :controls="false" :min="1" placeholder="请输入司机编号" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="dispatchLoading" @click="submitDispatch">确认派车</el-button>
      <el-button @click="dispatchVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 途经站点弹窗 -->
  <Dialog v-model="stopsVisible" title="途经站点" width="680">
    <el-table v-loading="stopsLoading" :data="stops" stripe>
      <el-table-column align="center" label="顺序" prop="stopOrder" width="80" />
      <el-table-column align="center" label="地址" prop="address" min-width="160" show-overflow-tooltip />
      <el-table-column align="center" label="联系人" prop="contactName" width="110" />
      <el-table-column align="center" label="联系电话" prop="contactPhone" width="130" />
      <el-table-column align="center" label="站点类型" prop="stopType" width="110" />
      <el-table-column align="center" label="状态" prop="status" width="90" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
    </el-table>
    <el-empty v-if="!stopsLoading && stops.length === 0" description="暂无途经站点" />
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsShipmentApi } from '@/api/tms/shipment'
import TmsShipmentForm from './TmsShipmentForm.vue'

defineOptions({ name: 'TmsShipment' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  carrierId: undefined,
  vehicleId: undefined,
  driverId: undefined,
  originAddress: undefined,
  destinationAddress: undefined,
  shipmentType: undefined,
  sourceOrderNo: undefined,
  totalQuantity: undefined,
  totalWeight: undefined,
  totalVolume: undefined,
  freightAmount: undefined,
  departureTime: undefined,
  estimatedArrivalTime: undefined,
  actualArrivalTime: undefined,
  status: undefined,
  remark: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsShipmentApi.getTmsShipmentPage(queryParams)
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

// ========== 运输状态机：10 待发车 -> 20 运输中 -> 30 已到达 -> 40 已签收 ==========
const currentRow = ref<any>(null)

// ---- 调度派车（10 -> 20） ----
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchFormRef = ref()
const dispatchForm = ref<any>({})
const dispatchRules = reactive({
  vehicleId: [{ required: true, message: '车辆编号不能为空', trigger: 'blur' }],
  driverId: [{ required: true, message: '司机编号不能为空', trigger: 'blur' }]
})

const openDispatch = (row: any) => {
  currentRow.value = row
  dispatchForm.value = { id: row.id, vehicleId: undefined, driverId: undefined }
  dispatchVisible.value = true
}

const submitDispatch = async () => {
  await dispatchFormRef.value.validate()
  dispatchLoading.value = true
  try {
    await TmsShipmentApi.dispatch(dispatchForm.value)
    message.success('已派车，运单进入运输中')
    dispatchVisible.value = false
    await getList()
  } finally {
    dispatchLoading.value = false
  }
}

/** 确认到达（20 -> 30） */
const handleConfirmArrival = async (row: any) => {
  try {
    await message.confirm(`确认运单「${row.no}」已到达目的地？`)
    await TmsShipmentApi.confirmArrival(row.id)
    message.success('已确认到达')
    await getList()
  } catch {}
}

/** 确认签收（30 -> 40） */
const handleConfirmSign = async (row: any) => {
  try {
    await message.confirm(`确认运单「${row.no}」已签收？`)
    await TmsShipmentApi.confirmSign(row.id)
    message.success('已确认签收')
    await getList()
  } catch {}
}

// ---- 途经站点 ----
const stopsVisible = ref(false)
const stopsLoading = ref(false)
const stops = ref<any[]>([])

const openStops = async (row: any) => {
  currentRow.value = row
  stopsVisible.value = true
  stopsLoading.value = true
  try {
    stops.value = (await TmsShipmentApi.getShipmentStopList(row.id)) || []
  } finally {
    stopsLoading.value = false
  }
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TmsShipmentApi.deleteTmsShipment(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
