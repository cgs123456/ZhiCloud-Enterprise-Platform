<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="编号" prop="no">
        <el-input v-model="queryParams.no" placeholder="请输入编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="ecnName" prop="ecnName">
        <el-input v-model="queryParams.ecnName" placeholder="请输入ecnName" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="changeType" prop="changeType">
        <el-input-number v-model="queryParams.changeType" placeholder="请输入changeType" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['mes:md-ecn-order:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="no" />
      <el-table-column label="ecnName" align="center" prop="ecnName" />
      <el-table-column label="changeType" align="center" prop="changeType" />
      <el-table-column label="bomId" align="center" prop="bomId" />
      <el-table-column label="newBomId" align="center" prop="newBomId" />
      <el-table-column label="changeReason" align="center" prop="changeReason" />
      <el-table-column label="changeDescription" align="center" prop="changeDescription" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="applicantUserId" align="center" prop="applicantUserId" />
      <el-table-column label="approveUserId" align="center" prop="approveUserId" />
      <el-table-column label="approveDate" align="center" prop="approveDate" />
      <el-table-column label="生效日期" align="center" prop="effectiveDate" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['mes:md-ecn-order:update']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['mes:md-ecn-order:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <MdEcnOrderForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { MdEcnOrderApi } from '@/api/mes/md-ecn-order'
import MdEcnOrderForm from './mdEcnOrderForm.vue'

defineOptions({ name: 'MesMdEcnOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  no: undefined,
  ecnName: undefined,
  changeType: undefined,
})
const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await MdEcnOrderApi.getMdEcnOrderPage(queryParams)
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
    await MdEcnOrderApi.deleteMdEcnOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
