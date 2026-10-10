<!-- WMS 波次单 -->
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
      <el-form-item label="波次单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入波次单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库编号" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" class="!w-240px" :controls="false" clearable placeholder="请输入仓库编号" />
      </el-form-item>
      <el-form-item label="波次策略" prop="strategy">
        <el-input-number v-model="queryParams.strategy" class="!w-240px" :controls="false" clearable placeholder="请输入波次策略" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
      </el-form-item>
      <el-form-item label="拣货员" prop="picker">
        <el-input v-model="queryParams.picker" class="!w-240px" clearable placeholder="请输入拣货员" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="出库单数" prop="shipmentCount">
        <el-input-number v-model="queryParams.shipmentCount" class="!w-240px" :controls="false" clearable placeholder="请输入出库单数" />
      </el-form-item>
      <el-form-item label="SKU 数" prop="skuCount">
        <el-input-number v-model="queryParams.skuCount" class="!w-240px" :controls="false" clearable placeholder="请输入SKU 数" />
      </el-form-item>
      <el-form-item label="总数量" prop="totalQuantity">
        <el-input-number v-model="queryParams.totalQuantity" class="!w-240px" :controls="false" clearable placeholder="请输入总数量" />
      </el-form-item>
      <el-form-item label="总金额" prop="totalPrice">
        <el-input-number v-model="queryParams.totalPrice" class="!w-240px" :controls="false" clearable placeholder="请输入总金额" />
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
          v-hasPermi="['wms:wave-order:create']"
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
      <el-table-column align="center" label="波次单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="仓库编号" prop="warehouseId" min-width="120" />
      <el-table-column align="center" label="波次策略" prop="strategy" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="单据日期" prop="orderTime" width="170" />
      <el-table-column align="center" label="状态" prop="status" min-width="120" />
      <el-table-column align="center" label="拣货员" prop="picker" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="出库单数" prop="shipmentCount" min-width="120" />
      <el-table-column align="center" label="SKU 数" prop="skuCount" min-width="120" />
      <el-table-column align="center" label="总数量" prop="totalQuantity" min-width="120" />
      <el-table-column align="center" label="总金额" prop="totalPrice" min-width="120" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['wms:wave-order:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['wms:wave-order:delete']"
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
  <WmsWaveOrderForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { WmsWaveOrderApi } from '@/api/wms/wave-order'
import WmsWaveOrderForm from './WmsWaveOrderForm.vue'

defineOptions({ name: 'WmsWaveOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined
  warehouseId: undefined
  strategy: undefined
  orderTime: undefined
  status: undefined
  picker: undefined
  remark: undefined
  shipmentCount: undefined
  skuCount: undefined
  totalQuantity: undefined
  totalPrice: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await WmsWaveOrderApi.getWmsWaveOrderPage(queryParams)
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
    await WmsWaveOrderApi.deleteWmsWaveOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
