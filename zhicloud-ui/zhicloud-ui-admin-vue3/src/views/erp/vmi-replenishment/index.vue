<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="请输入编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" :controls="false" placeholder="供应商 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-input-number v-model="queryParams.warehouseId" :controls="false" placeholder="仓库 ID" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="handleGenerate" v-hasPermi="['erp:vmi-replenishment:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 生成补货建议
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="no" />
      <el-table-column label="供应商" align="center" prop="supplierId" />
      <el-table-column label="仓库" align="center" prop="warehouseId" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="补货数量" align="center" prop="totalQuantity" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="220">
        <template #default="scope">
          <el-button link type="success" @click="handleConvert(scope.row)" v-if="scope.row.status === 10" v-hasPermi="['erp:vmi-replenishment:update']">转采购订单</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:vmi-replenishment:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { VmiReplenishmentApi } from '@/api/erp/vmi-replenishment'

defineOptions({ name: 'ErpVmiReplenishment' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined as any,
  supplierId: undefined as any,
  warehouseId: undefined as any
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await VmiReplenishmentApi.getVmiReplenishmentPage(queryParams)
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

/** 生成补货建议（后端按 VMI 库存自动生成，无手工新增） */
const handleGenerate = async () => {
  try {
    await message.confirm('确认按当前 VMI 库存生成补货建议？')
    const ids: any = await VmiReplenishmentApi.VmiReplenishmentGenerate({})
    message.success(`已生成 ${Array.isArray(ids) ? ids.length : 0} 条补货建议`)
    await getList()
  } catch {}
}

/** 转为采购订单 */
const handleConvert = async (row: any) => {
  try {
    await message.confirm(`确认将补货建议「${row.no}」转为采购订单？`)
    await VmiReplenishmentApi.VmiReplenishmentConvertToPurchaseOrder(row.id)
    message.success('已生成采购订单')
    await getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await VmiReplenishmentApi.deleteVmiReplenishment(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
