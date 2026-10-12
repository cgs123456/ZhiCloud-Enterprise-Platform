<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="forecastId" prop="forecastId">
        <el-input-number v-model="queryParams.forecastId" placeholder="请输入forecastId" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="exceptionType" prop="exceptionType">
        <el-input-number v-model="queryParams.exceptionType" placeholder="请输入exceptionType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="exceptionDescription" prop="exceptionDescription">
        <el-input v-model="queryParams.exceptionDescription" placeholder="请输入exceptionDescription" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['erp:cpfr-exception:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="forecastId" align="center" prop="forecastId" />
      <el-table-column label="exceptionType" align="center" prop="exceptionType" />
      <el-table-column label="exceptionDescription" align="center" prop="exceptionDescription" />
      <el-table-column label="handlingStatus" align="center" prop="handlingStatus" />
      <el-table-column label="handlerUserId" align="center" prop="handlerUserId" />
      <el-table-column label="handlingTime" align="center" prop="handlingTime" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['erp:cpfr-exception:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['erp:cpfr-exception:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <CpfrExceptionForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { CpfrExceptionApi } from '@/api/erp/cpfr-exception'
import CpfrExceptionForm from './cpfrExceptionForm.vue'

defineOptions({ name: 'ErpCpfrException' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  forecastId: undefined,
  exceptionType: undefined,
  exceptionDescription: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CpfrExceptionApi.getCpfrExceptionPage(queryParams)
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
    await CpfrExceptionApi.deleteCpfrException(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
