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
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:shipment:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:shipment:delete']"
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
  <TmsShipmentForm ref="formRef" @success="getList" />
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
  no: undefined
  carrierId: undefined
  vehicleId: undefined
  driverId: undefined
  originAddress: undefined
  destinationAddress: undefined
  shipmentType: undefined
  sourceOrderNo: undefined
  totalQuantity: undefined
  totalWeight: undefined
  totalVolume: undefined
  freightAmount: undefined
  departureTime: undefined
  estimatedArrivalTime: undefined
  actualArrivalTime: undefined
  status: undefined
  remark: undefined
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
