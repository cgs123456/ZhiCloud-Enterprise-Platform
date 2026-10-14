<!-- TMS 车队运营记录（totalCost/profit 由服务端重算，只读展示） -->
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
      <el-form-item label="运营日期" prop="operationDate">
        <el-date-picker
          v-model="queryParams.operationDate"
          type="daterange"
          range-separator="~"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="!w-240px"
        />
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
          v-hasPermi="['tms:fleet-operation:create']"
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
      <el-table-column align="center" label="车辆编号" prop="vehicleId" min-width="100" />
      <el-table-column align="center" label="运营日期" prop="operationDate" width="120" />
      <el-table-column align="right" label="行驶里程" prop="mileage" min-width="110" />
      <el-table-column align="right" label="油耗" prop="fuelConsumption" min-width="100" />
      <el-table-column align="right" label="油费" prop="fuelCost" min-width="100" />
      <el-table-column align="right" label="维修保养费" prop="maintenanceCost" min-width="110" />
      <el-table-column align="right" label="保险费" prop="insuranceCost" min-width="100" />
      <el-table-column align="right" label="年检费" prop="inspectionCost" min-width="100" />
      <el-table-column align="right" label="运营收入" prop="revenue" min-width="110" />
      <el-table-column align="right" label="总运营成本" prop="totalCost" min-width="110">
        <template #default="scope">
          <span class="text-gray-500">{{ scope.row.totalCost ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column align="right" label="运营利润" prop="profit" min-width="110">
        <template #default="scope">
          <span :class="(scope.row.profit ?? 0) < 0 ? 'text-red-500' : 'text-green-600'">
            {{ scope.row.profit ?? 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['tms:fleet-operation:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['tms:fleet-operation:delete']"
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
  <TmsFleetOperationForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsFleetOperationApi } from '@/api/tms/fleet-operation'
import TmsFleetOperationForm from './TmsFleetOperationForm.vue'

defineOptions({ name: 'TmsFleetOperation' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  vehicleId: undefined,
  operationDate: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await TmsFleetOperationApi.getTmsFleetOperationPage(queryParams)
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
    await TmsFleetOperationApi.deleteTmsFleetOperation(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
