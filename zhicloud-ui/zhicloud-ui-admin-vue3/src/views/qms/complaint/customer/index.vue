<!-- QMS 客户投诉 -->
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
      <el-form-item label="投诉编号" prop="complaintNo">
        <el-input v-model="queryParams.complaintNo" class="!w-240px" clearable placeholder="请输入投诉编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户 ID" prop="customerId">
        <el-input-number v-model="queryParams.customerId" class="!w-240px" :controls="false" clearable placeholder="请输入客户 ID" />
      </el-form-item>
      <el-form-item label="产品 ID" prop="productId">
        <el-input-number v-model="queryParams.productId" class="!w-240px" :controls="false" clearable placeholder="请输入产品 ID" />
      </el-form-item>
      <el-form-item label="处理方式" prop="handleType">
        <el-input-number v-model="queryParams.handleType" class="!w-240px" :controls="false" clearable placeholder="请输入处理方式" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-input-number v-model="queryParams.status" class="!w-240px" :controls="false" clearable placeholder="请输入状态" />
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
          v-hasPermi="['qms:customer-complaint:create']"
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
      <el-table-column align="center" label="投诉编号" prop="complaintNo" min-width="110" />
      <el-table-column align="center" label="客户 ID" prop="customerId" min-width="110" />
      <el-table-column align="center" label="客户名称" prop="customerName" min-width="110" />
      <el-table-column align="center" label="产品 ID" prop="productId" min-width="110" />
      <el-table-column align="center" label="产品名称" prop="productName" min-width="110" />
      <el-table-column align="center" label="投诉内容" prop="complaintContent" min-width="120" />
      <el-table-column align="center" label="投诉日期" prop="complaintDate" min-width="110" />
      <el-table-column align="center" label="调查根因" prop="rootCause" min-width="110" />
      <el-table-column align="center" label="影响范围" prop="impactScope" min-width="110" />
      <el-table-column align="center" label="处理方式" prop="handleType" min-width="110" />
      <el-table-column align="center" :formatter="dateFormatter" label="创建时间" prop="createTime" width="170" />
      <el-table-column align="center" fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button
            v-hasPermi="['qms:customer-complaint:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            修改
          </el-button>
          <el-button
            v-hasPermi="['qms:customer-complaint:delete']"
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
  <CustomerComplaintForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { CustomerComplaintApi } from '@/api/qms/complaint/customer'
import CustomerComplaintForm from './CustomerComplaintForm.vue'

defineOptions({ name: 'CustomerComplaint' })

const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  complaintNo: undefined,
  customerId: undefined,
  productId: undefined,
  handleType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CustomerComplaintApi.getCustomerComplaintPage(queryParams)
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
    await CustomerComplaintApi.deleteCustomerComplaint(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
