<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="请输入编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="partnerType" prop="partnerType">
        <el-input-number v-model="queryParams.partnerType" placeholder="请输入partnerType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="partnerId" prop="partnerId">
        <el-input-number v-model="queryParams.partnerId" placeholder="请输入partnerId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:cpfr-forecast:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="no" />
      <el-table-column label="partnerType" align="center" prop="partnerType" />
      <el-table-column label="partnerId" align="center" prop="partnerId" />
      <el-table-column label="productId" align="center" prop="productId" />
      <el-table-column label="productName" align="center" prop="productName" />
      <el-table-column label="forecastPeriod" align="center" prop="forecastPeriod" />
      <el-table-column label="forecastQuantity" align="center" prop="forecastQuantity" />
      <el-table-column label="actualQuantity" align="center" prop="actualQuantity" />
      <el-table-column label="deviationRate" align="center" prop="deviationRate" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:cpfr-forecast:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:cpfr-forecast:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <CpfrForecastForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { CpfrForecastApi } from '@/api/erp/cpfr-forecast'
import CpfrForecastForm from './cpfrForecastForm.vue'

defineOptions({ name: 'ErpCpfrForecast' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined
  partnerType: undefined
  partnerId: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CpfrForecastApi.getCpfrForecastPage(queryParams)
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

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await CpfrForecastApi.deleteCpfrForecast(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
