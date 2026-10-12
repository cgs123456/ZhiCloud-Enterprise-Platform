<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="workshopId" prop="workshopId">
        <el-input-number v-model="queryParams.workshopId" placeholder="请输入workshopId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="workstationId" prop="workstationId">
        <el-input-number v-model="queryParams.workstationId" placeholder="请输入workstationId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="energyType" prop="energyType">
        <el-input-number v-model="queryParams.energyType" placeholder="请输入energyType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:energy-consumption:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="workshopId" align="center" prop="workshopId" />
      <el-table-column label="workstationId" align="center" prop="workstationId" />
      <el-table-column label="energyType" align="center" prop="energyType" />
      <el-table-column label="recordDate" align="center" prop="recordDate" />
      <el-table-column label="consumption" align="center" prop="consumption" />
      <el-table-column label="unit" align="center" prop="unit" />
      <el-table-column label="unitPrice" align="center" prop="unitPrice" />
      <el-table-column label="totalAmount" align="center" prop="totalAmount" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['mes:energy-consumption:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['mes:energy-consumption:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <EnergyConsumptionForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { EnergyConsumptionApi } from '@/api/mes/energy-consumption'
import EnergyConsumptionForm from './energyConsumptionForm.vue'

defineOptions({ name: 'MesEnergyConsumption' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workshopId: undefined,
  workstationId: undefined,
  energyType: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EnergyConsumptionApi.getEnergyConsumptionPage(queryParams)
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
    await EnergyConsumptionApi.deleteEnergyConsumption(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
