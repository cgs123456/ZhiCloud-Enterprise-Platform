<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="supplierId" prop="supplierId">
        <el-input-number v-model="queryParams.supplierId" placeholder="请输入supplierId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="evaluationPeriod" prop="evaluationPeriod">
        <el-input v-model="queryParams.evaluationPeriod" placeholder="请输入evaluationPeriod" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="qualityScore" prop="qualityScore">
        <el-input-number v-model="queryParams.qualityScore" placeholder="请输入qualityScore" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:supplier-evaluation:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="supplierId" align="center" prop="supplierId" />
      <el-table-column label="evaluationPeriod" align="center" prop="evaluationPeriod" />
      <el-table-column label="qualityScore" align="center" prop="qualityScore" />
      <el-table-column label="deliveryScore" align="center" prop="deliveryScore" />
      <el-table-column label="priceScore" align="center" prop="priceScore" />
      <el-table-column label="serviceScore" align="center" prop="serviceScore" />
      <el-table-column label="totalScore" align="center" prop="totalScore" />
      <el-table-column label="grade" align="center" prop="grade" />
      <el-table-column label="evaluator" align="center" prop="evaluator" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:supplier-evaluation:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:supplier-evaluation:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <SupplierEvaluationForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { SupplierEvaluationApi } from '@/api/erp/supplier-evaluation'
import SupplierEvaluationForm from './supplierEvaluationForm.vue'

defineOptions({ name: 'ErpSupplierEvaluation' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierId: undefined
  evaluationPeriod: undefined
  qualityScore: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await SupplierEvaluationApi.getSupplierEvaluationPage(queryParams)
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
    await SupplierEvaluationApi.deleteSupplierEvaluation(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
