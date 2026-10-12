<!-- WMS 越库单 -->
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
      <el-form-item label="越库单号" prop="no">
        <el-input v-model="queryParams.no" class="!w-240px" clearable placeholder="请输入越库单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="源头供应商编号" prop="sourceSupplierId">
        <el-input-number v-model="queryParams.sourceSupplierId" class="!w-240px" :controls="false" clearable placeholder="请输入源头供应商编号" />
      </el-form-item>
      <el-form-item label="源头供应商名称" prop="sourceSupplierName">
        <el-input v-model="queryParams.sourceSupplierName" class="!w-240px" clearable placeholder="请输入源头供应商名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="目标客户编号" prop="targetCustomerId">
        <el-input-number v-model="queryParams.targetCustomerId" class="!w-240px" :controls="false" clearable placeholder="请输入目标客户编号" />
      </el-form-item>
      <el-form-item label="目标客户名称" prop="targetCustomerName">
        <el-input v-model="queryParams.targetCustomerName" class="!w-240px" clearable placeholder="请输入目标客户名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="关联入库单号" prop="receiptOrderNo">
        <el-input v-model="queryParams.receiptOrderNo" class="!w-240px" clearable placeholder="请输入关联入库单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="关联出库单号" prop="shipmentOrderNo">
        <el-input v-model="queryParams.shipmentOrderNo" class="!w-240px" clearable placeholder="请输入关联出库单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="越库状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入越库状态" />
      </el-form-item>
      <el-form-item label="总数量" prop="totalQuantity">
        <el-input-number v-model="queryParams.totalQuantity" class="!w-240px" :controls="false" clearable placeholder="请输入总数量" />
      </el-form-item>
      <el-form-item label="总金额" prop="totalAmount">
        <el-input-number v-model="queryParams.totalAmount" class="!w-240px" :controls="false" clearable placeholder="请输入总金额" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="queryParams.remark" class="!w-240px" clearable placeholder="请输入备注" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="创建者名称" prop="creatorName">
        <el-input v-model="queryParams.creatorName" class="!w-240px" clearable placeholder="请输入创建者名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="更新者名称" prop="updaterName">
        <el-input v-model="queryParams.updaterName" class="!w-240px" clearable placeholder="请输入更新者名称" @keyup.enter="handleQuery" />
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
          v-hasPermi="['wms:cross-dock-order:create']"
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
      <el-table-column align="center" label="越库单号" prop="no" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="源头供应商编号" prop="sourceSupplierId" min-width="120" />
      <el-table-column align="center" label="源头供应商名称" prop="sourceSupplierName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="目标客户编号" prop="targetCustomerId" min-width="120" />
      <el-table-column align="center" label="目标客户名称" prop="targetCustomerName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="关联入库单号" prop="receiptOrderNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="关联出库单号" prop="shipmentOrderNo" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="越库状态" prop="status" min-width="120" />
      <el-table-column align="center" label="总数量" prop="totalQuantity" min-width="120" />
      <el-table-column align="center" label="总金额" prop="totalAmount" min-width="120" />
      <el-table-column align="center" label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="创建者名称" prop="creatorName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="更新者名称" prop="updaterName" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['wms:cross-dock-order:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['wms:cross-dock-order:delete']"
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
  <WmsCrossDockOrderForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { WmsCrossDockOrderApi } from '@/api/wms/cross-dock-order'
import WmsCrossDockOrderForm from './WmsCrossDockOrderForm.vue'

defineOptions({ name: 'WmsCrossDockOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  sourceSupplierId: undefined,
  sourceSupplierName: undefined,
  targetCustomerId: undefined,
  targetCustomerName: undefined,
  receiptOrderNo: undefined,
  shipmentOrderNo: undefined,
  status: undefined,
  totalQuantity: undefined,
  totalAmount: undefined,
  remark: undefined,
  creatorName: undefined,
  updaterName: undefined,
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await WmsCrossDockOrderApi.getWmsCrossDockOrderPage(queryParams)
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
    await WmsCrossDockOrderApi.deleteWmsCrossDockOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
