<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="assetId" prop="assetId">
        <el-input-number v-model="queryParams.assetId" placeholder="请输入assetId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="changeType" prop="changeType">
        <el-input-number v-model="queryParams.changeType" placeholder="请输入changeType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="beforeValue" prop="beforeValue">
        <el-input v-model="queryParams.beforeValue" placeholder="请输入beforeValue" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:fa-change:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="assetId" align="center" prop="assetId" />
      <el-table-column label="changeType" align="center" prop="changeType" />
      <el-table-column label="beforeValue" align="center" prop="beforeValue" />
      <el-table-column label="afterValue" align="center" prop="afterValue" />
      <el-table-column label="changeDate" align="center" prop="changeDate" />
      <el-table-column label="changeReason" align="center" prop="changeReason" />
      <el-table-column label="operatorId" align="center" prop="operatorId" />
      <el-table-column label="approverId" align="center" prop="approverId" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="rejectReason" align="center" prop="rejectReason" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:fa-change:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:fa-change:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <FaChangeForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { FaChangeApi } from '@/api/erp/fa-change'
import FaChangeForm from './faChangeForm.vue'

defineOptions({ name: 'ErpFaChange' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  assetId: undefined,
  changeType: undefined,
  beforeValue: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FaChangeApi.getFaChangePage(queryParams)
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
    await FaChangeApi.deleteFaChange(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
