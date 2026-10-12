<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="consolidationPeriod" prop="consolidationPeriod">
        <el-input v-model="queryParams.consolidationPeriod" placeholder="请输入consolidationPeriod" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="parentCompanyId" prop="parentCompanyId">
        <el-input-number v-model="queryParams.parentCompanyId" placeholder="请输入parentCompanyId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="subsidiaryCompanyId" prop="subsidiaryCompanyId">
        <el-input-number v-model="queryParams.subsidiaryCompanyId" placeholder="请输入subsidiaryCompanyId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:consolidation-worksheet:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="consolidationPeriod" align="center" prop="consolidationPeriod" />
      <el-table-column label="parentCompanyId" align="center" prop="parentCompanyId" />
      <el-table-column label="subsidiaryCompanyId" align="center" prop="subsidiaryCompanyId" />
      <el-table-column label="eliminationType" align="center" prop="eliminationType" />
      <el-table-column label="eliminationAmount" align="center" prop="eliminationAmount" />
      <el-table-column label="description" align="center" prop="description" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:consolidation-worksheet:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:consolidation-worksheet:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <ConsolidationWorksheetForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ConsolidationWorksheetApi } from '@/api/erp/consolidation-worksheet'
import ConsolidationWorksheetForm from './consolidationWorksheetForm.vue'

defineOptions({ name: 'ErpConsolidationWorksheet' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  consolidationPeriod: undefined,
  parentCompanyId: undefined,
  subsidiaryCompanyId: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ConsolidationWorksheetApi.getConsolidationWorksheetPage(queryParams)
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
    await ConsolidationWorksheetApi.deleteConsolidationWorksheet(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
