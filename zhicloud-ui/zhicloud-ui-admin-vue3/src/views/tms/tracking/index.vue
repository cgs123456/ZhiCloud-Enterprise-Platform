<!-- TMS 跟踪（按运单查询轨迹 / 上报事件） -->
<template>
  <ContentWrap>
    <el-form :inline="true" class="mb-12px" label-width="90px">
      <el-form-item label="运单 ID">
        <el-input-number v-model="shipmentId" class="!w-200px" :controls="false" placeholder="请输入运单 ID" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">
          <Icon class="mr-5px" icon="ep:search" />
          查询轨迹
        </el-button>
        <el-button v-hasPermi="['tms:tracking:create']" plain type="primary" @click="openForm">
          <Icon class="mr-5px" icon="ep:plus" />
          上报事件
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="运单 ID" prop="shipmentId" min-width="110" />
      <el-table-column align="center" label="事件类型" prop="eventType" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="事件时间" prop="eventTime" width="170" />
      <el-table-column align="center" label="位置" prop="location" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" label="经度" prop="longitude" min-width="110" />
      <el-table-column align="center" label="纬度" prop="latitude" min-width="110" />
      <el-table-column align="center" label="描述" prop="description" min-width="180" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="100">
        <template #default="scope">
          <el-button v-hasPermi="['tms:tracking:delete']" link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <TmsTrackingForm ref="formRef" :shipment-id="shipmentId" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { TmsTrackingApi } from '@/api/tms/tracking'
import TmsTrackingForm from './TmsTrackingForm.vue'

defineOptions({ name: 'TmsTracking' })

const message = useMessage()
const loading = ref(false)
const list = ref<any[]>([])
const shipmentId = ref<number | undefined>(undefined)

const getList = async () => {
  if (!shipmentId.value) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = (await TmsTrackingApi.listByShipment(shipmentId.value)) || []
  } finally {
    loading.value = false
  }
}

const formRef = ref()
const openForm = () => {
  if (!shipmentId.value) {
    message.warning('请先输入运单 ID')
    return
  }
  formRef.value.open(shipmentId.value)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TmsTrackingApi.deleteTracking(id)
    message.success('删除成功')
    await getList()
  } catch {}
}
</script>
