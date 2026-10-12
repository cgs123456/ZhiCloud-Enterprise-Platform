<template>
  <ContentWrap>
    <!-- 设备数据采集记录：数据由 SCADA 采集写入，无手工新增/修改 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="设备" prop="machineryId">
        <el-input-number v-model="queryParams.machineryId" :controls="false" placeholder="设备 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="SCADA配置" prop="scadaConfigId">
        <el-input-number v-model="queryParams.scadaConfigId" :controls="false" placeholder="SCADA 配置 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="handleCollect" v-hasPermi="['mes:dv-device-data:create']">
          <Icon icon="ep:video-camera" class="mr-5px" /> 立即采集
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="设备" align="center" prop="machineryId" />
      <el-table-column label="SCADA配置" align="center" prop="scadaConfigId" />
      <el-table-column label="属性名" align="center" prop="propertyName" />
      <el-table-column label="属性值" align="center" prop="propertyValue" />
      <el-table-column label="数据类型" align="center" prop="dataType" />
      <el-table-column label="采集时间" align="center" prop="collectTime" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="90">
        <template #default="scope">
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['mes:dv-device-data:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { DvDeviceDataApi } from '@/api/mes/dv-device-data'

defineOptions({ name: 'MesDvDeviceData' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  machineryId: undefined as any,
  scadaConfigId: undefined as any
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await DvDeviceDataApi.getDvDeviceDataPage(queryParams)
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

/** 立即采集一次设备数据 */
const handleCollect = async () => {
  if (!queryParams.machineryId) {
    message.warning('请先填写设备 ID 再采集')
    return
  }
  try {
    await message.confirm('确认立即从 SCADA 采集该设备数据？')
    await DvDeviceDataApi.DvDeviceDataCollect(queryParams.machineryId)
    message.success('已触发采集')
    await getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvDeviceDataApi.deleteDvDeviceData(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
