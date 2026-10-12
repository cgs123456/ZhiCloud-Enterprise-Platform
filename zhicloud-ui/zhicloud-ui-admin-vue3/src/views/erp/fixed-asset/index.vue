<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编码" prop="code">
        <el-input v-model="queryParams.code" placeholder="请输入编码" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="category" prop="category">
        <el-input v-model="queryParams.category" placeholder="请输入category" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:fixed-asset:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编码" align="center" prop="code" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="category" align="center" prop="category" />
      <el-table-column label="specification" align="center" prop="specification" />
      <el-table-column label="departmentId" align="center" prop="departmentId" />
      <el-table-column label="location" align="center" prop="location" />
      <el-table-column label="responsiblePerson" align="center" prop="responsiblePerson" />
      <el-table-column label="originalValue" align="center" prop="originalValue" />
      <el-table-column label="salvageValue" align="center" prop="salvageValue" />
      <el-table-column label="usefulLifeMonths" align="center" prop="usefulLifeMonths" />
      <el-table-column label="depreciationMethod" align="center" prop="depreciationMethod" />
      <el-table-column label="capitalizationDate" align="center" prop="capitalizationDate" />
      <el-table-column label="assetAccountId" align="center" prop="assetAccountId" />
      <el-table-column label="accumulatedDepreciationAccountId" align="center" prop="accumulatedDepreciationAccountId" />
      <el-table-column label="depreciationExpenseAccountId" align="center" prop="depreciationExpenseAccountId" />
      <el-table-column label="depreciatedMonths" align="center" prop="depreciatedMonths" />
      <el-table-column label="accumulatedDepreciation" align="center" prop="accumulatedDepreciation" />
      <el-table-column label="netBookValue" align="center" prop="netBookValue" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:fixed-asset:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:fixed-asset:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <FixedAssetForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { FixedAssetApi } from '@/api/erp/fixed-asset'
import FixedAssetForm from './fixedAssetForm.vue'

defineOptions({ name: 'ErpFixedAsset' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined
  name: undefined
  category: undefined
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FixedAssetApi.getFixedAssetPage(queryParams)
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
    await FixedAssetApi.deleteFixedAsset(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
