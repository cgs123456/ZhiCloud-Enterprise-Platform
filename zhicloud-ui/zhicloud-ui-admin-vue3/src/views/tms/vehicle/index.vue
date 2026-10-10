<!-- TMS 车辆 -->
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
      <el-form-item label="车辆编号" prop="vehicleId">
        <el-input-number v-model="queryParams.vehicleId" class="!w-240px" :controls="false" clearable placeholder="请输入车辆编号" />
      </el-form-item>
      <el-form-item label="行驶里程（公里）" prop="mileage">
        <el-input-number v-model="queryParams.mileage" class="!w-240px" :controls="false" clearable placeholder="请输入行驶里程（公里）" />
      </el-form-item>
      <el-form-item label="油耗（升）" prop="fuelConsumption">
        <el-input-number v-model="queryParams.fuelConsumption" class="!w-240px" :controls="false" clearable placeholder="请输入油耗（升）" />
      </el-form-item>
      <el-form-item label="油费（元）" prop="fuelCost">
        <el-input-number v-model="queryParams.fuelCost" class="!w-240px" :controls="false" clearable placeholder="请输入油费（元）" />
      </el-form-item>
      <el-form-item label="维修保养费（元）" prop="maintenanceCost">
        <el-input-number v-model="queryParams.maintenanceCost" class="!w-240px" :controls="false" clearable placeholder="请输入维修保养费（元）" />
      </el-form-item>
      <el-form-item label="保险费（元）" prop="insuranceCost">
        <el-input-number v-model="queryParams.insuranceCost" class="!w-240px" :controls="false" clearable placeholder="请输入保险费（元）" />
      </el-form-item>
      <el-form-item label="年检费（元）" prop="inspectionCost">
        <el-input-number v-model="queryParams.inspectionCost" class="!w-240px" :controls="false" clearable placeholder="请输入年检费（元）" />
      </el-form-item>
      <el-form-item label="运营收入（元）" prop="revenue">
        <el-input-number v-model="queryParams.revenue" class="!w-240px" :controls="false" clearable placeholder="请输入运营收入（元）" />
      </el-form-item>
      <el-form-item label="总运营成本（元）" prop="totalCost">
        <el-input-number v-model="queryParams.totalCost" class="!w-240px" :controls="false" clearable placeholder="请输入总运营成本（元）" />
      </el-form-item>
      <el-form-item label="运营利润（元）" prop="profit">
        <el-input-number v-model="queryParams.profit" class="!w-240px" :controls="false" clearable placeholder="请输入运营利润（元）" />
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
          v-hasPermi="['tms:vehicle:create']"
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
      <el-table-column align="center" label="车辆编号" prop="vehicleId" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="运营日期" prop="operationDate" width="170" />
      <el-table-column align="center" label="行驶里程（公里）" prop="mileage" min-width="120" />
      <el-table-column align="center" label="油耗（升）" prop="fuelConsumption" min-width="120" />
      <el-table-column align="center" label="油费（元）" prop="fuelCost" min-width="120" />
      <el-table-column align="center" label="维修保养费（元）" prop="maintenanceCost" min-width="120" />
      <el-table-column align="center" label="保险费（元）" prop="insuranceCost" min-width="120" />
      <el-table-column align="center" label="年检费（元）" prop="inspectionCost" min-width="120" />
      <el-table-column align="center" label="运营收入（元）" prop="revenue" min-width="120" />
      <el-table-column align="center" label="总运营成本（元）" prop="totalCost" min-width="120" />
      <el-table-column align="center" label="运营利润（元）" prop="profit" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:vehicle:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:vehicle:delete']"
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
  <TmsVehicleForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsVehicleApi } from '@/api/tms/vehicle'
import TmsVehicleForm from './TmsVehicleForm.vue'

defineOptions({ name: 'TmsVehicle' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  vehicleId: undefined
  operationDate: undefined
  mileage: undefined
  fuelConsumption: undefined
  fuelCost: undefined
  maintenanceCost: undefined
  insuranceCost: undefined
  inspectionCost: undefined
  revenue: undefined
  totalCost: undefined
  profit: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsVehicleApi.getTmsVehiclePage(queryParams)
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
    await TmsVehicleApi.deleteTmsVehicle(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
