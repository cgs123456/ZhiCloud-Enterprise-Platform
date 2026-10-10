<!-- MES SCADA 配置 -->
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
      <el-form-item label="MES 设备编号" prop="machineryId">
        <el-input-number v-model="queryParams.machineryId" class="!w-240px" :controls="false" clearable placeholder="请输入MES 设备编号" />
      </el-form-item>
      <el-form-item label="MES 设备名称" prop="machineryName">
        <el-input v-model="queryParams.machineryName" class="!w-240px" clearable placeholder="请输入MES 设备名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="IoT 平台设备 PK" prop="iotDevicePk">
        <el-input v-model="queryParams.iotDevicePk" class="!w-240px" clearable placeholder="请输入IoT 平台设备 PK" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="IoT 平台产品编号" prop="iotProductId">
        <el-input-number v-model="queryParams.iotProductId" class="!w-240px" :controls="false" clearable placeholder="请输入IoT 平台产品编号" />
      </el-form-item>
      <el-form-item label="SCADA 协议类型" prop="protocolType">
        <el-input v-model="queryParams.protocolType" class="!w-240px" clearable placeholder="请输入SCADA 协议类型" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="点位映射配置（JSON）" prop="pointConfig">
        <el-input v-model="queryParams.pointConfig" class="!w-240px" clearable placeholder="请输入点位映射配置（JSON）" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-input-number v-model="queryParams.enabled" class="!w-240px" :controls="false" clearable placeholder="请输入是否启用" />
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
          v-hasPermi="['mes:dv-scada-config:create']"
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
      <el-table-column align="center" label="MES 设备编号" prop="machineryId" min-width="120" />
      <el-table-column align="center" label="MES 设备名称" prop="machineryName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="IoT 平台设备 PK" prop="iotDevicePk" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="IoT 平台产品编号" prop="iotProductId" min-width="120" />
      <el-table-column align="center" label="SCADA 协议类型" prop="protocolType" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="点位映射配置（JSON）" prop="pointConfig" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="是否启用" prop="enabled" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['mes:dv-scada-config:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['mes:dv-scada-config:delete']"
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
  <MesDvScadaConfigForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { MesDvScadaConfigApi } from '@/api/mes/dv-scada-config'
import MesDvScadaConfigForm from './MesDvScadaConfigForm.vue'

defineOptions({ name: 'MesDvScadaConfig' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  machineryId: undefined
  machineryName: undefined
  iotDevicePk: undefined
  iotProductId: undefined
  protocolType: undefined
  pointConfig: undefined
  enabled: undefined
  remark: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MesDvScadaConfigApi.getMesDvScadaConfigPage(queryParams)
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
    await MesDvScadaConfigApi.deleteMesDvScadaConfig(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
